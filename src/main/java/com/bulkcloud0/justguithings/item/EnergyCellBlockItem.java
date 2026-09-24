package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
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
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class EnergyCellBlockItem extends BlockItem {
    public EnergyCellBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energyCapability =
                    LazyOptional.of(() -> new EnergyCellItemEnergyStorage(stack));

            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                if (cap == CapabilityEnergy.ENERGY) {
                    return energyCapability.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        int storedEnergy = EnergyCellItemEnergyStorage.getStoredEnergy(stack);

        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.energy_cell.energy",
                storedEnergy,
                EnergyCellTileEntity.CAPACITY).withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.energy_cell.portable").withStyle(TextFormatting.DARK_GRAY));
    }
}
