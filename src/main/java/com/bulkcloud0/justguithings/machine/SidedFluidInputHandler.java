package com.bulkcloud0.justguithings.machine;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

public class SidedFluidInputHandler implements IFluidHandler {
    private final IFluidHandler delegate;
    private final BooleanSupplier enabled;

    public SidedFluidInputHandler(IFluidHandler delegate, BooleanSupplier enabled) {
        this.delegate = delegate;
        this.enabled = enabled;
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return enabled.getAsBoolean() && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return enabled.getAsBoolean() ? delegate.fill(resource, action) : 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
