package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.logistics.ItemPipeSideMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
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
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.Locale;

public class BasicItemPipeBlock extends AbstractConduitBlock {
    public BasicItemPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicItemPipeBlock) {
            return true;
        }

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicItemPipeTileEntity
                && ((BasicItemPipeTileEntity) self).getSideMode(direction) == ItemPipeSideMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).isPresent();
    }

    private void invalidateAdjacentPipeCaches(World world, BlockPos pos) {
        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicItemPipeTileEntity) {
            ((BasicItemPipeTileEntity) self).invalidateNetworkCache();
        }

        for (Direction direction : Direction.values()) {
            TileEntity neighbor = world.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof BasicItemPipeTileEntity) {
                ((BasicItemPipeTileEntity) neighbor).invalidateNetworkCache();
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
        if (held.getItem() != ModItems.CONFIGURATOR.get() || !(tile instanceof BasicItemPipeTileEntity)) {
            return ActionResultType.PASS;
        }

        if (!world.isClientSide) {
            BasicItemPipeTileEntity pipe = (BasicItemPipeTileEntity) tile;
            ItemPipeSideMode mode = pipe.cycleSideMode(hit.getDirection());
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
        return new BasicItemPipeTileEntity();
    }
}
