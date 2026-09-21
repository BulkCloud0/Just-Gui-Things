package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.AssemblerTileEntity;

public class AssemblerBlock extends BaseMachineBlock<AssemblerTileEntity> {
    public AssemblerBlock(Properties properties) {
        super(properties, AssemblerTileEntity.class, AssemblerTileEntity::new);
    }
}
