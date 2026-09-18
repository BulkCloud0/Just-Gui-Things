package com.bulkcloud0.justguithings.machine;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.BooleanSupplier;

public class MachineSidedEnergyHandler implements IEnergyStorage {
    private final IEnergyStorage delegate;
    private final BooleanSupplier receiveEnabled;
    private final BooleanSupplier extractEnabled;

    public MachineSidedEnergyHandler(IEnergyStorage delegate,
                                     BooleanSupplier receiveEnabled,
                                     BooleanSupplier extractEnabled) {
        this.delegate = delegate;
        this.receiveEnabled = receiveEnabled;
        this.extractEnabled = extractEnabled;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return receiveEnabled.getAsBoolean() ? delegate.receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return extractEnabled.getAsBoolean() ? delegate.extractEnergy(maxExtract, simulate) : 0;
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
        return extractEnabled.getAsBoolean() && delegate.canExtract();
    }

    @Override
    public boolean canReceive() {
        return receiveEnabled.getAsBoolean() && delegate.canReceive();
    }
}
