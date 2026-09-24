package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.IndustrialMixerTileEntity;

public class IndustrialMixerBlock extends BaseMachineBlock<IndustrialMixerTileEntity> {
    public IndustrialMixerBlock(Properties properties) {
        super(properties, IndustrialMixerTileEntity.class, IndustrialMixerTileEntity::new);
    }
}
