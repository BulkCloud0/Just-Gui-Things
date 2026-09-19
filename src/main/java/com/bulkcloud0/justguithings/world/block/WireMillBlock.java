package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.WireMillTileEntity;

public class WireMillBlock extends BaseMachineBlock<WireMillTileEntity> {
    public WireMillBlock(Properties properties) {
        super(properties, WireMillTileEntity.class, WireMillTileEntity::new);
    }
}
