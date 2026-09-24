package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;

public class CoalGeneratorBlock extends BaseMachineBlock<CoalGeneratorTileEntity> {
    public CoalGeneratorBlock(Properties properties) {
        super(properties, CoalGeneratorTileEntity.class, CoalGeneratorTileEntity::new);
    }
}
