package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;

import java.util.function.Function;

public final class SideConfigContainerData implements IIntArray {
    private static final Direction[] DIRECTIONS = {
            Direction.UP,
            Direction.DOWN,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private final TileEntity tileEntity;
    private final Function<Direction, MachineSideMode> serverGetter;
    private final int[] clientValues = new int[DIRECTIONS.length];

    public SideConfigContainerData(TileEntity tileEntity, Function<Direction, MachineSideMode> serverGetter) {
        this.tileEntity = tileEntity;
        this.serverGetter = serverGetter;
    }

    @Override
    public int get(int index) {
        if (index < 0 || index >= DIRECTIONS.length) {
            return MachineSideMode.DISABLED.ordinal();
        }
        if (tileEntity.getLevel() != null && !tileEntity.getLevel().isClientSide) {
            return serverGetter.apply(DIRECTIONS[index]).ordinal();
        }
        return clientValues[index];
    }

    @Override
    public void set(int index, int value) {
        if (index >= 0 && index < clientValues.length) {
            clientValues[index] = value;
        }
    }

    @Override
    public int getCount() {
        return DIRECTIONS.length;
    }

    public MachineSideMode getMode(Direction direction) {
        for (int index = 0; index < DIRECTIONS.length; index++) {
            if (DIRECTIONS[index] == direction) {
                return MachineSideMode.fromOrdinal(get(index));
            }
        }
        return MachineSideMode.DISABLED;
    }
}
