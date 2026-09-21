package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.ConfiguratorItem;
import com.bulkcloud0.justguithings.item.RoutingControllerItem;
import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
import com.bulkcloud0.justguithings.logistics.RoutingPriority;
import com.bulkcloud0.justguithings.logistics.RoutingRedstoneMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.DirectionText;
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

public class BasicEnergyCableBlock extends AbstractConduitBlock {
    public BasicEnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canConnectTo(IBlockReader world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (!isPositionLoaded(world, neighborPos)) {
            return false;
        }

        BlockState neighborState = world.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof BasicEnergyCableBlock) {
            return isConduitLinkEnabled(world, pos, direction);
        }

        TileEntity self = world.getBlockEntity(pos);
        if (self instanceof BasicEnergyCableTileEntity
                && ((BasicEnergyCableTileEntity) self).getSideMode(direction) == ConduitTransferMode.DISABLED) {
            return false;
        }

        TileEntity neighbor = getLoadedBlockEntity(world, neighborPos);
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
            TileEntity neighbor = getLoadedBlockEntity(world, pos.relative(direction));
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
            if (player.isShiftKeyDown()) {
                return ActionResultType.PASS;
            }
            if (!world.isClientSide) {
                if (ConfiguratorItem.isConduitConnectionMode(held)) {
                    Boolean connected = cable.toggleConduitConnection(direction);
                    ITextComponent face = DirectionText.getDisplayName(direction);
                    if (connected == null) {
                        ConfiguratorItem.displayConduitTargetRequired(player);
                    } else {
                        ConfiguratorItem.displayConduitConnection(player, face, connected);
                    }
                } else if (ConfiguratorItem.isMachineRedstoneMode(held)) {
                    ConfiguratorItem.displayMachineTargetRequired(player);
                } else {
                    ConduitTransferMode mode = cable.cycleSideMode(direction);
                    AbstractConduitBlock.refreshConnections(world, pos);
    
                    ITextComponent face = DirectionText.getDisplayName(direction);
                    player.displayClientMessage(
                            new TranslationTextComponent(
                                    "message.justguithings.energy_cable.side_mode",
                                    face, mode.getEnergyDisplayName()),
                            true);
                }
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (held.getItem() != ModItems.ROUTING_CONTROLLER.get()) {
            return ActionResultType.PASS;
        }
        if (player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }

        if (!world.isClientSide) {
            RoutingControllerScope scope = RoutingControllerItem.getScope(held);
            RoutingControllerMode mode = RoutingControllerItem.getMode(held, scope);
            ITextComponent face = DirectionText.getDisplayName(direction);

            if (mode == RoutingControllerMode.COPY_RULE) {
                RoutingControllerItem.copyEnergyRoutingRule(held, scope, cable, direction);
                RoutingControllerItem.displayRuleCopied(player, scope, direction);
            } else if (mode == RoutingControllerMode.PASTE_RULE) {
                RoutingControllerItem.displayRulePasteResult(
                        player, scope, direction,
                        RoutingControllerItem.pasteEnergyRoutingRule(held, scope, cable, direction));
            } else if (scope == RoutingControllerScope.SOURCE) {
                applySourceRoutingController(cable, direction, player, mode, face);
            } else {
                applyTargetRoutingController(cable, direction, player, mode, face);
            }
        }

        return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    private void applyTargetRoutingController(BasicEnergyCableTileEntity cable,
                                              Direction direction,
                                              PlayerEntity player,
                                              RoutingControllerMode mode,
                                              ITextComponent face) {
        if (!cable.getSideMode(direction).canPush()) {
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.energy_output_required",
                            face),
                    true);
            return;
        }

        if (mode == RoutingControllerMode.PRIORITY) {
            RoutingPriority priority = cable.cycleTargetPriority(direction);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.energy_priority",
                            face, priority.getDisplayName()),
                    true);
            return;
        }

        if (mode == RoutingControllerMode.REDSTONE) {
            RoutingRedstoneMode redstoneMode = cable.cycleTargetRedstoneMode(direction);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.energy_redstone",
                            face, redstoneMode.getDisplayName()),
                    true);
            return;
        }

        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.routing_controller.energy_mode_unsupported",
                        mode.getDisplayName()),
                true);
    }

    private void applySourceRoutingController(BasicEnergyCableTileEntity cable,
                                              Direction direction,
                                              PlayerEntity player,
                                              RoutingControllerMode mode,
                                              ITextComponent face) {
        if (!cable.getSideMode(direction).canPull()) {
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.energy_input_required",
                            face),
                    true);
            return;
        }

        if (mode == RoutingControllerMode.REDSTONE) {
            RoutingRedstoneMode redstoneMode = cable.cycleSourceRedstoneMode(direction);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.energy_source_redstone",
                            face, redstoneMode.getDisplayName()),
                    true);
            return;
        }

        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.routing_controller.energy_source_mode_unsupported",
                        mode.getDisplayName()),
                true);
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
