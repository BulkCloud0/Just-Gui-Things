package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;

public final class FluidRoutingTargetRule {
    private RoutingPriority priority = RoutingPriority.NORMAL;
    private FluidRouteFilter filter = new FluidRouteFilter();
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public FluidRoutingTargetRule() {
    }

    public FluidRoutingTargetRule(FluidRoutingTargetRule other) {
        priority = other.priority;
        filter = new FluidRouteFilter(other.filter);
        redstoneMode = other.redstoneMode;
    }

    public RoutingPriority getPriority() {
        return priority;
    }

    public RoutingPriority cyclePriority() {
        priority = priority.next();
        return priority;
    }

    public FluidRouteFilter getFilter() {
        return new FluidRouteFilter(filter);
    }

    public RoutingFilterSampleChange toggleFilterSample(FluidStack stack) {
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

    public boolean accepts(FluidStack stack) {
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

    public static FluidRoutingTargetRule load(CompoundNBT nbt) {
        FluidRoutingTargetRule rule = new FluidRoutingTargetRule();
        if (nbt.contains("Priority")) {
            rule.priority = RoutingPriority.fromOrdinal(nbt.getInt("Priority"));
        }
        if (nbt.contains("Filter")) {
            rule.filter = FluidRouteFilter.load(nbt.getCompound("Filter"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }
        return rule;
    }
}
