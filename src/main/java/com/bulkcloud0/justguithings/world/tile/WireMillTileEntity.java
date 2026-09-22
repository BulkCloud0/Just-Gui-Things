package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseSingleInputProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.WireDrawingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.WireMillContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class WireMillTileEntity extends BaseSingleInputProcessingMachineTileEntity<WireDrawingRecipe> {
    public static final int CAPACITY = 130_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 150;
    public static final int MAX_MODULES_PER_TYPE = 4;
    public static final int DEFAULT_ENERGY_PER_TICK = 50;

    public WireMillTileEntity() {
        super(ModTileEntities.WIRE_MILL.get(), ModRecipes.WIRE_DRAWING_TYPE,
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
    protected int getEffectiveProcessingTime(WireDrawingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(WireDrawingRecipe recipe) {
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
        return new TranslationTextComponent("container.justguithings.wire_mill");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new WireMillContainer(windowId, playerInventory, this);
    }
}
