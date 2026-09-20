package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.SteamGeneratorTileEntity;

public class SteamGeneratorBlock extends BaseMachineBlock<SteamGeneratorTileEntity> {
    public SteamGeneratorBlock(Properties properties) {
        super(properties, SteamGeneratorTileEntity.class, SteamGeneratorTileEntity::new);
    }
}
