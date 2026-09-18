package com.bulkcloud0.justguithings.machine;

public enum MachineSideMode {
    DISABLED,
    INPUT,
    OUTPUT,
    ENERGY,
    FLUID_INPUT,
    ENERGY_OUTPUT,
    ENERGY_BOTH;

    public MachineSideMode next() {
        MachineSideMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MachineSideMode fromOrdinal(int ordinal) {
        MachineSideMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return DISABLED;
        }
        return values[ordinal];
    }
}
