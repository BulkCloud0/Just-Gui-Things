package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class BasicEnergyCableBlock extends Block {
    public BasicEnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new BasicEnergyCableTileEntity();
    }
}
