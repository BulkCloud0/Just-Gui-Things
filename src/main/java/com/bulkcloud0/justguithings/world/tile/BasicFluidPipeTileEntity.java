package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.FluidRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.FluidRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.RoutingFilterMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicFluidPipeTileEntity extends AbstractConduitNetworkTileEntity<BasicFluidPipeTileEntity> {
    public static final int TRANSFER_RATE = 250;
    private static final int RECOVERY_CAPACITY = 1_000;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;
    private static final RoutingPriority[] ROUTING_ORDER = {
            RoutingPriority.HIGH,
            RoutingPriority.NORMAL,
            RoutingPriority.LOW
    };

    private final EnumMap<Direction, ConduitTransferMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, FluidRoutingTargetRule> targetRules = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, FluidRoutingSourceRule> sourceRules = new EnumMap<>(Direction.class);

    private final FluidTank recoveryTank = new FluidTank(RECOVERY_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int sourceCursor;
    private int targetCursor;

    public BasicFluidPipeTileEntity() {
        super(ModTileEntities.BASIC_FLUID_PIPE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
            targetRules.put(direction, new FluidRoutingTargetRule());
            sourceRules.put(direction, new FluidRoutingSourceRule());
        }
    }

    @Override
    protected Class<BasicFluidPipeTileEntity> getNetworkNodeClass() {
        return BasicFluidPipeTileEntity.class;
    }

    @Override
    protected void onNetworkCacheCleared() {
        sourceCursor = 0;
        targetCursor = 0;
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

        transferFluids(getCachedNetwork());
    }

    public ConduitTransferMode getSideMode(Direction direction) {
        return sideModes.getOrDefault(direction, ConduitTransferMode.BOTH);
    }

    public ConduitTransferMode cycleSideMode(Direction direction) {
        ConduitTransferMode next = getSideMode(direction).next();
        sideModes.put(direction, next);
        setChanged();
        return next;
    }

    public RoutingPriority cycleTargetPriority(Direction direction) {
        RoutingPriority next = getMutableTargetRule(direction).cyclePriority();
        setChanged();
        return next;
    }

    public RoutingFilterSampleChange toggleTargetFilterSample(Direction direction, FluidStack sample) {
        RoutingFilterSampleChange change = getMutableTargetRule(direction).toggleFilterSample(sample);
        if (change != RoutingFilterSampleChange.FULL) {
            setChanged();
        }
        return change;
    }

    public int getTargetFilterSampleCount(Direction direction) {
        return getMutableTargetRule(direction).getFilterSampleCount();
    }

    public void clearTargetFilter(Direction direction) {
        getMutableTargetRule(direction).clearFilterSamples();
        setChanged();
    }

    public RoutingFilterMode cycleTargetFilterMode(Direction direction) {
        RoutingFilterMode next = getMutableTargetRule(direction).cycleFilterMode();
        setChanged();
        return next;
    }

    public boolean toggleTargetFilterNbt(Direction direction) {
        boolean matchNbt = getMutableTargetRule(direction).toggleFilterNbt();
        setChanged();
        return matchNbt;
    }

    public RoutingRedstoneMode cycleTargetRedstoneMode(Direction direction) {
        RoutingRedstoneMode next = getMutableTargetRule(direction).cycleRedstoneMode();
        setChanged();
        return next;
    }

    public RoutingFilterSampleChange toggleSourceFilterSample(Direction direction, FluidStack sample) {
        RoutingFilterSampleChange change = getMutableSourceRule(direction).toggleFilterSample(sample);
        if (change != RoutingFilterSampleChange.FULL) {
            setChanged();
        }
        return change;
    }

    public int getSourceFilterSampleCount(Direction direction) {
        return getMutableSourceRule(direction).getFilterSampleCount();
    }

    public void clearSourceFilter(Direction direction) {
        getMutableSourceRule(direction).clearFilterSamples();
        setChanged();
    }

    public RoutingFilterMode cycleSourceFilterMode(Direction direction) {
        RoutingFilterMode next = getMutableSourceRule(direction).cycleFilterMode();
        setChanged();
        return next;
    }

    public boolean toggleSourceFilterNbt(Direction direction) {
        boolean matchNbt = getMutableSourceRule(direction).toggleFilterNbt();
        setChanged();
        return matchNbt;
    }

    public int cycleSourceMinStock(Direction direction) {
        int minStock = getMutableSourceRule(direction).cycleMinStock();
        setChanged();
        return minStock;
    }

    public RoutingRedstoneMode cycleSourceRedstoneMode(Direction direction) {
        RoutingRedstoneMode next = getMutableSourceRule(direction).cycleRedstoneMode();
        setChanged();
        return next;
    }

    private FluidRoutingTargetRule getTargetRule(Direction direction) {
        return new FluidRoutingTargetRule(getMutableTargetRule(direction));
    }

    private FluidRoutingSourceRule getSourceRule(Direction direction) {
        return new FluidRoutingSourceRule(getMutableSourceRule(direction));
    }

    private FluidRoutingTargetRule getMutableTargetRule(Direction direction) {
        FluidRoutingTargetRule rule = targetRules.get(direction);
        if (rule == null) {
            rule = new FluidRoutingTargetRule();
            targetRules.put(direction, rule);
        }
        return rule;
    }

    private FluidRoutingSourceRule getMutableSourceRule(Direction direction) {
        FluidRoutingSourceRule rule = sourceRules.get(direction);
        if (rule == null) {
            rule = new FluidRoutingSourceRule();
            sourceRules.put(direction, rule);
        }
        return rule;
    }

    private void transferFluids(List<BasicFluidPipeTileEntity> network) {
        Set<BlockPos> pipePositions = new HashSet<>();
        for (BasicFluidPipeTileEntity pipe : network) {
            if (pipe.isRemoved()) {
                invalidateNetworkCache();
                return;
            }
            pipePositions.add(pipe.getBlockPos());
        }

        List<SourceEndpoint> sources = new ArrayList<>();
        List<TargetEndpoint> targets = new ArrayList<>();
        collectEndpoints(network, pipePositions, sources, targets);

        if (targets.isEmpty()) {
            return;
        }

        int budget = flushRecovery(network, targets, TRANSFER_RATE);
        if (budget <= 0 || hasRecovery(network) || sources.isEmpty()) {
            return;
        }

        int sourceStart = Math.floorMod(sourceCursor, sources.size());
        for (int sourceOffset = 0; sourceOffset < sources.size() && budget > 0; sourceOffset++) {
            SourceEndpoint source = sources.get((sourceStart + sourceOffset) % sources.size());
            FluidStack simulatedDrain = source.rule.findDrainable(source.handler, budget);
            if (simulatedDrain.isEmpty()) {
                continue;
            }

            int moved = routeToTarget(network, source, targets, simulatedDrain, budget);
            if (moved > 0) {
                budget -= moved;
            }

            if (hasRecovery(network)) {
                break;
            }
        }

        sourceCursor = (sourceStart + 1) % sources.size();
    }

    private int routeToTarget(List<BasicFluidPipeTileEntity> network,
                              SourceEndpoint source,
                              List<TargetEndpoint> targets,
                              FluidStack simulatedDrain,
                              int maxAmount) {
        int targetStart = Math.floorMod(targetCursor, targets.size());

        for (RoutingPriority priority : ROUTING_ORDER) {
            for (int targetOffset = 0; targetOffset < targets.size(); targetOffset++) {
                int targetIndex = (targetStart + targetOffset) % targets.size();
                TargetEndpoint target = targets.get(targetIndex);

                if (target.rule.getPriority() != priority
                        || target.inventoryPos.equals(source.inventoryPos)
                        || !target.rule.accepts(simulatedDrain)) {
                    continue;
                }

                int moved = moveFluid(network, source, target, simulatedDrain, maxAmount);
                if (moved > 0) {
                    targetCursor = (targetIndex + 1) % targets.size();
                    return moved;
                }
            }
        }

        return 0;
    }

    private void collectEndpoints(List<BasicFluidPipeTileEntity> network,
                                  Set<BlockPos> pipePositions,
                                  List<SourceEndpoint> sources,
                                  List<TargetEndpoint> targets) {
        Set<EndpointKey> sourceKeys = new HashSet<>();
        Set<EndpointKey> targetKeys = new HashSet<>();

        for (BasicFluidPipeTileEntity pipe : network) {
            boolean pipePowered = level.hasNeighborSignal(pipe.getBlockPos());

            for (Direction direction : Direction.values()) {
                ConduitTransferMode mode = pipe.getSideMode(direction);
                if (mode == ConduitTransferMode.DISABLED) {
                    continue;
                }

                BlockPos neighborPos = pipe.getBlockPos().relative(direction);
                if (pipePositions.contains(neighborPos)) {
                    continue;
                }

                TileEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null) {
                    continue;
                }

                IFluidHandler handler = neighbor
                        .getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite())
                        .orElse(null);
                if (handler == null) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());

                if (mode.canPull() && sourceKeys.add(key)) {
                    FluidRoutingSourceRule rule = pipe.getSourceRule(direction);
                    if (rule.allowsRedstone(pipePowered)) {
                        sources.add(new SourceEndpoint(neighborPos, handler, rule));
                    }
                }

                if (mode.canPush() && targetKeys.add(key)) {
                    FluidRoutingTargetRule rule = pipe.getTargetRule(direction);
                    if (rule.allowsRedstone(pipePowered)) {
                        targets.add(new TargetEndpoint(neighborPos, handler, rule));
                    }
                }
            }
        }
    }

    private int moveFluid(List<BasicFluidPipeTileEntity> network,
                          SourceEndpoint source,
                          TargetEndpoint target,
                          FluidStack simulatedDrain,
                          int maxAmount) {
        int sourceLimit = source.rule.getDrainableAmount(source.handler, simulatedDrain, maxAmount);
        if (sourceLimit <= 0) {
            return 0;
        }

        FluidStack offer = simulatedDrain.copy();
        offer.setAmount(Math.min(sourceLimit, simulatedDrain.getAmount()));

        int accepted = target.handler.fill(offer, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        FluidStack request = offer.copy();
        request.setAmount(Math.min(accepted, offer.getAmount()));
        FluidStack drained = source.handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        int inserted = target.handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        inserted = Math.max(0, Math.min(inserted, drained.getAmount()));

        int remainderAmount = drained.getAmount() - inserted;
        if (remainderAmount > 0) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(remainderAmount);

            int returned = source.handler.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            returned = Math.max(0, Math.min(returned, remainder.getAmount()));
            remainder.setAmount(remainder.getAmount() - returned);

            if (!remainder.isEmpty()) {
                storeRecovery(network, remainder);
            }
        }

        return inserted;
    }

    private int flushRecovery(List<BasicFluidPipeTileEntity> network,
                              List<TargetEndpoint> targets,
                              int budget) {
        if (budget <= 0 || targets.isEmpty()) {
            return budget;
        }

        for (BasicFluidPipeTileEntity pipe : network) {
            while (budget > 0 && !pipe.recoveryTank.isEmpty()) {
                FluidStack buffered = pipe.recoveryTank.getFluid().copy();
                buffered.setAmount(Math.min(buffered.getAmount(), budget));

                int moved = routeRecoveryTarget(targets, buffered);
                if (moved <= 0) {
                    return budget;
                }

                pipe.recoveryTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                budget -= moved;
            }
        }

        return budget;
    }

    private int routeRecoveryTarget(List<TargetEndpoint> targets, FluidStack buffered) {
        int targetStart = Math.floorMod(targetCursor, targets.size());

        for (RoutingPriority priority : ROUTING_ORDER) {
            for (int offset = 0; offset < targets.size(); offset++) {
                int targetIndex = (targetStart + offset) % targets.size();
                TargetEndpoint target = targets.get(targetIndex);

                if (target.rule.getPriority() != priority || !target.rule.accepts(buffered)) {
                    continue;
                }

                int accepted = target.handler.fill(buffered, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) {
                    continue;
                }

                FluidStack transfer = buffered.copy();
                transfer.setAmount(Math.min(accepted, buffered.getAmount()));
                int inserted = target.handler.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
                inserted = Math.max(0, Math.min(inserted, transfer.getAmount()));
                if (inserted <= 0) {
                    continue;
                }

                targetCursor = (targetIndex + 1) % targets.size();
                return inserted;
            }
        }

        return 0;
    }

    private boolean hasRecovery(List<BasicFluidPipeTileEntity> network) {
        for (BasicFluidPipeTileEntity pipe : network) {
            if (!pipe.recoveryTank.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void storeRecovery(List<BasicFluidPipeTileEntity> network, FluidStack stack) {
        FluidStack remaining = stack.copy();
        for (BasicFluidPipeTileEntity pipe : network) {
            if (remaining.isEmpty()) {
                break;
            }

            int accepted = pipe.recoveryTank.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) {
                remaining.setAmount(remaining.getAmount() - accepted);
            }
        }

        if (!remaining.isEmpty()) {
            throw new IllegalStateException("Fluid recovery capacity exhausted for conduit network");
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    sideModes.put(direction, ConduitTransferMode.fromOrdinal(config.getInt(key)));
                }
            }
        }

        for (Direction direction : Direction.values()) {
            targetRules.put(direction, new FluidRoutingTargetRule());
            sourceRules.put(direction, new FluidRoutingSourceRule());
        }

        if (nbt.contains("RoutingConfig")) {
            CompoundNBT routing = nbt.getCompound("RoutingConfig");
            for (Direction direction : Direction.values()) {
                String targetKey = "TargetRule" + direction.ordinal();
                String sourceKey = "SourceRule" + direction.ordinal();

                if (routing.contains(targetKey)) {
                    targetRules.put(direction,
                            FluidRoutingTargetRule.load(routing.getCompound(targetKey)));
                }
                if (routing.contains(sourceKey)) {
                    sourceRules.put(direction,
                            FluidRoutingSourceRule.load(routing.getCompound(sourceKey)));
                }
            }
        }

        if (nbt.contains("RecoveryTank")) {
            recoveryTank.readFromNBT(nbt.getCompound("RecoveryTank"));
        }

        invalidateNetworkCache();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

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

        nbt.put("RecoveryTank", recoveryTank.writeToNBT(new CompoundNBT()));
        return nbt;
    }

    private static final class SourceEndpoint {
        private final BlockPos inventoryPos;
        private final IFluidHandler handler;
        private final FluidRoutingSourceRule rule;

        private SourceEndpoint(BlockPos inventoryPos, IFluidHandler handler,
                               FluidRoutingSourceRule rule) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
            this.rule = new FluidRoutingSourceRule(rule);
        }
    }

    private static final class TargetEndpoint {
        private final BlockPos inventoryPos;
        private final IFluidHandler handler;
        private final FluidRoutingTargetRule rule;

        private TargetEndpoint(BlockPos inventoryPos, IFluidHandler handler,
                               FluidRoutingTargetRule rule) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
            this.rule = new FluidRoutingTargetRule(rule);
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
