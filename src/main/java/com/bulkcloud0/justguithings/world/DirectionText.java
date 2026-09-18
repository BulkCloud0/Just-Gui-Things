package com.bulkcloud0.justguithings.world;

import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class DirectionText {
    private DirectionText() {
    }

    public static ITextComponent getDisplayName(Direction direction) {
        return new TranslationTextComponent(
                "direction.justguithings." + direction.toString());
    }
}
