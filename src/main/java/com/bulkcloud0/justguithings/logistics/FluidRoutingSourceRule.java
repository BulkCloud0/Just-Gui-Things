package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidRoutingSourceRule {
    private static final int[] MIN_STOCK_VALUES = {0, 250, 1000, 4000, 8000, 16000};

    private FluidRouteFilter filter = new FluidRouteFilter();
    private int minStock;
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public FluidRoutingSourceRule() {
    }

    public FluidRoutingSourceRule(FluidRoutingSourceRule other) {
        filter = new FluidRouteFilter(other.filter);
        minStock = other.minStock;
        redstoneMode = other.redstoneMode;
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

    public int getMinStock() {
        return minStock;
    }

    public int cycleMinStock() {
        int currentIndex = 0;
        for (int index = 0; index < MIN_STOCK_VALUES.length; index++) {
            if (MIN_STOCK_VALUES[index] == minStock) {
                currentIndex = index;
                break;
            }
        }
        minStock = MIN_STOCK_VALUES[(currentIndex + 1) % MIN_STOCK_VALUES.length];
        return minStock;
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

    public FluidStack findDrainable(IFluidHandler handler, int requested) {
        if (requested <= 0) {
            return FluidStack.EMPTY;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack candidate = handler.getFluidInTank(tank);
            if (candidate.isEmpty() || !filter.accepts(candidate)) {
                continue;
            }

            int amount = getDrainableAmount(handler, candidate, requested);
            if (amount <= 0) {
                continue;
            }

            FluidStack request = candidate.copy();
            request.setAmount(amount);
            FluidStack simulated = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty()) {
                simulated.setAmount(Math.min(simulated.getAmount(), amount));
                return simulated;
            }
        }

        return FluidStack.EMPTY;
    }

    public int getDrainableAmount(IFluidHandler handler, FluidStack candidate, int requested) {
        if (candidate == null || candidate.isEmpty() || requested <= 0 || !filter.accepts(candidate)) {
            return 0;
        }

        if (minStock <= 0) {
            return requested;
        }

        int matchingAmount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty() && filter.matchesIdentity(candidate, stack)) {
                matchingAmount += stack.getAmount();
            }
        }

        return Math.min(requested, Math.max(0, matchingAmount - minStock));
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Filter", filter.save());
        nbt.putInt("MinStock", minStock);
        nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        return nbt;
    }

    public static FluidRoutingSourceRule load(CompoundNBT nbt) {
        FluidRoutingSourceRule rule = new FluidRoutingSourceRule();
        if (nbt.contains("Filter")) {
            rule.filter = FluidRouteFilter.load(nbt.getCompound("Filter"));
        }
        if (nbt.contains("MinStock")) {
            rule.minStock = normalizeMinStock(nbt.getInt("MinStock"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }
        return rule;
    }

    private static int normalizeMinStock(int value) {
        for (int allowed : MIN_STOCK_VALUES) {
            if (value == allowed) {
                return allowed;
            }
        }
        return 0;
    }
}
