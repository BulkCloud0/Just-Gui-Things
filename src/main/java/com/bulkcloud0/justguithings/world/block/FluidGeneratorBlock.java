package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.FluidGeneratorTileEntity;

public class FluidGeneratorBlock extends BaseMachineBlock<FluidGeneratorTileEntity> {
    public FluidGeneratorBlock(Properties properties) {
        super(properties, FluidGeneratorTileEntity.class, FluidGeneratorTileEntity::new);
    }
}
