package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class ItemRoutingTargetRule {
    private RoutingPriority priority = RoutingPriority.NORMAL;
    private ItemRouteFilter filter = new ItemRouteFilter();
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public ItemRoutingTargetRule() {
    }

    public ItemRoutingTargetRule(ItemRoutingTargetRule other) {
        priority = other.priority;
        filter = new ItemRouteFilter(other.filter);
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
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }

        return rule;
    }
}
