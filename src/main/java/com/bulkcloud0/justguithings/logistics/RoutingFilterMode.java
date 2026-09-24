package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum RoutingFilterMode {
    WHITELIST,
    BLACKLIST;

    public RoutingFilterMode next() {
        return this == WHITELIST ? BLACKLIST : WHITELIST;
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "routing.justguithings.filter_mode." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static RoutingFilterMode fromOrdinal(int ordinal) {
        RoutingFilterMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return WHITELIST;
        }
        return values[ordinal];
    }
}
