package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.AutomatedAssemblerTileEntity;

public class AutomatedAssemblerBlock extends BaseMachineBlock<AutomatedAssemblerTileEntity> {
    public AutomatedAssemblerBlock(Properties properties) {
        super(properties, AutomatedAssemblerTileEntity.class, AutomatedAssemblerTileEntity::new);
    }
}
