package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.ChargingStationTileEntity;

public class ChargingStationBlock extends BaseMachineBlock<ChargingStationTileEntity> {
    public ChargingStationBlock(Properties properties) {
        super(properties, ChargingStationTileEntity.class, ChargingStationTileEntity::new);
    }
}
