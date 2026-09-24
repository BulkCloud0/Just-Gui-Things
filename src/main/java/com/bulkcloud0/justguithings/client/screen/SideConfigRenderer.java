package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.client.SideModeColors;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Locale;
import java.util.function.Function;

public final class SideConfigRenderer {
    private static final Direction[] DIRECTIONS = {
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private SideConfigRenderer() {
    }

    public static void drawHorizontal(MatrixStack matrixStack, FontRenderer font, float x, float y,
                                      Function<Direction, MachineSideMode> modeGetter) {
        float cursor = x;
        for (Direction direction : DIRECTIONS) {
            MachineSideMode mode = modeGetter.apply(direction);
            String token = token(direction, mode);
            font.draw(matrixStack, token, cursor, y, SideModeColors.getMachineColor(mode));
            cursor += font.width(token) + 3.0F;
        }
    }

    public static void drawCompactGrid(MatrixStack matrixStack, FontRenderer font, float x, float y,
                                       Function<Direction, MachineSideMode> modeGetter) {
        MachineSideMode[] modes = new MachineSideMode[DIRECTIONS.length];
        String[] tokens = new String[DIRECTIONS.length];
        float columnWidth = 0.0F;

        for (int index = 0; index < DIRECTIONS.length; index++) {
            modes[index] = modeGetter.apply(DIRECTIONS[index]);
            tokens[index] = token(DIRECTIONS[index], modes[index]);
            columnWidth = Math.max(columnWidth, font.width(tokens[index]) + 4.0F);
        }

        for (int index = 0; index < DIRECTIONS.length; index++) {
            int row = index / 3;
            int column = index % 3;
            font.draw(
                    matrixStack,
                    tokens[index],
                    x + column * columnWidth,
                    y + row * 10.0F,
                    SideModeColors.getMachineColor(modes[index]));
        }
    }

    private static String token(Direction direction, MachineSideMode mode) {
        return directionCode(direction) + ":" + modeCode(mode);
    }

    private static String directionCode(Direction direction) {
        return new TranslationTextComponent(
                "side_config.justguithings.direction." + direction.toString()).getString();
    }

    private static String modeCode(MachineSideMode mode) {
        return new TranslationTextComponent(
                "side_config.justguithings.mode."
                        + mode.name().toLowerCase(Locale.ROOT)).getString();
    }
}
