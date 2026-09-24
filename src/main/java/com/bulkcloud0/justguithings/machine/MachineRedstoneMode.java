package com.bulkcloud0.justguithings.machine;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum MachineRedstoneMode {
    ALWAYS,
    REQUIRE_SIGNAL,
    REQUIRE_NO_SIGNAL;

    public MachineRedstoneMode next() {
        MachineRedstoneMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean allows(boolean powered) {
        switch (this) {
            case REQUIRE_SIGNAL:
                return powered;
            case REQUIRE_NO_SIGNAL:
                return !powered;
            case ALWAYS:
            default:
                return true;
        }
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "machine_redstone_mode.justguithings." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static MachineRedstoneMode fromOrdinal(int ordinal) {
        MachineRedstoneMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ALWAYS;
        }
        return values[ordinal];
    }
}
