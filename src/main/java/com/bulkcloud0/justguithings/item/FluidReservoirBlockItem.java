package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class FluidReservoirBlockItem extends BlockItem {
    public FluidReservoirBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IFluidHandlerItem> fluidCapability =
                    LazyOptional.of(() -> new FluidReservoirItemFluidHandler(stack));

            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                if (cap == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY) {
                    return fluidCapability.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        FluidStack fluid = FluidReservoirItemFluidHandler.getStoredFluid(stack);
        tooltip.add(new TranslationTextComponent("tooltip.justguithings.fluid_reservoir.amount",
                fluid.getAmount(), FluidReservoirTileEntity.CAPACITY).withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("tooltip.justguithings.fluid_reservoir.portable")
                .withStyle(TextFormatting.DARK_GRAY));
    }
}
