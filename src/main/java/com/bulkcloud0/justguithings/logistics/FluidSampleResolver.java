package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public final class FluidSampleResolver {
    private FluidSampleResolver() {
    }

    public static FluidStack resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }

        IFluidHandlerItem handler = stack
                .getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
                .orElse(null);
        if (handler != null) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty()) {
                    FluidStack copy = fluid.copy();
                    copy.setAmount(1);
                    return copy;
                }
            }
        }

        CompoundNBT root = stack.getTag();
        if (root != null && root.contains("BlockEntityTag", 10)) {
            CompoundNBT blockEntity = root.getCompound("BlockEntityTag");
            if (blockEntity.contains("Tank", 10)) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(blockEntity.getCompound("Tank"));
                if (!fluid.isEmpty()) {
                    fluid.setAmount(1);
                    return fluid;
                }
            }
        }

        return FluidStack.EMPTY;
    }
}
