package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.ItemBufferTileEntity;

public class ItemBufferBlock extends BaseMachineBlock<ItemBufferTileEntity> {
    public ItemBufferBlock(Properties properties) {
        super(properties, ItemBufferTileEntity.class, ItemBufferTileEntity::new);
    }
}
