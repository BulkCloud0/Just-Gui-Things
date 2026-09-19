package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.RodMillTileEntity;

public class RodMillBlock extends BaseMachineBlock<RodMillTileEntity> {
    public RodMillBlock(Properties properties) {
        super(properties, RodMillTileEntity.class, RodMillTileEntity::new);
    }
}
