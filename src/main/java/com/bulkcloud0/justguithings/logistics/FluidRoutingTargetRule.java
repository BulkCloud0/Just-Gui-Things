package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidRoutingTargetRule {
    private static final int[] MAX_STOCK_VALUES = {0, 250, 1000, 4000, 8000, 16000};

    private RoutingPriority priority = RoutingPriority.NORMAL;
    private FluidRouteFilter filter = new FluidRouteFilter();
    private int maxStock;
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public FluidRoutingTargetRule() {
    }

    public FluidRoutingTargetRule(FluidRoutingTargetRule other) {
        priority = other.priority;
        filter = new FluidRouteFilter(other.filter);
        maxStock = other.maxStock;
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

    public int getFillableAmount(IFluidHandler handler, FluidStack candidate, int requested) {
        if (handler == null || candidate == null || candidate.isEmpty()
                || requested <= 0 || !filter.accepts(candidate)) {
            return 0;
        }

        int requestedAmount = Math.min(requested, candidate.getAmount());
        if (maxStock <= 0) {
            return requestedAmount;
        }

        int matchingAmount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty() && filter.matchesIdentity(candidate, stack)) {
                matchingAmount += stack.getAmount();
            }
        }

        return Math.min(requestedAmount, Math.max(0, maxStock - matchingAmount));
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

    public static FluidRoutingTargetRule load(CompoundNBT nbt) {
        FluidRoutingTargetRule rule = new FluidRoutingTargetRule();
        if (nbt.contains("Priority")) {
            rule.priority = RoutingPriority.fromOrdinal(nbt.getInt("Priority"));
        }
        if (nbt.contains("Filter")) {
            rule.filter = FluidRouteFilter.load(nbt.getCompound("Filter"));
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
