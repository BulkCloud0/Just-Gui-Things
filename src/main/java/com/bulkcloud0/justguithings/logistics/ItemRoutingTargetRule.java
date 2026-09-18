package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class ItemRoutingTargetRule {
    private ItemRoutingPriority priority = ItemRoutingPriority.NORMAL;
    private ItemRouteFilter filter = new ItemRouteFilter();
    private ItemRoutingRedstoneMode redstoneMode = ItemRoutingRedstoneMode.ALWAYS;

    public ItemRoutingTargetRule() {
    }

    public ItemRoutingTargetRule(ItemRoutingTargetRule other) {
        priority = other.priority;
        filter = new ItemRouteFilter(other.filter);
        redstoneMode = other.redstoneMode;
    }

    public ItemRoutingPriority getPriority() {
        return priority;
    }

    public void setPriority(ItemRoutingPriority priority) {
        this.priority = priority == null ? ItemRoutingPriority.NORMAL : priority;
    }

    public ItemRoutingPriority cyclePriority() {
        priority = priority.next();
        return priority;
    }

    public ItemRouteFilter getFilter() {
        return new ItemRouteFilter(filter);
    }

    public void setFilter(ItemRouteFilter filter) {
        this.filter = filter == null ? new ItemRouteFilter() : new ItemRouteFilter(filter);
    }

    public ItemFilterSampleChange toggleFilterSample(ItemStack stack) {
        return filter.toggleSample(stack);
    }

    public int getFilterSampleCount() {
        return filter.getSampleCount();
    }

    public void clearFilterSamples() {
        filter.clearSamples();
    }

    public ItemFilterMode cycleFilterMode() {
        return filter.cycleMode();
    }

    public boolean toggleFilterNbt() {
        return filter.toggleMatchNbt();
    }

    public boolean accepts(ItemStack stack) {
        return filter.accepts(stack);
    }

    public ItemRoutingRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public ItemRoutingRedstoneMode cycleRedstoneMode() {
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
            rule.priority = ItemRoutingPriority.fromOrdinal(nbt.getInt("Priority"));
        }
        if (nbt.contains("Filter")) {
            rule.filter = ItemRouteFilter.load(nbt.getCompound("Filter"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = ItemRoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }

        return rule;
    }
}
