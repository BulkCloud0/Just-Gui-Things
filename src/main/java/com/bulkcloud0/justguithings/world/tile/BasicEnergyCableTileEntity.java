package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.energy.SidedEnergyConduitHandler;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.EnergyRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.EnergyRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.FairShareAllocator;
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

    private final EnumMap<Direction, ConduitTransferMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, EnergyRoutingTargetRule> targetRules = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, EnergyRoutingSourceRule> sourceRules = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities =
            new EnumMap<>(Direction.class);

    private int distributionCursor;

    public BasicEnergyCableTileEntity() {
        super(ModTileEntities.BASIC_ENERGY_CABLE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
            targetRules.put(direction, new EnergyRoutingTargetRule());
            sourceRules.put(direction, new EnergyRoutingSourceRule());
        }
        initializeSidedEnergyCapabilities();
    }

    private void initializeSidedEnergyCapabilities() {
        for (Direction direction : Direction.values()) {
            sidedEnergyCapabilities.put(direction, createSidedEnergyCapability(direction));
        }
    }

    private LazyOptional<IEnergyStorage> createSidedEnergyCapability(Direction side) {
        return LazyOptional.of(() ->
                new SidedEnergyConduitHandler(
                        energyStorage,
                        () -> canReceiveFromExternal(side),
                        () -> getSideMode(side).canPush()));
    }

    private void refreshSidedEnergyCapability(Direction side) {
        LazyOptional<IEnergyStorage> old = sidedEnergyCapabilities.put(
                side, createSidedEnergyCapability(side));
        if (old != null) {
            old.invalidate();
        }

        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.updateNeighborsAt(worldPosition, state.getBlock());
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

    public ConduitTransferMode getSideMode(Direction direction) {
        return sideModes.getOrDefault(direction, ConduitTransferMode.BOTH);
    }

    public ConduitTransferMode cycleSideMode(Direction direction) {
        ConduitTransferMode current = getSideMode(direction);
        ConduitTransferMode next = current.next();
        sideModes.put(direction, next);
        if (next != current) {
            refreshSidedEnergyCapability(direction);
        }
        setChanged();
        syncToClient();
        return next;
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

    public RoutingRedstoneMode cycleSourceRedstoneMode(Direction direction) {
        RoutingRedstoneMode next = getMutableSourceRule(direction).cycleRedstoneMode();
        setChanged();
        return next;
    }

    private boolean canReceiveFromExternal(Direction direction) {
        if (!getSideMode(direction).canPull()) {
            return false;
        }

        boolean powered = level != null && level.hasNeighborSignal(worldPosition);
        return getMutableSourceRule(direction).allowsRedstone(powered);
    }

    public EnergyRoutingTargetRule getTargetRule(Direction direction) {
        return new EnergyRoutingTargetRule(getMutableTargetRule(direction));
    }

    public EnergyRoutingSourceRule getSourceRule(Direction direction) {
        return new EnergyRoutingSourceRule(getMutableSourceRule(direction));
    }

    public void setTargetRule(Direction direction, EnergyRoutingTargetRule rule) {
        targetRules.put(direction, rule == null
                ? new EnergyRoutingTargetRule()
                : new EnergyRoutingTargetRule(rule));
        setChanged();
    }

    public void setSourceRule(Direction direction, EnergyRoutingSourceRule rule) {
        sourceRules.put(direction, rule == null
                ? new EnergyRoutingSourceRule()
                : new EnergyRoutingSourceRule(rule));
        setChanged();
    }

    private EnergyRoutingTargetRule getMutableTargetRule(Direction direction) {
        EnergyRoutingTargetRule rule = targetRules.get(direction);
        if (rule == null) {
            rule = new EnergyRoutingTargetRule();
            targetRules.put(direction, rule);
        }
        return rule;
    }

    private EnergyRoutingSourceRule getMutableSourceRule(Direction direction) {
        EnergyRoutingSourceRule rule = sourceRules.get(direction);
        if (rule == null) {
            rule = new EnergyRoutingSourceRule();
            sourceRules.put(direction, rule);
        }
        return rule;
    }

    private void distributeEnergy(List<BasicEnergyCableTileEntity> network) {
        int budget = Math.min(TRANSFER_RATE, getNetworkEnergy(network));
        if (budget <= 0) {
            return;
        }

        List<EnergyTargetEndpoint> receivers = collectReceivers();
        if (receivers.isEmpty()) {
            return;
        }

        int start = Math.floorMod(distributionCursor, receivers.size());
        boolean movedAny = false;

        for (RoutingPriority priority : ROUTING_ORDER) {
            if (budget <= 0) {
                break;
            }

            List<EnergyTargetEndpoint> priorityTargets = new ArrayList<>();
            for (int offset = 0; offset < receivers.size(); offset++) {
                EnergyTargetEndpoint target = receivers.get((start + offset) % receivers.size());
                if (target.rule.getPriority() == priority) {
                    priorityTargets.add(target);
                }
            }

            if (priorityTargets.isEmpty()) {
                continue;
            }

            boolean retry;
            int distributionRounds = 0;
            do {
                retry = false;
                distributionRounds++;
                int[] demands = new int[priorityTargets.size()];

                for (int index = 0; index < priorityTargets.size(); index++) {
                    demands[index] = Math.max(0,
                            priorityTargets.get(index).handler.receiveEnergy(budget, true));
                }

                int[] allocations = FairShareAllocator.allocate(budget, demands);
                int movedThisRound = 0;

                for (int index = 0; index < priorityTargets.size() && budget > 0; index++) {
                    int planned = Math.min(allocations[index], budget);
                    if (planned <= 0) {
                        continue;
                    }

                    EnergyTargetEndpoint target = priorityTargets.get(index);
                    int accepted = transferEnergyAtomically(network, target.handler, planned);
                    if (accepted <= 0) {
                        continue;
                    }

                    budget -= accepted;
                    movedThisRound += accepted;
                    movedAny = true;

                    int receiverIndex = receivers.indexOf(target);
                    if (receiverIndex >= 0) {
                        distributionCursor = (receiverIndex + 1) % receivers.size();
                    }

                    if (accepted < planned) {
                        retry = true;
                    }
                }

                if (movedThisRound <= 0) {
                    break;
                }

            } while (retry && budget > 0 && distributionRounds < 3);
        }

        if (!movedAny) {
            distributionCursor = (start + 1) % receivers.size();
        }
    }

    private int transferEnergyAtomically(List<BasicEnergyCableTileEntity> network,
                                         IEnergyStorage receiver,
                                         int requested) {
        if (requested <= 0) {
            return 0;
        }

        int accepted = Math.max(0, receiver.receiveEnergy(requested, true));
        int reserved = drainNetworkEnergy(network, Math.min(requested, accepted));
        if (reserved <= 0) {
            return 0;
        }

        int inserted = Math.max(0, Math.min(reserved, receiver.receiveEnergy(reserved, false)));
        int refund = reserved - inserted;
        if (refund > 0) {
            returnNetworkEnergy(network, refund);
        }

        return inserted;
    }

    private List<EnergyTargetEndpoint> collectReceivers() {
        List<EnergyTargetEndpoint> receivers = new ArrayList<>();
        Set<EndpointKey> visitedConsumers = new HashSet<>();

        for (ExternalEndpoint<BasicEnergyCableTileEntity> endpoint : getCachedExternalEndpoints()) {
            BasicEnergyCableTileEntity cable = endpoint.getConduit();
            Direction direction = endpoint.getConduitSide();
            if (!cable.getSideMode(direction).canPush()) {
                continue;
            }

            EndpointKey key = new EndpointKey(endpoint.getNeighborPos(), endpoint.getNeighborSide());
            if (!visitedConsumers.add(key)) {
                continue;
            }

            boolean cablePowered = level.hasNeighborSignal(cable.getBlockPos());
            EnergyRoutingTargetRule rule = cable.getMutableTargetRule(direction);
            if (!rule.allowsRedstone(cablePowered)) {
                continue;
            }

            TileEntity neighbor = getLoadedBlockEntity(endpoint.getNeighborPos());
            if (neighbor == null) {
                continue;
            }

            IEnergyStorage receiver = neighbor
                    .getCapability(CapabilityEnergy.ENERGY, endpoint.getNeighborSide())
                    .orElse(null);
            if (receiver == null || !receiver.canReceive()) {
                continue;
            }

            receivers.add(new EnergyTargetEndpoint(receiver, rule));
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

    private int drainNetworkEnergy(List<BasicEnergyCableTileEntity> network, int amount) {
        int remaining = Math.max(0, amount);
        int drainedTotal = 0;

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
                drainedTotal += drained;
                cable.setChanged();
            }
        }

        return drainedTotal;
    }

    private void returnNetworkEnergy(List<BasicEnergyCableTileEntity> network, int amount) {
        int remaining = Math.max(0, amount);

        for (BasicEnergyCableTileEntity cable : network) {
            if (remaining <= 0) {
                break;
            }
            if (cable.isRemoved()) {
                continue;
            }

            int restored = cable.energyStorage.addEnergy(remaining);
            if (restored > 0) {
                remaining -= restored;
                cable.setChanged();
            }
        }

        if (remaining > 0) {
            throw new IllegalStateException("Unable to return reserved energy to conduit network");
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        energyStorage.setEnergy(nbt.getInt("Energy"));

        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
            targetRules.put(direction, new EnergyRoutingTargetRule());
            sourceRules.put(direction, new EnergyRoutingSourceRule());
        }

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    sideModes.put(direction,
                            ConduitTransferMode.fromOrdinal(config.getInt(key)));
                }
            }
        }

        if (nbt.contains("RoutingConfig")) {
            CompoundNBT routing = nbt.getCompound("RoutingConfig");
            for (Direction direction : Direction.values()) {
                String targetKey = "TargetRule" + direction.ordinal();
                String sourceKey = "SourceRule" + direction.ordinal();

                if (routing.contains(targetKey)) {
                    targetRules.put(direction,
                            EnergyRoutingTargetRule.load(routing.getCompound(targetKey)));
                }
                if (routing.contains(sourceKey)) {
                    sourceRules.put(direction,
                            EnergyRoutingSourceRule.load(routing.getCompound(sourceKey)));
                }
            }
        }

        invalidateNetworkCache();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Energy", energyStorage.getEnergyStored());

        CompoundNBT config = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            config.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", config);

        CompoundNBT routing = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            routing.put("TargetRule" + direction.ordinal(), getMutableTargetRule(direction).save());
            routing.put("SourceRule" + direction.ordinal(), getMutableSourceRule(direction).save());
        }
        nbt.put("RoutingConfig", routing);

        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (side == null) {
                // Side-configurable block capabilities require a concrete face.
                return LazyOptional.empty();
            }

            ConduitTransferMode mode = getSideMode(side);
            if (mode == ConduitTransferMode.DISABLED) {
                return LazyOptional.empty();
            }

            LazyOptional<IEnergyStorage> sided = sidedEnergyCapabilities.get(side);
            return sided == null ? LazyOptional.empty() : sided.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IEnergyStorage> capability : sidedEnergyCapabilities.values()) {
            capability.invalidate();
        }
    }

    @Override
    protected void reviveCaps() {
        super.reviveCaps();
        initializeSidedEnergyCapabilities();
    }

    private static final class EnergyTargetEndpoint {
        private final IEnergyStorage handler;
        private final EnergyRoutingTargetRule rule;

        private EnergyTargetEndpoint(IEnergyStorage handler,
                                     EnergyRoutingTargetRule rule) {
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
