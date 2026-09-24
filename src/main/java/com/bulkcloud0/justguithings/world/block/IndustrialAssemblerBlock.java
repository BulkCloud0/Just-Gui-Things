package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.IndustrialAssemblerTileEntity;

public class IndustrialAssemblerBlock extends BaseMachineBlock<IndustrialAssemblerTileEntity> {
    public IndustrialAssemblerBlock(Properties properties) {
        super(properties, IndustrialAssemblerTileEntity.class, IndustrialAssemblerTileEntity::new);
    }
}
