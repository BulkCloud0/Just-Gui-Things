package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.RoutingControllerItem;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.ItemFilterMode;
import com.bulkcloud0.justguithings.logistics.ItemFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.ItemRouteFilter;
import com.bulkcloud0.justguithings.logistics.ItemRoutingPriority;
import com.bulkcloud0.justguithings.logistics.ItemRoutingRedstoneMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
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
import net.minecraft.util.text.TranslationTextComponent;
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
                && ((BasicItemPipeTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        direction.getOpposite()).isPresent();
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
        TileEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof BasicItemPipeTileEntity)) {
            return ActionResultType.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        BasicItemPipeTileEntity pipe = (BasicItemPipeTileEntity) tile;
        Direction faceDirection = hit.getDirection();

        if (held.getItem() == ModItems.CONFIGURATOR.get()) {
            if (!world.isClientSide) {
                ConduitTransferMode mode = pipe.cycleSideMode(faceDirection);
                AbstractConduitBlock.refreshConnections(world, pos);

                String face = faceDirection.toString().toUpperCase(Locale.ROOT);
                player.displayClientMessage(new StringTextComponent(face + ": " + mode.name()), true);
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (held.getItem() == ModItems.ROUTING_CONTROLLER.get()) {
            if (!world.isClientSide) {
                applyRoutingController(
                        pipe, faceDirection, player, hand, RoutingControllerItem.getMode(held));
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        return ActionResultType.PASS;
    }

    private void applyRoutingController(BasicItemPipeTileEntity pipe, Direction direction,
                                        PlayerEntity player, Hand controllerHand,
                                        RoutingControllerMode controllerMode) {
        String face = direction.toString().toUpperCase(Locale.ROOT);

        switch (controllerMode) {
            case FILTER_SAMPLE:
                Hand sampleHand = controllerHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                ItemStack sample = player.getItemInHand(sampleHand);
                if (sample.isEmpty()) {
                    pipe.clearTargetFilter(direction);
                    player.displayClientMessage(
                            new TranslationTextComponent(
                                    "message.justguithings.routing_controller.filter_cleared", face),
                            true);
                } else {
                    ItemFilterSampleChange change = pipe.toggleTargetFilterSample(direction, sample);
                    int count = pipe.getTargetFilterSampleCount(direction);

                    switch (change) {
                        case ADDED:
                            player.displayClientMessage(
                                    new TranslationTextComponent(
                                            "message.justguithings.routing_controller.filter_added",
                                            face, sample.getHoverName(), count, ItemRouteFilter.MAX_SAMPLES),
                                    true);
                            break;
                        case REMOVED:
                            player.displayClientMessage(
                                    new TranslationTextComponent(
                                            "message.justguithings.routing_controller.filter_removed",
                                            face, sample.getHoverName(), count, ItemRouteFilter.MAX_SAMPLES),
                                    true);
                            break;
                        case FULL:
                        default:
                            player.displayClientMessage(
                                    new TranslationTextComponent(
                                            "message.justguithings.routing_controller.filter_full",
                                            face, ItemRouteFilter.MAX_SAMPLES),
                                    true);
                            break;
                    }
                }
                break;

            case FILTER_MODE:
                ItemFilterMode filterMode = pipe.cycleTargetFilterMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.filter_mode",
                                face, filterMode.getDisplayName()),
                        true);
                break;

            case NBT_MATCH:
                boolean matchNbt = pipe.toggleTargetFilterNbt(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                matchNbt
                                        ? "message.justguithings.routing_controller.nbt_exact"
                                        : "message.justguithings.routing_controller.nbt_ignored",
                                face),
                        true);
                break;

            case REDSTONE:
                ItemRoutingRedstoneMode redstoneMode = pipe.cycleTargetRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
                break;

            case PRIORITY:
            default:
                ItemRoutingPriority priority = pipe.cycleTargetPriority(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.priority",
                                face, priority.getDisplayName()),
                        true);
                break;
        }
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
