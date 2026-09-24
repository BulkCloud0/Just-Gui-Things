package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EnergyCellBlock extends BaseMachineBlock<EnergyCellTileEntity> {
    public EnergyCellBlock(Properties properties) {
        super(properties, EnergyCellTileEntity.class, EnergyCellTileEntity::new);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getBlockEntity(pos);
        return tile instanceof EnergyCellTileEntity
                ? ((EnergyCellTileEntity) tile).getAnalogSignal()
                : 0;
    }
}
