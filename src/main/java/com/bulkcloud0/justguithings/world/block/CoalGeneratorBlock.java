package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class CoalGeneratorBlock extends Block {
    public CoalGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new CoalGeneratorTileEntity();
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (!world.isClientSide) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof CoalGeneratorTileEntity && player instanceof ServerPlayerEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (CoalGeneratorTileEntity) tile, pos);
            }
            return ActionResultType.CONSUME;
        }

        return ActionResultType.SUCCESS;
    }
}
