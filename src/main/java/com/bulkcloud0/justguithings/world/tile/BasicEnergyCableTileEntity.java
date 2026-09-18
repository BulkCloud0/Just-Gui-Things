package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.logistics.EnergyRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicEnergyCableTileEntity extends AbstractConduitNetworkTileEntity<BasicEnergyCableTileEntity> {
    public static final int INTERNAL_BUFFER = 1_000;
    public static final int TRANSFER_RATE = 500;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;
    private static final RoutingPriority[] ROUTING_ORDER = {
            RoutingPriority.HIGH,
            RoutingPriority.NORMAL,
            RoutingPriority.LOW
    };

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

    private final EnumMap<Direction, EnergyRoutingTargetRule> targetRules = new EnumMap<>(Direction.class);
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private int distributionCursor;

    public BasicEnergyCableTileEntity() {
        super(ModTileEntities.BASIC_ENERGY_CABLE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            targetRules.put(direction, new EnergyRoutingTargetRule());
        }
    }

    @Override
    protected Class<BasicEnergyCableTileEntity> getNetworkNodeClass() {
        return BasicEnergyCableTileEntity.class;
    }

    @Override
    protected void onNetworkCacheCleared() {
        distributionCursor = 0;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        tickNetworkMaintenance(VISUAL_REFRESH_INTERVAL);
        if (!isNetworkController()) {
            return;
        }

        distributeEnergy(getCachedNetwork());
    }

    public RoutingPriority cycleTargetPriority(Direction direction) {
        RoutingPriority next = getMutableTargetRule(direction).cyclePriority();
        setChanged();
        return next;
    }

    public RoutingRedstoneMode cycleTargetRedstoneMode(Direction direction) {
        RoutingRedstoneMode next = getMutableTargetRule(direction).cycleRedstoneMode();
        setChanged();
        return next;
    }

    private EnergyRoutingTargetRule getTargetRule(Direction direction) {
        return new EnergyRoutingTargetRule(getMutableTargetRule(direction));
    }

    private EnergyRoutingTargetRule getMutableTargetRule(Direction direction) {
        EnergyRoutingTargetRule rule = targetRules.get(direction);
        if (rule == null) {
            rule = new EnergyRoutingTargetRule();
            targetRules.put(direction, rule);
        }
        return rule;
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

        List<EnergyTargetEndpoint> receivers = collectReceivers(network, cablePositions);
        if (receivers.isEmpty()) {
            return;
        }

        int start = Math.floorMod(distributionCursor, receivers.size());
        boolean movedAny = false;

        for (RoutingPriority priority : ROUTING_ORDER) {
            for (int offset = 0; offset < receivers.size() && budget > 0 && available > 0; offset++) {
                int receiverIndex = (start + offset) % receivers.size();
                EnergyTargetEndpoint target = receivers.get(receiverIndex);
                if (target.rule.getPriority() != priority) {
                    continue;
                }

                int offer = Math.min(budget, available);
                int accepted = target.handler.receiveEnergy(offer, true);
                if (accepted <= 0) {
                    continue;
                }

                accepted = target.handler.receiveEnergy(Math.min(offer, accepted), false);
                if (accepted <= 0) {
                    continue;
                }

                drainNetworkEnergy(network, accepted);
                available -= accepted;
                budget -= accepted;
                distributionCursor = (receiverIndex + 1) % receivers.size();
                movedAny = true;
            }
        }

        if (!movedAny) {
            distributionCursor = (start + 1) % receivers.size();
        }
    }

    private List<EnergyTargetEndpoint> collectReceivers(List<BasicEnergyCableTileEntity> network,
                                                        Set<BlockPos> cablePositions) {
        List<EnergyTargetEndpoint> receivers = new ArrayList<>();
        Set<EndpointKey> visitedConsumers = new HashSet<>();

        for (BasicEnergyCableTileEntity cable : network) {
            boolean cablePowered = level.hasNeighborSignal(cable.getBlockPos());

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cable.getBlockPos().relative(direction);
                if (cablePositions.contains(neighborPos)) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                if (!visitedConsumers.add(key)) {
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

                EnergyRoutingTargetRule rule = cable.getTargetRule(direction);
                if (!rule.allowsRedstone(cablePowered)) {
                    continue;
                }

                receivers.add(new EnergyTargetEndpoint(neighborPos, receiver, rule));
            }
        }

        return receivers;
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

        for (Direction direction : Direction.values()) {
            targetRules.put(direction, new EnergyRoutingTargetRule());
        }

        if (nbt.contains("RoutingConfig")) {
            CompoundNBT routing = nbt.getCompound("RoutingConfig");
            for (Direction direction : Direction.values()) {
                String key = "TargetRule" + direction.ordinal();
                if (routing.contains(key)) {
                    targetRules.put(direction,
                            EnergyRoutingTargetRule.load(routing.getCompound(key)));
                }
            }
        }

        invalidateNetworkCache();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Energy", energyStorage.getEnergyStored());

        CompoundNBT routing = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            routing.put("TargetRule" + direction.ordinal(), getMutableTargetRule(direction).save());
        }
        nbt.put("RoutingConfig", routing);

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

    private static final class EnergyTargetEndpoint {
        private final BlockPos blockPos;
        private final IEnergyStorage handler;
        private final EnergyRoutingTargetRule rule;

        private EnergyTargetEndpoint(BlockPos blockPos, IEnergyStorage handler,
                                     EnergyRoutingTargetRule rule) {
            this.blockPos = blockPos.immutable();
            this.handler = handler;
            this.rule = new EnergyRoutingTargetRule(rule);
        }
    }

    private static final class EndpointKey {
        private final BlockPos pos;
        private final Direction side;

        private EndpointKey(BlockPos pos, Direction side) {
            this.pos = pos.immutable();
            this.side = side;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EndpointKey)) {
                return false;
            }
            EndpointKey that = (EndpointKey) other;
            return pos.equals(that.pos) && side == that.side;
        }

        @Override
        public int hashCode() {
            return 31 * pos.hashCode() + side.hashCode();
        }
    }
}
