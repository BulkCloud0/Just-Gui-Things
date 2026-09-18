package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseSingleInputProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.recipe.HeatingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.ResistiveFurnaceContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class ResistiveFurnaceTileEntity extends BaseSingleInputProcessingMachineTileEntity<HeatingRecipe> {
    public static final int CAPACITY = 120_000;
    public static final int MAX_RECEIVE = 1_200;
    public static final int DEFAULT_PROCESS_TICKS = 140;
    public static final int DEFAULT_ENERGY_PER_TICK = 40;

    private static final int INDUCTION_SLOT = 2;

    public ResistiveFurnaceTileEntity() {
        super(ModTileEntities.RESISTIVE_FURNACE.get(), ModRecipes.HEATING_TYPE,
                CAPACITY, MAX_RECEIVE, 3, DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        return slot == INDUCTION_SLOT ? MachineModuleTypes.INDUCTION_COIL : null;
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MachineModuleTypes.INDUCTION_COIL.equals(moduleType) ? 1 : super.getModuleSlotLimit(slot, moduleType);
    }

    @Override
    protected int getEffectiveProcessingTime(HeatingRecipe recipe) {
        if (!hasInductionCoil()) {
            return recipe.getProcessingTime();
        }
        return Math.max(1, (recipe.getProcessingTime() + 1) / 2);
    }

    @Override
    protected int getEffectiveEnergyPerTick(HeatingRecipe recipe) {
        if (!hasInductionCoil()) {
            return recipe.getEnergyPerTick();
        }
        return Math.max(1, (recipe.getEnergyPerTick() * 5 + 1) / 2);
    }

    public boolean hasInductionCoil() {
        return getModuleCount(MachineModuleTypes.INDUCTION_COIL) > 0;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.resistive_furnace");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ResistiveFurnaceContainer(windowId, playerInventory, this);
    }
}
