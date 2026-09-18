package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseSingleInputProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.HeatingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.ResistiveFurnaceContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class ResistiveFurnaceTileEntity extends BaseSingleInputProcessingMachineTileEntity<HeatingRecipe> {
    public static final int CAPACITY = 120_000;
    public static final int MAX_RECEIVE = 1_200;
    public static final int DEFAULT_PROCESS_TICKS = 140;
    public static final int DEFAULT_ENERGY_PER_TICK = 40;

    public ResistiveFurnaceTileEntity() {
        super(ModTileEntities.RESISTIVE_FURNACE.get(), ModRecipes.HEATING_TYPE,
                CAPACITY, MAX_RECEIVE, DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
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
