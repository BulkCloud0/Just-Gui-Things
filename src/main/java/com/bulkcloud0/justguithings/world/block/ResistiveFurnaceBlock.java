package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.ResistiveFurnaceTileEntity;

public class ResistiveFurnaceBlock extends BaseMachineBlock<ResistiveFurnaceTileEntity> {
    public ResistiveFurnaceBlock(Properties properties) {
        super(properties, ResistiveFurnaceTileEntity.class, ResistiveFurnaceTileEntity::new);
    }
}
