package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.IndustrialSawmillTileEntity;

public class IndustrialSawmillBlock extends BaseMachineBlock<IndustrialSawmillTileEntity> {
    public IndustrialSawmillBlock(Properties properties) {
        super(properties, IndustrialSawmillTileEntity.class, IndustrialSawmillTileEntity::new);
    }
}
