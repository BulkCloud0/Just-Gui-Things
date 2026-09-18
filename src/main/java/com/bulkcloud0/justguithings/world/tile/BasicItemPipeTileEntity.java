package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.ItemFilterMode;
import com.bulkcloud0.justguithings.logistics.ItemFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.ItemRouteFilter;
import com.bulkcloud0.justguithings.logistics.ItemRoutingPriority;
import com.bulkcloud0.justguithings.logistics.ItemRoutingRedstoneMode;
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
    private static final ItemRoutingPriority[] ROUTING_ORDER = {
            ItemRoutingPriority.HIGH,
            ItemRoutingPriority.NORMAL,
            ItemRoutingPriority.LOW
    };

    private final EnumMap<Direction, ConduitTransferMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, ItemRoutingTargetRule> targetRules = new EnumMap<>(Direction.class);

    private int sourceCursor;
    private int targetCursor;

    public BasicItemPipeTileEntity() {
        super(ModTileEntities.BASIC_ITEM_PIPE.get(), NETWORK_CACHE_TTL);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, ConduitTransferMode.BOTH);
            targetRules.put(direction, new ItemRoutingTargetRule());
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

    public ItemRoutingPriority getTargetPriority(Direction direction) {
        return getMutableTargetRule(direction).getPriority();
    }

    public ItemRoutingPriority cycleTargetPriority(Direction direction) {
        ItemRoutingPriority next = getMutableTargetRule(direction).cyclePriority();
        setChanged();
        return next;
    }

    public ItemFilterSampleChange toggleTargetFilterSample(Direction direction, ItemStack sample) {
        ItemFilterSampleChange change = getMutableTargetRule(direction).toggleFilterSample(sample);
        if (change != ItemFilterSampleChange.FULL) {
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

    public ItemFilterMode cycleTargetFilterMode(Direction direction) {
        ItemFilterMode next = getMutableTargetRule(direction).cycleFilterMode();
        setChanged();
        return next;
    }

    public boolean toggleTargetFilterNbt(Direction direction) {
        boolean matchNbt = getMutableTargetRule(direction).toggleFilterNbt();
        setChanged();
        return matchNbt;
    }

    public ItemRoutingRedstoneMode cycleTargetRedstoneMode(Direction direction) {
        ItemRoutingRedstoneMode next = getMutableTargetRule(direction).cycleRedstoneMode();
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

    private void transferItems(List<BasicItemPipeTileEntity> network) {
        Set<BlockPos> pipePositions = new HashSet<>();
        for (BasicItemPipeTileEntity pipe : network) {
            if (pipe.isRemoved()) {
                invalidateNetworkCache();
                return;
            }
            pipePositions.add(pipe.getBlockPos());
        }

        List<ItemEndpoint> sources = new ArrayList<>();
        List<ItemEndpoint> targets = new ArrayList<>();
        collectEndpoints(network, pipePositions, sources, targets);

        if (sources.isEmpty() || targets.isEmpty()) {
            return;
        }

        int budget = TRANSFER_RATE;
        int sourceStart = Math.floorMod(sourceCursor, sources.size());

        for (int sourceOffset = 0; sourceOffset < sources.size() && budget > 0; sourceOffset++) {
            ItemEndpoint source = sources.get((sourceStart + sourceOffset) % sources.size());

            for (int slot = 0; slot < source.handler.getSlots() && budget > 0; slot++) {
                ItemStack simulated = source.handler.extractItem(slot, budget, true);
                if (simulated.isEmpty()) {
                    continue;
                }

                int moved = routeToTarget(source, slot, targets, budget);
                if (moved > 0) {
                    budget -= moved;
                }
            }
        }

        sourceCursor = (sourceStart + 1) % sources.size();
    }

    private int routeToTarget(ItemEndpoint source, int sourceSlot, List<ItemEndpoint> targets, int maxAmount) {
        int targetStart = Math.floorMod(targetCursor, targets.size());

        for (ItemRoutingPriority priority : ROUTING_ORDER) {
            for (int targetOffset = 0; targetOffset < targets.size(); targetOffset++) {
                int targetIndex = (targetStart + targetOffset) % targets.size();
                ItemEndpoint target = targets.get(targetIndex);

                if (target.rule.getPriority() != priority || target.inventoryPos.equals(source.inventoryPos)) {
                    continue;
                }

                int moved = moveItem(source, sourceSlot, target, maxAmount);
                if (moved > 0) {
                    targetCursor = (targetIndex + 1) % targets.size();
                    return moved;
                }
            }
        }

        return 0;
    }

    private void collectEndpoints(List<BasicItemPipeTileEntity> network,
                                  Set<BlockPos> pipePositions,
                                  List<ItemEndpoint> sources,
                                  List<ItemEndpoint> targets) {
        Set<EndpointKey> sourceKeys = new HashSet<>();
        Set<EndpointKey> targetKeys = new HashSet<>();

        for (BasicItemPipeTileEntity pipe : network) {
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

                IItemHandler handler = neighbor
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())
                        .orElse(null);
                if (handler == null) {
                    continue;
                }

                EndpointKey key = new EndpointKey(neighborPos, direction.getOpposite());
                if (mode.canPull() && sourceKeys.add(key)) {
                    sources.add(ItemEndpoint.source(neighborPos, handler));
                }

                if (mode.canPush() && targetKeys.add(key)) {
                    ItemRoutingTargetRule rule = pipe.getTargetRule(direction);
                    if (rule.allowsRedstone(pipePowered)) {
                        targets.add(ItemEndpoint.target(neighborPos, handler, rule));
                    }
                }
            }
        }
    }

    private int moveItem(ItemEndpoint source, int sourceSlot, ItemEndpoint target, int maxAmount) {
        ItemStack simulatedExtract = source.handler.extractItem(sourceSlot, maxAmount, true);
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
        }

        if (nbt.contains("RoutingConfig")) {
            CompoundNBT routing = nbt.getCompound("RoutingConfig");
            for (Direction direction : Direction.values()) {
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
                            ItemRoutingPriority.fromOrdinal(routing.getInt(legacyPriorityKey)));
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
        }
        nbt.put("RoutingConfig", routing);

        return nbt;
    }

    private static final class ItemEndpoint {
        private final BlockPos inventoryPos;
        private final IItemHandler handler;
        private final ItemRoutingTargetRule rule;

        private ItemEndpoint(BlockPos inventoryPos, IItemHandler handler, ItemRoutingTargetRule rule) {
            this.inventoryPos = inventoryPos;
            this.handler = handler;
            this.rule = new ItemRoutingTargetRule(rule);
        }

        private static ItemEndpoint source(BlockPos inventoryPos, IItemHandler handler) {
            return new ItemEndpoint(inventoryPos, handler, new ItemRoutingTargetRule());
        }

        private static ItemEndpoint target(BlockPos inventoryPos, IItemHandler handler,
                                           ItemRoutingTargetRule rule) {
            return new ItemEndpoint(inventoryPos, handler, rule);
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
