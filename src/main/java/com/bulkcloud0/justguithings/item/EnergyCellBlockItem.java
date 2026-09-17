package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class EnergyCellBlockItem extends BlockItem {
    public EnergyCellBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        int storedEnergy = 0;
        CompoundNBT root = stack.getTag();
        if (root != null && root.contains("BlockEntityTag", 10)) {
            CompoundNBT blockEntityTag = root.getCompound("BlockEntityTag");
            storedEnergy = Math.max(0, Math.min(EnergyCellTileEntity.CAPACITY, blockEntityTag.getInt("Energy")));
        }

        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.energy_cell.energy",
                storedEnergy,
                EnergyCellTileEntity.CAPACITY).withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.energy_cell.portable").withStyle(TextFormatting.DARK_GRAY));
    }
}
