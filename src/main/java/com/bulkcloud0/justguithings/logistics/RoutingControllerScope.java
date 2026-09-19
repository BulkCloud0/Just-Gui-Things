package com.bulkcloud0.justguithings.logistics;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum RoutingControllerScope {
    TARGET,
    SOURCE;

    public RoutingControllerScope next() {
        return this == TARGET ? SOURCE : TARGET;
    }

    public RoutingControllerMode getDefaultMode() {
        return this == TARGET ? RoutingControllerMode.PRIORITY : RoutingControllerMode.FILTER_SAMPLE;
    }

    public boolean supports(RoutingControllerMode mode) {
        if (this == TARGET) {
            return mode != RoutingControllerMode.MIN_STOCK;
        }
        return mode != RoutingControllerMode.PRIORITY;
    }

    public RoutingControllerMode normalize(RoutingControllerMode mode) {
        return supports(mode) ? mode : getDefaultMode();
    }

    public RoutingControllerMode nextMode(RoutingControllerMode current) {
        RoutingControllerMode candidate = normalize(current);
        for (int index = 0; index < RoutingControllerMode.values().length; index++) {
            candidate = candidate.next();
            if (supports(candidate)) {
                return candidate;
            }
        }
        return getDefaultMode();
    }

    public ITextComponent getDisplayName() {
        return new TranslationTextComponent(
                "routing.justguithings.controller_scope." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static RoutingControllerScope fromOrdinal(int ordinal) {
        RoutingControllerScope[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TARGET;
        }
        return values[ordinal];
    }
}
