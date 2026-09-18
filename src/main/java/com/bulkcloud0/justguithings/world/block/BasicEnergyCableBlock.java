package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nullable;

public class BasicEnergyCableBlock extends AbstractConduitBlock {
    public BasicEnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicEnergyCableBlock) {
            return true;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).isPresent();
    }

    private void invalidateAdjacentCableCaches(World world, BlockPos pos) {
        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicEnergyCableTileEntity) {
            ((BasicEnergyCableTileEntity) self).invalidateNetworkCache();
        }
        for (Direction direction : Direction.values()) {
            TileEntity neighbor = world.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof BasicEnergyCableTileEntity) {
                ((BasicEnergyCableTileEntity) neighbor).invalidateNetworkCache();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && oldState.getBlock() != state.getBlock()) {
            invalidateAdjacentCableCaches(world, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean moving) {
        if (!world.isClientSide && state.getBlock() != newState.getBlock()) {
            invalidateAdjacentCableCaches(world, pos);
        }
        super.onRemove(state, world, pos, newState, moving);
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
