package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum ItemFilterMode {
    WHITELIST,
    BLACKLIST;

    public ItemFilterMode next() {
        return this == WHITELIST ? BLACKLIST : WHITELIST;
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "routing.justguithings.filter_mode." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static ItemFilterMode fromOrdinal(int ordinal) {
        ItemFilterMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return WHITELIST;
        }
        return values[ordinal];
    }
}
