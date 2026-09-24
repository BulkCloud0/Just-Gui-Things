package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.StampingPressTileEntity;

public class StampingPressBlock extends BaseMachineBlock<StampingPressTileEntity> {
    public StampingPressBlock(Properties properties) {
        super(properties, StampingPressTileEntity.class, StampingPressTileEntity::new);
    }
}
