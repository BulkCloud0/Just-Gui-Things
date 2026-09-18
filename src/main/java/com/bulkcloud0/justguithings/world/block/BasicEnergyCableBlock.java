package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.RoutingControllerItem;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
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
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nullable;
import java.util.Locale;

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

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicEnergyCableTileEntity
                && ((BasicEnergyCableTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = world.getBlockEntity(neighborPos);
        return neighbor != null
                && neighbor.getCapability(
                        CapabilityEnergy.ENERGY,
                        direction.getOpposite()).isPresent();
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
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        TileEntity tile = world.getBlockEntity(pos);
        ItemStack held = player.getItemInHand(hand);

        if (!(tile instanceof BasicEnergyCableTileEntity)) {
            return ActionResultType.PASS;
        }

        Direction direction = hit.getDirection();
        BasicEnergyCableTileEntity cable = (BasicEnergyCableTileEntity) tile;

        if (held.getItem() == ModItems.CONFIGURATOR.get()) {
            if (!world.isClientSide) {
                ConduitTransferMode mode = cable.cycleSideMode(direction);
                AbstractConduitBlock.refreshConnections(world, pos);

                String face = direction.toString().toUpperCase(Locale.ROOT);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.energy_cable.side_mode",
                                face, getEnergySideModeName(mode)),
                        true);
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (held.getItem() != ModItems.ROUTING_CONTROLLER.get()) {
            return ActionResultType.PASS;
        }

        if (!world.isClientSide) {
            RoutingControllerScope scope = RoutingControllerItem.getScope(held);
            RoutingControllerMode mode = RoutingControllerItem.getMode(held, scope);
            String face = direction.toString().toUpperCase(Locale.ROOT);

            if (!cable.getSideMode(direction).canPush()) {
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.energy_output_required",
                                face),
                        true);
            } else if (scope != RoutingControllerScope.TARGET) {
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.energy_target_only",
                                face),
                        true);
            } else if (mode == RoutingControllerMode.PRIORITY) {
                RoutingPriority priority = cable.cycleTargetPriority(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.energy_priority",
                                face, priority.getDisplayName()),
                        true);
            } else if (mode == RoutingControllerMode.REDSTONE) {
                RoutingRedstoneMode redstoneMode = cable.cycleTargetRedstoneMode(direction);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.energy_redstone",
                                face, redstoneMode.getDisplayName()),
                        true);
            } else {
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.energy_mode_unsupported",
                                mode.getDisplayName()),
                        true);
            }
        }

        return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    private ITextComponent getEnergySideModeName(ConduitTransferMode mode) {
        String key;
        switch (mode) {
            case PULL:
                key = "routing.justguithings.energy_side_mode.input";
                break;
            case PUSH:
                key = "routing.justguithings.energy_side_mode.output";
                break;
            case DISABLED:
                key = "routing.justguithings.energy_side_mode.disabled";
                break;
            case BOTH:
            default:
                key = "routing.justguithings.energy_side_mode.both";
                break;
        }
        return new TranslationTextComponent(key);
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
