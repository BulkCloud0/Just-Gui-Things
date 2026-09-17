package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.machine.MachineInventoryDropHelper;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.world.tile.QuenchChamberTileEntity;
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
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Locale;

public class QuenchChamberBlock extends Block {
    public QuenchChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new QuenchChamberTileEntity();
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof QuenchChamberTileEntity) {
                MachineInventoryDropHelper.dropContents(world, pos, ((QuenchChamberTileEntity) tile).getInventory());
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        TileEntity tile = world.getBlockEntity(pos);
        ItemStack held = player.getItemInHand(hand);

        if (tile instanceof QuenchChamberTileEntity
                && FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getDirection())) {
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (tile instanceof QuenchChamberTileEntity && held.getItem() == ModItems.CONFIGURATOR.get()) {
            if (!world.isClientSide) {
                QuenchChamberTileEntity chamber = (QuenchChamberTileEntity) tile;
                MachineSideMode mode = chamber.cycleSideMode(hit.getDirection());
                String face = hit.getDirection().toString().toUpperCase(Locale.ROOT);
                player.displayClientMessage(new StringTextComponent(face + ": " + mode.name()), true);
            }
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }

        if (!world.isClientSide) {
            if (tile instanceof QuenchChamberTileEntity && player instanceof ServerPlayerEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (QuenchChamberTileEntity) tile, pos);
            }
            return ActionResultType.CONSUME;
        }
        return ActionResultType.SUCCESS;
    }
}
