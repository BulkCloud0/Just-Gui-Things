package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.PrecisionExtruderTileEntity;

public class PrecisionExtruderBlock extends BaseMachineBlock<PrecisionExtruderTileEntity> {
    public PrecisionExtruderBlock(Properties properties) {
        super(properties, PrecisionExtruderTileEntity.class, PrecisionExtruderTileEntity::new);
    }
}
