package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum RoutingControllerMode {
    PRIORITY,
    FILTER_SAMPLE,
    FILTER_MODE,
    NBT_MATCH;

    public RoutingControllerMode next() {
        RoutingControllerMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "routing.justguithings.controller_mode." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static RoutingControllerMode fromOrdinal(int ordinal) {
        RoutingControllerMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return PRIORITY;
        }
        return values[ordinal];
    }
}
