package com.bulkcloud0.justguithings.energy;

import net.minecraftforge.energy.EnergyStorage;

public class ModEnergyStorage extends EnergyStorage {
    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (energy >= capacity) {
            return 0;
        }
        return super.receiveEnergy(maxReceive, simulate);
    }

    public int addEnergy(int amount) {
        int accepted = Math.max(0, Math.min(capacity - energy, amount));
        energy += accepted;
        return accepted;
    }

    public int consumeEnergy(int amount) {
        int consumed = Math.min(energy, amount);
        energy -= consumed;
        return consumed;
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    public void setCapacity(int amount) {
        capacity = Math.max(0, amount);
    }
}
