package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.AutoCrafterTileEntity;

public class AutoCrafterBlock extends BaseMachineBlock<AutoCrafterTileEntity> {
    public AutoCrafterBlock(Properties properties) {
        super(properties, AutoCrafterTileEntity.class, AutoCrafterTileEntity::new);
    }
}
