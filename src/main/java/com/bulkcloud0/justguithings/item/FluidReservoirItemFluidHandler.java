package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;

final class FluidReservoirItemFluidHandler implements IFluidHandlerItem {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String TANK_TAG = "Tank";

    private final ItemStack stack;

    FluidReservoirItemFluidHandler(ItemStack stack) {
        this.stack = stack;
    }

    @Nonnull
    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? getStoredFluid(stack) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? FluidReservoirTileEntity.CAPACITY : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack fluid) {
        return tank == 0 && !fluid.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        FluidStack stored = getStoredFluid(stack);
        if (!stored.isEmpty() && !stored.isFluidEqual(resource)) {
            return 0;
        }

        int space = FluidReservoirTileEntity.CAPACITY - stored.getAmount();
        int accepted = Math.min(space, resource.getAmount());
        if (accepted <= 0) {
            return 0;
        }

        if (action.execute()) {
            FluidStack updated;
            if (stored.isEmpty()) {
                updated = resource.copy();
                updated.setAmount(accepted);
            } else {
                updated = stored.copy();
                updated.grow(accepted);
            }
            setStoredFluid(updated);
        }
        return accepted;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack stored = getStoredFluid(stack);
        if (stored.isEmpty() || !stored.isFluidEqual(resource)) {
            return FluidStack.EMPTY;
        }

        return drain(Math.min(resource.getAmount(), stored.getAmount()), action);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack stored = getStoredFluid(stack);
        if (stored.isEmpty()) {
            return FluidStack.EMPTY;
        }

        int drainedAmount = Math.min(maxDrain, stored.getAmount());
        FluidStack drained = stored.copy();
        drained.setAmount(drainedAmount);

        if (action.execute()) {
            FluidStack remaining = stored.copy();
            remaining.shrink(drainedAmount);
            setStoredFluid(remaining);
        }

        return drained;
    }

    static FluidStack getStoredFluid(ItemStack stack) {
        CompoundNBT root = stack.getTag();
        if (root == null || !root.contains(BLOCK_ENTITY_TAG, 10)) {
            return FluidStack.EMPTY;
        }

        CompoundNBT blockEntityTag = root.getCompound(BLOCK_ENTITY_TAG);
        if (!blockEntityTag.contains(TANK_TAG, 10)) {
            return FluidStack.EMPTY;
        }

        FluidStack fluid = FluidStack.loadFluidStackFromNBT(blockEntityTag.getCompound(TANK_TAG));
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack clamped = fluid.copy();
        clamped.setAmount(Math.max(0, Math.min(
                FluidReservoirTileEntity.CAPACITY,
                fluid.getAmount())));
        return clamped.getAmount() > 0 ? clamped : FluidStack.EMPTY;
    }

    private void setStoredFluid(FluidStack fluid) {
        CompoundNBT root = stack.getOrCreateTag();
        CompoundNBT blockEntityTag = root.contains(BLOCK_ENTITY_TAG, 10)
                ? root.getCompound(BLOCK_ENTITY_TAG)
                : new CompoundNBT();

        if (fluid.isEmpty() || fluid.getAmount() <= 0) {
            blockEntityTag.remove(TANK_TAG);
        } else {
            FluidStack clamped = fluid.copy();
            clamped.setAmount(Math.min(
                    FluidReservoirTileEntity.CAPACITY,
                    clamped.getAmount()));

            CompoundNBT tankTag = new CompoundNBT();
            clamped.writeToNBT(tankTag);
            blockEntityTag.put(TANK_TAG, tankTag);
        }

        root.put(BLOCK_ENTITY_TAG, blockEntityTag);
    }
}
