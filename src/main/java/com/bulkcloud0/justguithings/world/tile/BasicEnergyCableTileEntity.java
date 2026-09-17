package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.block.BasicEnergyCableBlock;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BasicEnergyCableTileEntity extends TileEntity implements ITickableTileEntity {
    public static final int INTERNAL_BUFFER = 1_000;
    public static final int TRANSFER_RATE = 500;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(INTERNAL_BUFFER, TRANSFER_RATE, TRANSFER_RATE) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private List<BasicEnergyCableTileEntity> cachedNetwork = Collections.emptyList();
    private BlockPos cachedController;
    private long cacheValidUntil;
    private int distributionCursor;

    public BasicEnergyCableTileEntity() {
        super(ModTileEntities.BASIC_ENERGY_CABLE.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if ((level.getGameTime() + worldPosition.asLong()) % VISUAL_REFRESH_INTERVAL == 0L) {
            BasicEnergyCableBlock.refreshConnections(level, worldPosition);
        }

        ensureNetworkCache();
        if (cachedController == null || !worldPosition.equals(cachedController)) {
            return;
        }

        distributeEnergy(cachedNetwork);
    }

    private void ensureNetworkCache() {
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (!cachedNetwork.isEmpty() && cachedController != null && gameTime < cacheValidUntil) {
            return;
        }

        rebuildNetworkCache(gameTime);
    }

    private void rebuildNetworkCache(long gameTime) {
        List<BasicEnergyCableTileEntity> discovered = discoverNetwork();
        if (discovered.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        BlockPos controller = discovered.get(0).getBlockPos();
        for (BasicEnergyCableTileEntity cable : discovered) {
            if (cable.getBlockPos().asLong() < controller.asLong()) {
                controller = cable.getBlockPos();
            }
        }

        List<BasicEnergyCableTileEntity> sharedNetwork = Collections.unmodifiableList(new ArrayList<>(discovered));
        long validUntil = gameTime + NETWORK_CACHE_TTL;
        for (BasicEnergyCableTileEntity cable : discovered) {
            cable.cachedNetwork = sharedNetwork;
            cable.cachedController = controller;
            cable.cacheValidUntil = validUntil;
        }
    }

    private List<BasicEnergyCableTileEntity> discoverNetwork() {
        List<BasicEnergyCableTileEntity> cables = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            TileEntity tile = level.getBlockEntity(pos);
            if (!(tile instanceof BasicEnergyCableTileEntity)) {
                continue;
            }

            BasicEnergyCableTileEntity cable = (BasicEnergyCableTileEntity) tile;
            cables.add(cable);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next)) {
                    continue;
                }

                TileEntity nextTile = level.getBlockEntity(next);
                if (nextTile instanceof BasicEnergyCableTileEntity) {
                    queue.add(next);
                }
            }
        }

        return cables;
    }

    public void invalidateNetworkCache() {
        if (cachedNetwork.isEmpty()) {
            clearNetworkCacheLocal();
            return;
        }

        List<BasicEnergyCableTileEntity> previousNetwork = new ArrayList<>(cachedNetwork);
        for (BasicEnergyCableTileEntity cable : previousNetwork) {
            cable.clearNetworkCacheLocal();
        }
    }

    private void clearNetworkCacheLocal() {
        cachedNetwork = Collections.emptyList();
        cachedController = null;
        cacheValidUntil = 0L;
        distributionCursor = 0;
    }

    private void distributeEnergy(List<BasicEnergyCableTileEntity> network) {
        int available = getNetworkEnergy(network);
        int budget = Math.min(TRANSFER_RATE, available);
        if (budget <= 0) {
            return;
        }

        Set<BlockPos> cablePositions = new HashSet<>();
        for (BasicEnergyCableTileEntity cable : network) {
            cablePositions.add(cable.getBlockPos());
        }

        List<IEnergyStorage> receivers = new ArrayList<>();
        Set<BlockPos> visitedConsumers = new HashSet<>();
        for (BasicEnergyCableTileEntity cable : network) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cable.getBlockPos().relative(direction);
                if (cablePositions.contains(neighborPos) || !visitedConsumers.add(neighborPos)) {
                    continue;
                }

                TileEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null) {
                    continue;
                }

                IEnergyStorage receiver = neighbor
                        .getCapability(CapabilityEnergy.ENERGY, direction.getOpposite())
                        .orElse(null);
                if (receiver != null && receiver.canReceive()) {
                    receivers.add(receiver);
                }
            }
        }

        if (receivers.isEmpty()) {
            return;
        }

        int start = Math.floorMod(distributionCursor, receivers.size());
        for (int offset = 0; offset < receivers.size() && budget > 0 && available > 0; offset++) {
            IEnergyStorage receiver = receivers.get((start + offset) % receivers.size());
            int offer = Math.min(budget, available);
            int accepted = receiver.receiveEnergy(offer, false);
            if (accepted > 0) {
                drainNetworkEnergy(network, accepted);
                available -= accepted;
                budget -= accepted;
            }
        }

        distributionCursor = (start + 1) % receivers.size();
    }

    private int getNetworkEnergy(List<BasicEnergyCableTileEntity> network) {
        long total = 0L;
        for (BasicEnergyCableTileEntity cable : network) {
            if (cable.isRemoved()) {
                invalidateNetworkCache();
                return 0;
            }
            total += cable.energyStorage.getEnergyStored();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private void drainNetworkEnergy(List<BasicEnergyCableTileEntity> network, int amount) {
        int remaining = amount;
        for (BasicEnergyCableTileEntity cable : network) {
            if (remaining <= 0) {
                break;
            }
            if (cable.isRemoved()) {
                continue;
            }

            int drained = cable.energyStorage.consumeEnergy(remaining);
            if (drained > 0) {
                remaining -= drained;
                cable.setChanged();
            }
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        energyStorage.setEnergy(nbt.getInt("Energy"));
        clearNetworkCacheLocal();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Energy", energyStorage.getEnergyStored());
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        invalidateNetworkCache();
        super.setRemoved();
        energyCapability.invalidate();
    }
}
