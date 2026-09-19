package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.energy.IEnergyStorage;

final class EnergyCellItemEnergyStorage implements IEnergyStorage {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String ENERGY_TAG = "Energy";

    private final ItemStack stack;

    EnergyCellItemEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }

        int stored = getEnergyStored();
        int accepted = Math.min(
                Math.min(maxReceive, EnergyCellTileEntity.MAX_TRANSFER),
                EnergyCellTileEntity.CAPACITY - stored);
        if (accepted > 0 && !simulate) {
            setEnergyStored(stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }

        int stored = getEnergyStored();
        int extracted = Math.min(
                Math.min(maxExtract, EnergyCellTileEntity.MAX_TRANSFER),
                stored);
        if (extracted > 0 && !simulate) {
            setEnergyStored(stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return getStoredEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return EnergyCellTileEntity.CAPACITY;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    static int getStoredEnergy(ItemStack stack) {
        CompoundNBT root = stack.getTag();
        if (root == null || !root.contains(BLOCK_ENTITY_TAG, 10)) {
            return 0;
        }

        CompoundNBT blockEntityTag = root.getCompound(BLOCK_ENTITY_TAG);
        return Math.max(0, Math.min(
                EnergyCellTileEntity.CAPACITY,
                blockEntityTag.getInt(ENERGY_TAG)));
    }

    private void setEnergyStored(int energy) {
        CompoundNBT root = stack.getOrCreateTag();
        CompoundNBT blockEntityTag = root.contains(BLOCK_ENTITY_TAG, 10)
                ? root.getCompound(BLOCK_ENTITY_TAG)
                : new CompoundNBT();

        blockEntityTag.putInt(
                ENERGY_TAG,
                Math.max(0, Math.min(EnergyCellTileEntity.CAPACITY, energy)));
        root.put(BLOCK_ENTITY_TAG, blockEntityTag);
    }
}
