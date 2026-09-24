package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.VacuumCollectorTileEntity;

public class VacuumCollectorBlock extends BaseMachineBlock<VacuumCollectorTileEntity> {
    public VacuumCollectorBlock(Properties properties) {
        super(properties, VacuumCollectorTileEntity.class, VacuumCollectorTileEntity::new);
    }
}
