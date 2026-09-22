package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
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
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class FluidReservoirBlock extends Block {
    public FluidReservoirBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new FluidReservoirTileEntity();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getBlockEntity(pos);
        return tile instanceof FluidReservoirTileEntity
                ? ((FluidReservoirTileEntity) tile).getAnalogSignal()
                : 0;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getDirection())) {
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (!world.isClientSide) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof FluidReservoirTileEntity && player instanceof ServerPlayerEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (FluidReservoirTileEntity) tile, pos);
            }
            return ActionResultType.CONSUME;
        }
        return ActionResultType.SUCCESS;
    }
}
