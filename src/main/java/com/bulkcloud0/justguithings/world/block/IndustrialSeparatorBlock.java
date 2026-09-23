package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.IndustrialSeparatorTileEntity;

public class IndustrialSeparatorBlock extends BaseMachineBlock<IndustrialSeparatorTileEntity> {
    public IndustrialSeparatorBlock(Properties properties) {
        super(properties, IndustrialSeparatorTileEntity.class, IndustrialSeparatorTileEntity::new);
    }
}
