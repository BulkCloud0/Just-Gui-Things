package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;

public final class ItemRoutingTargetRule {
    private static final int[] MAX_STOCK_VALUES = {0, 1, 8, 16, 32, 64};

    private RoutingPriority priority = RoutingPriority.NORMAL;
    private ItemRouteFilter filter = new ItemRouteFilter();
    private int maxStock;
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public ItemRoutingTargetRule() {
    }

    public ItemRoutingTargetRule(ItemRoutingTargetRule other) {
        priority = other.priority;
        filter = new ItemRouteFilter(other.filter);
        maxStock = other.maxStock;
        redstoneMode = other.redstoneMode;
    }

    public RoutingPriority getPriority() {
        return priority;
    }

    public void setPriority(RoutingPriority priority) {
        this.priority = priority == null ? RoutingPriority.NORMAL : priority;
    }

    public RoutingPriority cyclePriority() {
        priority = priority.next();
        return priority;
    }

    public ItemRouteFilter getFilter() {
        return new ItemRouteFilter(filter);
    }

    public void setFilter(ItemRouteFilter filter) {
        this.filter = filter == null ? new ItemRouteFilter() : new ItemRouteFilter(filter);
    }

    public RoutingFilterSampleChange toggleFilterSample(ItemStack stack) {
        return filter.toggleSample(stack);
    }

    public int getFilterSampleCount() {
        return filter.getSampleCount();
    }

    public void clearFilterSamples() {
        filter.clearSamples();
    }

    public RoutingFilterMode cycleFilterMode() {
        return filter.cycleMode();
    }

    public boolean toggleFilterNbt() {
        return filter.toggleMatchNbt();
    }

    public boolean accepts(ItemStack stack) {
        return filter.accepts(stack);
    }

    public int getMaxStock() {
        return maxStock;
    }

    public int cycleMaxStock() {
        int currentIndex = 0;
        for (int index = 0; index < MAX_STOCK_VALUES.length; index++) {
            if (MAX_STOCK_VALUES[index] == maxStock) {
                currentIndex = index;
                break;
            }
        }
        maxStock = MAX_STOCK_VALUES[(currentIndex + 1) % MAX_STOCK_VALUES.length];
        return maxStock;
    }

    public int getInsertableAmount(IItemHandler handler, ItemStack candidate, int requested) {
        if (handler == null || candidate == null || candidate.isEmpty()
                || requested <= 0 || !filter.accepts(candidate)) {
            return 0;
        }

        int requestedAmount = Math.min(requested, candidate.getCount());
        if (maxStock <= 0) {
            return requestedAmount;
        }

        int matchingCount = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && filter.matchesIdentity(candidate, stack)) {
                matchingCount += stack.getCount();
            }
        }

        return Math.min(requestedAmount, Math.max(0, maxStock - matchingCount));
    }

    public RoutingRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public RoutingRedstoneMode cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        return redstoneMode;
    }

    public boolean allowsRedstone(boolean powered) {
        return redstoneMode.allows(powered);
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("Priority", priority.ordinal());
        nbt.put("Filter", filter.save());
        nbt.putInt("MaxStock", maxStock);
        nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        return nbt;
    }

    public static ItemRoutingTargetRule load(CompoundNBT nbt) {
        ItemRoutingTargetRule rule = new ItemRoutingTargetRule();

        if (nbt.contains("Priority")) {
            rule.priority = RoutingPriority.fromOrdinal(nbt.getInt("Priority"));
        }
        if (nbt.contains("Filter")) {
            rule.filter = ItemRouteFilter.load(nbt.getCompound("Filter"));
        }
        if (nbt.contains("MaxStock")) {
            rule.maxStock = normalizeMaxStock(nbt.getInt("MaxStock"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }

        return rule;
    }

    private static int normalizeMaxStock(int value) {
        for (int allowed : MAX_STOCK_VALUES) {
            if (value == allowed) {
                return allowed;
            }
        }
        return 0;
    }
}
