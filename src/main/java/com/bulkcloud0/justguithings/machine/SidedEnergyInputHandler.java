package com.bulkcloud0.justguithings.machine;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.BooleanSupplier;

public class SidedEnergyInputHandler implements IEnergyStorage {
    private final IEnergyStorage delegate;
    private final BooleanSupplier enabled;

    public SidedEnergyInputHandler(IEnergyStorage delegate, BooleanSupplier enabled) {
        this.delegate = delegate;
        this.enabled = enabled;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return enabled.getAsBoolean() ? delegate.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return delegate.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return enabled.getAsBoolean() && delegate.canReceive();
    }
}
