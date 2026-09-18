package com.bulkcloud0.justguithings.api.machine.module;

import com.bulkcloud0.justguithings.JustGuiThings;
import net.minecraft.util.ResourceLocation;

/**
 * Stable module type identifiers understood by the built-in JGT machines.
 */
public final class MachineModuleTypes {
    public static final ResourceLocation SPEED = id("speed");
    public static final ResourceLocation EFFICIENCY = id("efficiency");
    public static final ResourceLocation BUFFER = id("buffer");
    public static final ResourceLocation BATCH = id("batch");

    private MachineModuleTypes() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(JustGuiThings.MOD_ID, path);
    }
}
