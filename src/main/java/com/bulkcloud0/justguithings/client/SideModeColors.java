package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.machine.MachineSideMode;

public final class SideModeColors {
    public static final int DISABLED = 0xFF7D8791;
    public static final int INPUT = 0xFF63C174;
    public static final int OUTPUT = 0xFFE29A4A;
    public static final int ENERGY = 0xFF45C7D9;
    public static final int FLUID_INPUT = 0xFF5B8DEF;
    public static final int FLUID_OUTPUT = 0xFF4EC6E6;

    private SideModeColors() {
    }

    public static int getConduitColor(ConduitTransferMode mode) {
        switch (mode) {
            case PULL:
                return INPUT;
            case PUSH:
                return OUTPUT;
            case BOTH:
                return ENERGY;
            case DISABLED:
            default:
                return DISABLED;
        }
    }

    public static int getMachineColor(MachineSideMode mode) {
        switch (mode) {
            case INPUT:
                return INPUT;
            case OUTPUT:
                return OUTPUT;
            case FLUID_INPUT:
                return FLUID_INPUT;
            case FLUID_OUTPUT:
                return FLUID_OUTPUT;
            case ENERGY:
            case ENERGY_OUTPUT:
            case ENERGY_BOTH:
            case ITEM_INPUT_ENERGY_OUTPUT:
                return ENERGY;
            case DISABLED:
            default:
                return DISABLED;
        }
    }

    public static float red(int argb) {
        return ((argb >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int argb) {
        return ((argb >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int argb) {
        return (argb & 0xFF) / 255.0F;
    }
}
