package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.item.ConfiguratorItem;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineInventoryDropHelper;
import com.bulkcloud0.justguithings.machine.MachineRedstoneMode;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.DirectionText;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class BaseMachineBlock<T extends BaseMachineTileEntity> extends Block {
    private final Class<T> tileClass;
    private final Supplier<T> tileFactory;

    protected BaseMachineBlock(Properties properties, Class<T> tileClass, Supplier<T> tileFactory) {
        super(properties);
        this.tileClass = tileClass;
        this.tileFactory = tileFactory;
    }

    @Override
    public final boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public final TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return tileFactory.get();
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            T tile = getMachine(world, pos);
            if (tile != null) {
                MachineInventoryDropHelper.dropContents(world, pos, tile.getInventory());
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public final ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                      Hand hand, BlockRayTraceResult hit) {
        T tile = getMachine(world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }

        ActionResultType specialResult = handleSpecialUse(state, world, pos, player, hand, hit, tile);
        if (specialResult != null) {
            return specialResult;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.CONFIGURATOR.get()) {
            if (player.isShiftKeyDown()) {
                return ActionResultType.PASS;
            }
            if (!world.isClientSide) {
                if (ConfiguratorItem.isMachineRedstoneMode(held)) {
                    if (tile.supportsRedstoneControl()) {
                        MachineRedstoneMode mode = tile.cycleRedstoneMode();
                        player.displayClientMessage(
                                new TranslationTextComponent(
                                        "message.justguithings.machine.redstone_mode",
                                        mode.getDisplayName()),
                                true);
                    } else {
                        ConfiguratorItem.displayMachineTargetRequired(player);
                    }
                } else {
                    MachineSideMode mode = tile.cycleSideMode(hit.getDirection());
                    ITextComponent face = DirectionText.getDisplayName(hit.getDirection());
                    player.displayClientMessage(
                            new TranslationTextComponent(
                                    "message.justguithings.machine.side_mode",
                                    face, mode.getDisplayName()),
                            true);
                }
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (!world.isClientSide) {
            if (player instanceof ServerPlayerEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player, tile, pos);
            }
            return ActionResultType.CONSUME;
        }

        return ActionResultType.SUCCESS;
    }

    @Nullable
    protected ActionResultType handleSpecialUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                                Hand hand, BlockRayTraceResult hit, T tile) {
        return null;
    }

    @Nullable
    protected final T getMachine(IBlockReader world, BlockPos pos) {
        TileEntity tile = world.getBlockEntity(pos);
        return tileClass.isInstance(tile) ? tileClass.cast(tile) : null;
    }
}
