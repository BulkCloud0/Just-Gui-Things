package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.RoutingControllerItem;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.ItemRouteFilter;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
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
        if (!isPositionLoaded(world, neighborPos)) {
            return false;
        }

        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicItemPipeBlock) {
            return true;
        }

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicItemPipeTileEntity
                && ((BasicItemPipeTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = getLoadedBlockEntity(world, neighborPos);
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
            TileEntity neighbor = getLoadedBlockEntity(world, pos.relative(direction));
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
            if (player.isShiftKeyDown()) {
                return ActionResultType.PASS;
            }
            if (!world.isClientSide) {
                RoutingControllerScope scope = RoutingControllerItem.getScope(held);
                RoutingControllerMode mode = RoutingControllerItem.getMode(held, scope);

                if (scope == RoutingControllerScope.SOURCE) {
                    applySourceRoutingController(pipe, faceDirection, player, hand, mode);
                } else {
                    applyTargetRoutingController(pipe, faceDirection, player, hand, mode);
                }
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        return ActionResultType.PASS;
    }

    private void applyTargetRoutingController(BasicItemPipeTileEntity pipe, Direction direction,
                                              PlayerEntity player, Hand controllerHand,
                                              RoutingControllerMode controllerMode) {
        String face = direction.toString().toUpperCase(Locale.ROOT);

        switch (controllerMode) {
            case FILTER_SAMPLE:
                applyTargetFilterSample(pipe, direction, player, controllerHand, face);
                break;

            case FILTER_MODE:
                RoutingFilterMode filterMode = pipe.cycleTargetFilterMode(direction);
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
                RoutingRedstoneMode redstoneMode = pipe.cycleTargetRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
                break;

            case PRIORITY:
            default:
                RoutingPriority priority = pipe.cycleTargetPriority(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.priority",
                                face, priority.getDisplayName()),
                        true);
                break;
        }
    }

    private void applySourceRoutingController(BasicItemPipeTileEntity pipe, Direction direction,
                                              PlayerEntity player, Hand controllerHand,
                                              RoutingControllerMode controllerMode) {
        String face = direction.toString().toUpperCase(Locale.ROOT);

        switch (controllerMode) {
            case FILTER_SAMPLE:
                applySourceFilterSample(pipe, direction, player, controllerHand, face);
                break;

            case FILTER_MODE:
                RoutingFilterMode filterMode = pipe.cycleSourceFilterMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.source_filter_mode",
                                face, filterMode.getDisplayName()),
                        true);
                break;

            case NBT_MATCH:
                boolean matchNbt = pipe.toggleSourceFilterNbt(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                matchNbt
                                        ? "message.justguithings.routing_controller.source_nbt_exact"
                                        : "message.justguithings.routing_controller.source_nbt_ignored",
                                face),
                        true);
                break;

            case REDSTONE:
                RoutingRedstoneMode redstoneMode = pipe.cycleSourceRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.source_redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
                break;

            case MIN_STOCK:
            default:
                int minStock = pipe.cycleSourceMinStock(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.min_stock",
                                face, minStock),
                        true);
                break;
        }
    }

    private void applyTargetFilterSample(BasicItemPipeTileEntity pipe, Direction direction,
                                         PlayerEntity player, Hand controllerHand, String face) {
        Hand sampleHand = controllerHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack sample = player.getItemInHand(sampleHand);

        if (sample.isEmpty()) {
            pipe.clearTargetFilter(direction);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.filter_cleared", face),
                    true);
            return;
        }

        RoutingFilterSampleChange change = pipe.toggleTargetFilterSample(direction, sample);
        int count = pipe.getTargetFilterSampleCount(direction);
        displayFilterSampleChange(player, face, sample, count, change, false);
    }

    private void applySourceFilterSample(BasicItemPipeTileEntity pipe, Direction direction,
                                         PlayerEntity player, Hand controllerHand, String face) {
        Hand sampleHand = controllerHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack sample = player.getItemInHand(sampleHand);

        if (sample.isEmpty()) {
            pipe.clearSourceFilter(direction);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.source_filter_cleared", face),
                    true);
            return;
        }

        RoutingFilterSampleChange change = pipe.toggleSourceFilterSample(direction, sample);
        int count = pipe.getSourceFilterSampleCount(direction);
        displayFilterSampleChange(player, face, sample, count, change, true);
    }

    private void displayFilterSampleChange(PlayerEntity player, String face, ItemStack sample,
                                           int count, RoutingFilterSampleChange change,
                                           boolean source) {
        String prefix = source
                ? "message.justguithings.routing_controller.source_filter_"
                : "message.justguithings.routing_controller.filter_";

        switch (change) {
            case ADDED:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "added",
                                face, sample.getHoverName(), count, ItemRouteFilter.MAX_SAMPLES),
                        true);
                break;
            case REMOVED:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "removed",
                                face, sample.getHoverName(), count, ItemRouteFilter.MAX_SAMPLES),
                        true);
                break;
            case FULL:
            default:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "full",
                                face, ItemRouteFilter.MAX_SAMPLES),
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
