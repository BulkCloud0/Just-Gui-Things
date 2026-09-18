package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum ItemRoutingPriority {
    NORMAL,
    HIGH,
    LOW;

    public ItemRoutingPriority next() {
        switch (this) {
            case NORMAL:
                return HIGH;
            case HIGH:
                return LOW;
            case LOW:
            default:
                return NORMAL;
        }
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("routing.justguithings.priority." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static ItemRoutingPriority fromOrdinal(int ordinal) {
        ItemRoutingPriority[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return NORMAL;
        }
        return values[ordinal];
    }
}
