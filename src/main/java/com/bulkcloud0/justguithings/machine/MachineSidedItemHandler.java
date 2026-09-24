package com.bulkcloud0.justguithings.machine;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class MachineSidedItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final int inputStart;
    private final int inputCount;
    private final int outputStart;
    private final int outputCount;
    private final Supplier<MachineSideMode> modeSupplier;

    public MachineSidedItemHandler(IItemHandler delegate,
                                   int inputStart,
                                   int inputCount,
                                   int outputStart,
                                   int outputCount,
                                   Supplier<MachineSideMode> modeSupplier) {
        this.delegate = delegate;
        this.inputStart = inputStart;
        this.inputCount = inputCount;
        this.outputStart = outputStart;
        this.outputCount = outputCount;
        this.modeSupplier = modeSupplier;
    }

    @Override
    public int getSlots() {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.INPUT) {
            return inputCount;
        }
        if (mode == MachineSideMode.OUTPUT) {
            return outputCount;
        }
        return 0;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        int mapped = mapSlot(slot, modeSupplier.get());
        return mapped >= 0 ? delegate.getStackInSlot(mapped) : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        MachineSideMode mode = modeSupplier.get();
        if (mode != MachineSideMode.INPUT) {
            return stack;
        }
        int mapped = mapSlot(slot, mode);
        return mapped >= 0 ? delegate.insertItem(mapped, stack, simulate) : stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        MachineSideMode mode = modeSupplier.get();
        if (mode != MachineSideMode.OUTPUT) {
            return ItemStack.EMPTY;
        }
        int mapped = mapSlot(slot, mode);
        return mapped >= 0 ? delegate.extractItem(mapped, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        int mapped = mapSlot(slot, modeSupplier.get());
        return mapped >= 0 ? delegate.getSlotLimit(mapped) : 0;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        MachineSideMode mode = modeSupplier.get();
        if (mode != MachineSideMode.INPUT) {
            return false;
        }
        int mapped = mapSlot(slot, mode);
        return mapped >= 0 && delegate.isItemValid(mapped, stack);
    }

    private int mapSlot(int slot, MachineSideMode mode) {
        if (mode == MachineSideMode.INPUT) {
            return slot >= 0 && slot < inputCount ? inputStart + slot : -1;
        }
        if (mode == MachineSideMode.OUTPUT) {
            return slot >= 0 && slot < outputCount ? outputStart + slot : -1;
        }
        return -1;
    }
}
