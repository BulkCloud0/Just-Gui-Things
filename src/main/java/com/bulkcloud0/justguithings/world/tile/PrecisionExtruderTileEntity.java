package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseSingleInputProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.ExtrudingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.PrecisionExtruderContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class PrecisionExtruderTileEntity extends BaseSingleInputProcessingMachineTileEntity<ExtrudingRecipe> {
    public static final int CAPACITY = 140_000;
    public static final int MAX_RECEIVE = 1_600;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 55;

    public PrecisionExtruderTileEntity() {
        super(ModTileEntities.PRECISION_EXTRUDER.get(), ModRecipes.EXTRUDING_TYPE,
                CAPACITY, MAX_RECEIVE, DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.precision_extruder");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PrecisionExtruderContainer(windowId, playerInventory, this);
    }
}
