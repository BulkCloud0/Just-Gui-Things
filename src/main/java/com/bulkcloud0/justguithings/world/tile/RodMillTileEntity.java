package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseSingleInputProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.RodFormingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.RodMillContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class RodMillTileEntity extends BaseSingleInputProcessingMachineTileEntity<RodFormingRecipe> {
    public static final int CAPACITY = 140_000;
    public static final int MAX_RECEIVE = 1_600;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int MAX_MODULES_PER_TYPE = 4;
    public static final int DEFAULT_ENERGY_PER_TICK = 55;

    public RodMillTileEntity() {
        super(ModTileEntities.ROD_MILL.get(), ModRecipes.ROD_FORMING_TYPE,
                CAPACITY, MAX_RECEIVE, 4, DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        switch (slot) {
            case 2: return MachineModuleTypes.SPEED;
            case 3: return MachineModuleTypes.EFFICIENCY;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected int getEffectiveProcessingTime(RodFormingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(RodFormingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount(),
                1);
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.rod_mill");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RodMillContainer(windowId, playerInventory, this);
    }
}
