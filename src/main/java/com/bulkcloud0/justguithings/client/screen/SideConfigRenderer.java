package com.bulkcloud0.justguithings.client.screen;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.Direction;

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
    private static final String[] LABELS = {"U", "D", "N", "S", "W", "E"};

    private static final int DISABLED_COLOR = 0xFF7D8791;
    private static final int INPUT_COLOR = 0xFF63C174;
    private static final int OUTPUT_COLOR = 0xFFE29A4A;
    private static final int ENERGY_COLOR = 0xFF45C7D9;
    private static final int FLUID_INPUT_COLOR = 0xFF5B8DEF;

    private SideConfigRenderer() {
    }

    public static void drawHorizontal(MatrixStack matrixStack, FontRenderer font, float x, float y,
                                      Function<Direction, MachineSideMode> modeGetter) {
        float cursor = x;
        for (int index = 0; index < DIRECTIONS.length; index++) {
            MachineSideMode mode = modeGetter.apply(DIRECTIONS[index]);
            String token = LABELS[index] + ":" + code(mode);
            font.draw(matrixStack, token, cursor, y, color(mode));
            cursor += font.width(token) + 3.0F;
        }
    }

    public static void drawCompactGrid(MatrixStack matrixStack, FontRenderer font, float x, float y,
                                       Function<Direction, MachineSideMode> modeGetter) {
        for (int index = 0; index < DIRECTIONS.length; index++) {
            int row = index / 3;
            int column = index % 3;
            MachineSideMode mode = modeGetter.apply(DIRECTIONS[index]);
            String token = LABELS[index] + ":" + code(mode);
            font.draw(matrixStack, token, x + column * 21.0F, y + row * 10.0F, color(mode));
        }
    }

    private static String code(MachineSideMode mode) {
        switch (mode) {
            case INPUT:
                return "I";
            case OUTPUT:
                return "O";
            case ENERGY:
                return "E";
            case FLUID_INPUT:
                return "F";
            case ENERGY_OUTPUT:
                return "EO";
            case ENERGY_BOTH:
                return "EB";
            case DISABLED:
            default:
                return "-";
        }
    }

    private static int color(MachineSideMode mode) {
        switch (mode) {
            case INPUT:
                return INPUT_COLOR;
            case OUTPUT:
                return OUTPUT_COLOR;
            case ENERGY:
                return ENERGY_COLOR;
            case FLUID_INPUT:
                return FLUID_INPUT_COLOR;
            case ENERGY_OUTPUT:
            case ENERGY_BOTH:
                return ENERGY_COLOR;
            case DISABLED:
            default:
                return DISABLED_COLOR;
        }
    }
}
