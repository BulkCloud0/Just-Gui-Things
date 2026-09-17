package dev.bulkcloud0.justguithings.energy;

import net.minecraftforge.energy.EnergyStorage;

public class ModEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) onChanged.run();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) onChanged.run();
        return extracted;
    }

    public int addEnergy(int amount) {
        int accepted = Math.min(capacity - energy, Math.max(0, amount));
        if (accepted > 0) {
            energy += accepted;
            onChanged.run();
        }
        return accepted;
    }

    public boolean consumeEnergy(int amount) {
        if (amount <= 0) return true;
        if (energy < amount) return false;
        energy -= amount;
        onChanged.run();
        return true;
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
        onChanged.run();
    }
}
