package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.IndustrialWasherTileEntity;

public class IndustrialWasherBlock extends BaseMachineBlock<IndustrialWasherTileEntity> {
    public IndustrialWasherBlock(Properties properties) {
        super(properties, IndustrialWasherTileEntity.class, IndustrialWasherTileEntity::new);
    }
}
