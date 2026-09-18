package com.bulkcloud0.justguithings.recipe;

import net.minecraft.util.ResourceLocation;

public interface MachineProcessingRecipe {
    ResourceLocation getId();

    int getProcessingTime();

    int getEnergyPerTick();
}
