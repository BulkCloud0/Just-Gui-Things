package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

public class FluidReservoirBlockItem extends BlockItem {
    public FluidReservoirBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        FluidStack fluid = FluidStack.EMPTY;
        CompoundNBT root = stack.getTag();
        if (root != null && root.contains("BlockEntityTag", 10)) {
            CompoundNBT be = root.getCompound("BlockEntityTag");
            if (be.contains("Tank", 10)) {
                fluid = FluidStack.loadFluidStackFromNBT(be.getCompound("Tank"));
            }
        }
        tooltip.add(new TranslationTextComponent("tooltip.justguithings.fluid_reservoir.amount",
                fluid.getAmount(), FluidReservoirTileEntity.CAPACITY).withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("tooltip.justguithings.fluid_reservoir.portable")
                .withStyle(TextFormatting.DARK_GRAY));
    }
}
