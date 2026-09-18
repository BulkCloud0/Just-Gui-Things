package com.bulkcloud0.justguithings.machine;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum MachineSideMode {
    DISABLED,
    INPUT,
    OUTPUT,
    ENERGY,
    FLUID_INPUT,
    ENERGY_OUTPUT,
    ENERGY_BOTH,
    FLUID_OUTPUT;

    public MachineSideMode next() {
        MachineSideMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "machine_side_mode.justguithings." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static MachineSideMode fromOrdinal(int ordinal) {
        MachineSideMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return DISABLED;
        }
        return values[ordinal];
    }
}
