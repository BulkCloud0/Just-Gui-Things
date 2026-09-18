package com.bulkcloud0.justguithings.energy;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.BooleanSupplier;

public final class SidedEnergyConduitHandler implements IEnergyStorage {
    private final IEnergyStorage delegate;
    private final BooleanSupplier allowReceive;
    private final BooleanSupplier allowExtract;

    public SidedEnergyConduitHandler(IEnergyStorage delegate,
                                     BooleanSupplier allowReceive,
                                     BooleanSupplier allowExtract) {
        this.delegate = delegate;
        this.allowReceive = allowReceive;
        this.allowExtract = allowExtract;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return canReceive() ? delegate.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return canExtract() ? delegate.extractEnergy(maxExtract, simulate) : 0;
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
        return allowExtract.getAsBoolean() && delegate.canExtract();
    }

    @Override
    public boolean canReceive() {
        return allowReceive.getAsBoolean() && delegate.canReceive();
    }
}
