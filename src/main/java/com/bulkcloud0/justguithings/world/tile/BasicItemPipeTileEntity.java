package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.ItemRouteFilter;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.logistics.ItemRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.ItemRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.ItemTransferHelper;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicItemPipeTileEntity extends AbstractConduitNetworkTileEntity<BasicItemPipeTileEntity> {
    public static final int TRANSFER_RATE = 8;
    private static final int NETWORK_CACHE_TTL = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 10;
    private static final RoutingPriority[] ROUTING_ORDER = {
            RoutingPriority.HIGH,
            RoutingPriority.NORMAL,
            RoutingPriority.LOW
    };

    private final EnumMap<Direction, ConduitTransferMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, ItemRoutingTargetRule> targetRules = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, ItemRoutingSourceRule> sourceRules = new EnumMap<>(Direction.class);

    private int sourceCursor;
    private int targetCursor;

    public BasicItemPipeTileEntity() {
        super(ModTileEntities.BASIC_ITEM_PIPE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
            targetRules.put(direction, new ItemRoutingTargetRule());
            sourceRules.put(direction, new ItemRoutingSourceRule());
        }
    }

    @Override
    protected Class<BasicItemPipeTileEntity> getNetworkNodeClass() {
        return BasicItemPipeTileEntity.class;
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

        transferItems(getCachedNetwork());
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

    public ItemRoutingTargetRule getTargetRule(Direction direction) {
        return new ItemRoutingTargetRule(getMutableTargetRule(direction));
    }

    public ItemRoutingSourceRule getSourceRule(Direction direction) {
        return new ItemRoutingSourceRule(getMutableSourceRule(direction));
    }

    public RoutingPriority cycleTargetPriority(Direction direction) {
        RoutingPriority next = getMutableTargetRule(direction).cyclePriority();
        setChanged();
        return next;
    }

    public RoutingFilterSampleChange toggleTargetFilterSample(Direction direction, ItemStack sample) {
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

    public RoutingFilterSampleChange toggleSourceFilterSample(Direction direction, ItemStack sample) {
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

    private ItemRoutingTargetRule getMutableTargetRule(Direction direction) {
        ItemRoutingTargetRule rule = targetRules.get(direction);
        if (rule == null) {
            rule = new ItemRoutingTargetRule();
            targetRules.put(direction, rule);
        }
        return rule;
    }

    private ItemRoutingSourceRule getMutableSourceRule(Direction direction) {
        ItemRoutingSourceRule rule = sourceRules.get(direction);
        if (rule == null) {
            rule = new ItemRoutingSourceRule();
            sourceRules.put(direction, rule);
        }
        return rule;
    }

    private void transferItems(List<BasicItemPipeTileEntity> network) {
        List<SourceEndpoint> sources = new ArrayList<>();
        List<TargetEndpoint> targets = new ArrayList<>();
        collectEndpoints(sources, targets);

        if (sources.isEmpty() || targets.isEmpty()) {
            return;
        }

        int budget = TRANSFER_RATE;
        int sourceStart = Math.floorMod(sourceCursor, sources.size());
        int lastContributingSource = -1;

        for (int sourceOffset = 0; sourceOffset < sources.size() && budget > 0; sourceOffset++) {
            int sourceIndex = (sourceStart + sourceOffset) % sources.size();
            SourceEndpoint source = sources.get(sourceIndex);

            for (int slot = 0; slot < source.handler.getSlots() && budget > 0; slot++) {
                int extractable = source.rule.getExtractableAmount(source.handler, slot, budget);
                if (extractable <= 0) {
                    continue;
                }

                ItemStack simulated = source.handler.extractItem(slot, extractable, true);
                if (simulated.isEmpty()) {
                    continue;
                }

                int moved = routeToTarget(source, slot, targets, Math.min(extractable, simulated.getCount()));
                if (moved > 0) {
                    budget -= moved;
                    lastContributingSource = sourceIndex;
                }
            }
        }

        int cursorBase = lastContributingSource >= 0 ? lastContributingSource : sourceStart;
        sourceCursor = (cursorBase + 1) % sources.size();
    }

    private int routeToTarget(SourceEndpoint source, int sourceSlot,
                              List<TargetEndpoint> targets, int maxAmount) {
        int remaining = Math.max(0, maxAmount);
        int movedTotal = 0;
        int targetStart = Math.floorMod(targetCursor, targets.size());

        for (RoutingPriority priority : ROUTING_ORDER) {
            for (int targetOffset = 0; targetOffset < targets.size() && remaining > 0; targetOffset++) {
                int targetIndex = (targetStart + targetOffset) % targets.size();
                TargetEndpoint target = targets.get(targetIndex);

                if (target.rule.getPriority() != priority
                        || target.inventoryPos.equals(source.inventoryPos)) {
                    continue;
                }

                int moved = moveItem(source, sourceSlot, target, remaining);
                if (moved <= 0) {
                    continue;
                }

                movedTotal += moved;
                remaining -= moved;
                targetCursor = (targetIndex + 1) % targets.size();
            }

            if (remaining <= 0) {
                break;
            }
        }

        return movedTotal;
    }

    private void collectEndpoints(List<SourceEndpoint> sources,
                                  List<TargetEndpoint> targets) {
        Set<EndpointKey> sourceKeys = new HashSet<>();
        Set<EndpointKey> targetKeys = new HashSet<>();

        for (ExternalEndpoint<BasicItemPipeTileEntity> endpoint : getCachedExternalEndpoints()) {
            BasicItemPipeTileEntity pipe = endpoint.getConduit();
            Direction direction = endpoint.getConduitSide();
            ConduitTransferMode mode = pipe.getSideMode(direction);
            if (mode == ConduitTransferMode.DISABLED) {
                continue;
            }

            TileEntity neighbor = getLoadedBlockEntity(endpoint.getNeighborPos());
            if (neighbor == null) {
                continue;
            }

            IItemHandler handler = neighbor
                    .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, endpoint.getNeighborSide())
                    .orElse(null);
            if (handler == null) {
                continue;
            }

            boolean pipePowered = level.hasNeighborSignal(pipe.getBlockPos());
            EndpointKey key = new EndpointKey(endpoint.getNeighborPos(), endpoint.getNeighborSide());

            if (mode.canPull() && sourceKeys.add(key)) {
                ItemRoutingSourceRule sourceRule = pipe.getSourceRule(direction);
                if (sourceRule.allowsRedstone(pipePowered)) {
                    sources.add(new SourceEndpoint(endpoint.getNeighborPos(), handler, sourceRule));
                }
            }

            if (mode.canPush() && targetKeys.add(key)) {
                ItemRoutingTargetRule targetRule = pipe.getTargetRule(direction);
                if (targetRule.allowsRedstone(pipePowered)) {
                    targets.add(new TargetEndpoint(endpoint.getNeighborPos(), handler, targetRule));
                }
            }
        }
    }

    private int moveItem(SourceEndpoint source, int sourceSlot,
                         TargetEndpoint target, int maxAmount) {
        int sourceLimit = source.rule.getExtractableAmount(source.handler, sourceSlot, maxAmount);
        if (sourceLimit <= 0) {
            return 0;
        }

        ItemStack simulatedExtract = source.handler.extractItem(sourceSlot, sourceLimit, true);
        if (simulatedExtract.isEmpty() || !target.rule.accepts(simulatedExtract)) {
            return 0;
        }

        ItemStack simulatedRemainder = ItemTransferHelper.insert(target.handler, simulatedExtract, true);
        int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return 0;
        }

        ItemStack extracted = source.handler.extractItem(sourceSlot, accepted, false);
        if (extracted.isEmpty()) {
            return 0;
        }

        ItemStack remainder = ItemTransferHelper.insert(target.handler, extracted, false);
        int inserted = extracted.getCount() - remainder.getCount();

        if (!remainder.isEmpty()) {
            ItemStack returned = source.handler.insertItem(sourceSlot, remainder, false);
            if (!returned.isEmpty() && level != null) {
                InventoryHelper.dropItemStack(level,
                        source.inventoryPos.getX() + 0.5D,
                        source.inventoryPos.getY() + 0.5D,
                        source.inventoryPos.getZ() + 0.5D,
                        returned);
            }
        }

        return inserted;
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
            targetRules.put(direction, new ItemRoutingTargetRule());
            sourceRules.put(direction, new ItemRoutingSourceRule());
        }

        if (nbt.contains("RoutingConfig")) {
            CompoundNBT routing = nbt.getCompound("RoutingConfig");

            for (Direction direction : Direction.values()) {
                String sourceRuleKey = "SourceRule" + direction.ordinal();
                if (routing.contains(sourceRuleKey)) {
                    sourceRules.put(direction,
                            ItemRoutingSourceRule.load(routing.getCompound(sourceRuleKey)));
                }

                String targetRuleKey = "TargetRule" + direction.ordinal();
                if (routing.contains(targetRuleKey)) {
                    targetRules.put(direction,
                            ItemRoutingTargetRule.load(routing.getCompound(targetRuleKey)));
                    continue;
                }

                ItemRoutingTargetRule migrated = new ItemRoutingTargetRule();

                String legacyPriorityKey = "Priority" + direction.ordinal();
                if (routing.contains(legacyPriorityKey)) {
                    migrated.setPriority(
                            RoutingPriority.fromOrdinal(routing.getInt(legacyPriorityKey)));
                }

                String legacyRuleKey = "Rule" + direction.ordinal();
                String legacyFilterKey = "Filter" + direction.ordinal();

                if (routing.contains(legacyRuleKey)) {
                    migrated.setFilter(
                            ItemRouteFilter.load(routing.getCompound(legacyRuleKey)));
                } else if (routing.contains(legacyFilterKey)) {
                    ItemRouteFilter legacyFilter = new ItemRouteFilter();
                    legacyFilter.addSample(
                            ItemStack.of(routing.getCompound(legacyFilterKey)));
                    migrated.setFilter(legacyFilter);
                }

                targetRules.put(direction, migrated);
            }
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
            routing.put(
                    "TargetRule" + direction.ordinal(),
                    getMutableTargetRule(direction).save());
            routing.put(
                    "SourceRule" + direction.ordinal(),
                    getMutableSourceRule(direction).save());
        }
        nbt.put("RoutingConfig", routing);

        return nbt;
    }

    private static final class SourceEndpoint {
        private final BlockPos inventoryPos;
        private final IItemHandler handler;
        private final ItemRoutingSourceRule rule;

        private SourceEndpoint(BlockPos inventoryPos, IItemHandler handler,
                               ItemRoutingSourceRule rule) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
            this.rule = new ItemRoutingSourceRule(rule);
        }
    }

    private static final class TargetEndpoint {
        private final BlockPos inventoryPos;
        private final IItemHandler handler;
        private final ItemRoutingTargetRule rule;

        private TargetEndpoint(BlockPos inventoryPos, IItemHandler handler,
                               ItemRoutingTargetRule rule) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
            this.rule = new ItemRoutingTargetRule(rule);
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
