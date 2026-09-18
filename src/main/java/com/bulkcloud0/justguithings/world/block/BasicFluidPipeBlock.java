package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;
import java.util.Locale;

public class BasicFluidPipeBlock extends AbstractConduitBlock {
    public BasicFluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicFluidPipeBlock) {
            return true;
        }

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicFluidPipeTileEntity
                && ((BasicFluidPipeTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite()).isPresent();
    }

    private void invalidateAdjacentPipeCaches(World world, BlockPos pos) {
        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicFluidPipeTileEntity) {
            ((BasicFluidPipeTileEntity) self).invalidateNetworkCache();
        }

        for (Direction direction : Direction.values()) {
            TileEntity neighbor = world.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof BasicFluidPipeTileEntity) {
                ((BasicFluidPipeTileEntity) neighbor).invalidateNetworkCache();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && oldState.getBlock() != state.getBlock()) {
            invalidateAdjacentPipeCaches(world, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean moving) {
        if (!world.isClientSide && state.getBlock() != newState.getBlock()) {
            invalidateAdjacentPipeCaches(world, pos);
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        ItemStack held = player.getItemInHand(hand);
        TileEntity tile = world.getBlockEntity(pos);
        if (held.getItem() != ModItems.CONFIGURATOR.get() || !(tile instanceof BasicFluidPipeTileEntity)) {
            return ActionResultType.PASS;
        }

        if (!world.isClientSide) {
            BasicFluidPipeTileEntity pipe = (BasicFluidPipeTileEntity) tile;
            ConduitTransferMode mode = pipe.cycleSideMode(hit.getDirection());
            AbstractConduitBlock.refreshConnections(world, pos);

            String face = hit.getDirection().toString().toUpperCase(Locale.ROOT);
            player.displayClientMessage(new StringTextComponent(face + ": " + mode.name()), true);
        }

        return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new BasicFluidPipeTileEntity();
    }
}
