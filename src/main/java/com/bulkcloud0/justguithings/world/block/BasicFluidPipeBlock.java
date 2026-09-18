package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.RoutingControllerItem;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.FluidRouteFilter;
import com.bulkcloud0.justguithings.logistics.FluidSampleResolver;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
import com.bulkcloud0.justguithings.logistics.RoutingFilterMode;
import com.bulkcloud0.justguithings.logistics.RoutingFilterSampleChange;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.DirectionText;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;

public class BasicFluidPipeBlock extends AbstractConduitBlock {
    public BasicFluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (!isPositionLoaded(world, neighborPos)) {
            return false;
        }

        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicFluidPipeBlock) {
            return true;
        }

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicFluidPipeTileEntity
                && ((BasicFluidPipeTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = getLoadedBlockEntity(world, neighborPos);
        return neighbor != null
                && neighbor.getCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                        direction.getOpposite()).isPresent();
    }

    private void invalidateAdjacentPipeCaches(World world, BlockPos pos) {
        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicFluidPipeTileEntity) {
            ((BasicFluidPipeTileEntity) self).invalidateNetworkCache();
        }

        for (Direction direction : Direction.values()) {
            TileEntity neighbor = getLoadedBlockEntity(world, pos.relative(direction));
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
        TileEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof BasicFluidPipeTileEntity)) {
            return ActionResultType.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        BasicFluidPipeTileEntity pipe = (BasicFluidPipeTileEntity) tile;
        Direction direction = hit.getDirection();

        if (held.getItem() == ModItems.CONFIGURATOR.get()) {
            if (player.isShiftKeyDown()) {
                return ActionResultType.PASS;
            }
            if (!world.isClientSide) {
                ConduitTransferMode mode = pipe.cycleSideMode(direction);
                AbstractConduitBlock.refreshConnections(world, pos);

                ITextComponent face = DirectionText.getDisplayName(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.fluid_pipe.side_mode",
                                face, mode.getDisplayName()),
                        true);
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

                if (mode == RoutingControllerMode.COPY_RULE) {
                    RoutingControllerItem.copyFluidRoutingRule(held, scope, pipe, direction);
                    RoutingControllerItem.displayRuleCopied(player, scope, direction);
                } else if (mode == RoutingControllerMode.PASTE_RULE) {
                    RoutingControllerItem.displayRulePasteResult(
                            player, scope, direction,
                            RoutingControllerItem.pasteFluidRoutingRule(held, scope, pipe, direction));
                } else if (scope == RoutingControllerScope.SOURCE) {
                    applySourceRoutingController(pipe, direction, player, hand, mode);
                } else {
                    applyTargetRoutingController(pipe, direction, player, hand, mode);
                }
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        return ActionResultType.PASS;
    }

    private void applyTargetRoutingController(BasicFluidPipeTileEntity pipe, Direction direction,
                                              PlayerEntity player, Hand controllerHand,
                                              RoutingControllerMode controllerMode) {
        ITextComponent face = DirectionText.getDisplayName(direction);

        switch (controllerMode) {
            case FILTER_SAMPLE:
                applyFilterSample(pipe, direction, player, controllerHand, face, false);
                break;
            case FILTER_MODE:
                RoutingFilterMode filterMode = pipe.cycleTargetFilterMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_filter_mode",
                                face, filterMode.getDisplayName()),
                        true);
                break;
            case NBT_MATCH:
                boolean matchNbt = pipe.toggleTargetFilterNbt(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                matchNbt
                                        ? "message.justguithings.routing_controller.fluid_nbt_exact"
                                        : "message.justguithings.routing_controller.fluid_nbt_ignored",
                                face),
                        true);
                break;
            case REDSTONE:
                RoutingRedstoneMode redstoneMode = pipe.cycleTargetRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
                break;
            case PRIORITY:
            default:
                RoutingPriority priority = pipe.cycleTargetPriority(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_priority",
                                face, priority.getDisplayName()),
                        true);
                break;
        }
    }

    private void applySourceRoutingController(BasicFluidPipeTileEntity pipe, Direction direction,
                                              PlayerEntity player, Hand controllerHand,
                                              RoutingControllerMode controllerMode) {
        ITextComponent face = DirectionText.getDisplayName(direction);

        switch (controllerMode) {
            case FILTER_SAMPLE:
                applyFilterSample(pipe, direction, player, controllerHand, face, true);
                break;
            case FILTER_MODE:
                RoutingFilterMode filterMode = pipe.cycleSourceFilterMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_source_filter_mode",
                                face, filterMode.getDisplayName()),
                        true);
                break;
            case NBT_MATCH:
                boolean matchNbt = pipe.toggleSourceFilterNbt(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                matchNbt
                                        ? "message.justguithings.routing_controller.fluid_source_nbt_exact"
                                        : "message.justguithings.routing_controller.fluid_source_nbt_ignored",
                                face),
                        true);
                break;
            case REDSTONE:
                RoutingRedstoneMode redstoneMode = pipe.cycleSourceRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_source_redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
                break;
            case MIN_STOCK:
            default:
                int minStock = pipe.cycleSourceMinStock(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.fluid_min_stock",
                                face, minStock),
                        true);
                break;
        }
    }

    private void applyFilterSample(BasicFluidPipeTileEntity pipe, Direction direction,
                                   PlayerEntity player, Hand controllerHand,
                                   ITextComponent face, boolean source) {
        Hand sampleHand = controllerHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack sampleItem = player.getItemInHand(sampleHand);

        if (sampleItem.isEmpty()) {
            if (source) {
                pipe.clearSourceFilter(direction);
            } else {
                pipe.clearTargetFilter(direction);
            }
            player.displayClientMessage(
                    new TranslationTextComponent(
                            source
                                    ? "message.justguithings.routing_controller.fluid_source_filter_cleared"
                                    : "message.justguithings.routing_controller.fluid_filter_cleared",
                            face),
                    true);
            return;
        }

        FluidStack sample = FluidSampleResolver.resolve(sampleItem);
        if (sample.isEmpty()) {
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.fluid_sample_missing",
                            sampleItem.getHoverName()),
                    true);
            return;
        }

        RoutingFilterSampleChange change = source
                ? pipe.toggleSourceFilterSample(direction, sample)
                : pipe.toggleTargetFilterSample(direction, sample);
        int count = source
                ? pipe.getSourceFilterSampleCount(direction)
                : pipe.getTargetFilterSampleCount(direction);

        String prefix = source
                ? "message.justguithings.routing_controller.fluid_source_filter_"
                : "message.justguithings.routing_controller.fluid_filter_";

        switch (change) {
            case ADDED:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "added",
                                face, sample.getDisplayName(), count, FluidRouteFilter.MAX_SAMPLES),
                        true);
                break;
            case REMOVED:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "removed",
                                face, sample.getDisplayName(), count, FluidRouteFilter.MAX_SAMPLES),
                        true);
                break;
            case FULL:
            default:
                player.displayClientMessage(
                        new TranslationTextComponent(
                                prefix + "full",
                                face, FluidRouteFilter.MAX_SAMPLES),
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
        return new BasicFluidPipeTileEntity();
    }
}
