package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;

public class EnergyCellBlock extends BaseMachineBlock<EnergyCellTileEntity> {
    public EnergyCellBlock(Properties properties) {
        super(properties, EnergyCellTileEntity.class, EnergyCellTileEntity::new);
    }
}
