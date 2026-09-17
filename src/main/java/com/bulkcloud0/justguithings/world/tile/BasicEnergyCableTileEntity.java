package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
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
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BasicEnergyCableTileEntity extends TileEntity implements ITickableTileEntity {
    public static final int INTERNAL_BUFFER = 1_000;
    public static final int TRANSFER_RATE = 500;

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

    public BasicEnergyCableTileEntity() {
        super(ModTileEntities.BASIC_ENERGY_CABLE.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        List<BasicEnergyCableTileEntity> network = discoverNetwork();
        if (network.isEmpty() || !isNetworkController(network)) {
            return;
        }

        distributeEnergy(network);
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
                if (visited.contains(next)) {
                    continue;
                }

                TileEntity nextTile = level.getBlockEntity(next);
                if (nextTile instanceof BasicEnergyCableTileEntity) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        return cables;
    }

    private boolean isNetworkController(List<BasicEnergyCableTileEntity> network) {
        long ownKey = worldPosition.asLong();
        for (BasicEnergyCableTileEntity cable : network) {
            if (cable.getBlockPos().asLong() < ownKey) {
                return false;
            }
        }
        return true;
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

        Set<BlockPos> visitedConsumers = new HashSet<>();
        for (BasicEnergyCableTileEntity cable : network) {
            for (Direction direction : Direction.values()) {
                if (budget <= 0 || available <= 0) {
                    return;
                }

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
                if (receiver == null || !receiver.canReceive()) {
                    continue;
                }

                int offer = Math.min(budget, available);
                int accepted = receiver.receiveEnergy(offer, false);
                if (accepted > 0) {
                    drainNetworkEnergy(network, accepted);
                    available -= accepted;
                    budget -= accepted;
                }
            }
        }
    }

    private int getNetworkEnergy(List<BasicEnergyCableTileEntity> network) {
        int total = 0;
        for (BasicEnergyCableTileEntity cable : network) {
            total += cable.energyStorage.getEnergyStored();
        }
        return total;
    }

    private void drainNetworkEnergy(List<BasicEnergyCableTileEntity> network, int amount) {
        int remaining = amount;
        for (BasicEnergyCableTileEntity cable : network) {
            if (remaining <= 0) {
                break;
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
        super.setRemoved();
        energyCapability.invalidate();
    }
}
