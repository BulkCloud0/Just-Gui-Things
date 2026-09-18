package com.bulkcloud0.justguithings.api.recipe;

import net.minecraft.util.ResourceLocation;

/**
 * Common metadata contract for energy-driven JGT processing recipes.
 *
 * Custom recipe implementations can use this contract when integrating with the reusable
 * processing-machine lifecycle.
 */
public interface MachineProcessingRecipe {
    ResourceLocation getId();

    int getProcessingTime();

    int getEnergyPerTick();
}
