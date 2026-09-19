package com.bulkcloud0.justguithings.api.machine.module;

import com.bulkcloud0.justguithings.JustGuiThings;
import net.minecraft.util.ResourceLocation;

/**
 * Stable identifiers for the built-in JGT machine module families.
 */
public final class MachineModuleTypes {
    public static final ResourceLocation SPEED = id("speed");
    public static final ResourceLocation EFFICIENCY = id("efficiency");
    public static final ResourceLocation BUFFER = id("buffer");
    public static final ResourceLocation BATCH = id("batch");
    public static final ResourceLocation POWER_COIL = id("power_coil");

    private MachineModuleTypes() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(JustGuiThings.MOD_ID, path);
    }
}
