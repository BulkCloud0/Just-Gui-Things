package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.CrusherTileEntity;

public class CrusherBlock extends BaseMachineBlock<CrusherTileEntity> {
    public CrusherBlock(Properties properties) {
        super(properties, CrusherTileEntity.class, CrusherTileEntity::new);
    }
}
