package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.ItemBufferTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBufferBlock extends BaseMachineBlock<ItemBufferTileEntity> {
    public ItemBufferBlock(Properties properties) {
        super(properties, ItemBufferTileEntity.class, ItemBufferTileEntity::new);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getBlockEntity(pos);
        return tile instanceof ItemBufferTileEntity
                ? ((ItemBufferTileEntity) tile).getAnalogSignal()
                : 0;
    }
}
