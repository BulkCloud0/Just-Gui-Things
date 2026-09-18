package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum ItemRoutingRedstoneMode {
    ALWAYS,
    REQUIRE_SIGNAL,
    REQUIRE_NO_SIGNAL;

    public ItemRoutingRedstoneMode next() {
        ItemRoutingRedstoneMode[] values = values();
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
                "routing.justguithings.redstone_mode." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static ItemRoutingRedstoneMode fromOrdinal(int ordinal) {
        ItemRoutingRedstoneMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return ALWAYS;
        }
        return values[ordinal];
    }
}
