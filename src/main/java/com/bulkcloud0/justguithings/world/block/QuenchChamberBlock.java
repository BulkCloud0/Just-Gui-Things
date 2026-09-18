package com.bulkcloud0.justguithings.world.block;

import com.bulkcloud0.justguithings.world.tile.QuenchChamberTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class QuenchChamberBlock extends BaseMachineBlock<QuenchChamberTileEntity> {
    public QuenchChamberBlock(Properties properties) {
        super(properties, QuenchChamberTileEntity.class, QuenchChamberTileEntity::new);
    }

    @Nullable
    @Override
    protected ActionResultType handleSpecialUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                                Hand hand, BlockRayTraceResult hit, QuenchChamberTileEntity tile) {
        if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getDirection())) {
            return world.isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        }
        return null;
    }
}
