package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.FluidContainerStationTileEntity;

public class FluidContainerStationBlock extends BaseMachineBlock<FluidContainerStationTileEntity> {
    public FluidContainerStationBlock(Properties properties) {
        super(properties, FluidContainerStationTileEntity.class, FluidContainerStationTileEntity::new);
    }
}
