package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Locale;

public class ConfiguratorItem extends TooltipItem {
    public ConfiguratorItem(Properties properties) {
        super(properties,
                "tooltip.justguithings.configurator.use",
                "tooltip.justguithings.configurator.inspect");
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }

        TileEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(tile instanceof BasicItemPipeTileEntity)
                && !(tile instanceof BasicFluidPipeTileEntity)
                && !(tile instanceof BasicEnergyCableTileEntity)
                && !(tile instanceof BaseMachineTileEntity)) {
            return ActionResultType.PASS;
        }

        if (!context.getLevel().isClientSide) {
            ITextComponent inspection = getInspectionText(tile, context.getClickedFace());
            if (inspection != null) {
                player.displayClientMessage(inspection, false);
            }
        }

        return context.getLevel().isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    @Nullable
    private ITextComponent getInspectionText(TileEntity tile, Direction direction) {
        String face = direction.toString().toUpperCase(Locale.ROOT);

        if (tile instanceof BasicItemPipeTileEntity) {
            ConduitTransferMode mode = ((BasicItemPipeTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_item_pipe",
                    face, mode.getDisplayName());
        }

        if (tile instanceof BasicFluidPipeTileEntity) {
            ConduitTransferMode mode = ((BasicFluidPipeTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_fluid_pipe",
                    face, mode.getDisplayName());
        }

        if (tile instanceof BasicEnergyCableTileEntity) {
            ConduitTransferMode mode = ((BasicEnergyCableTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_energy_cable",
                    face, mode.getEnergyDisplayName());
        }

        if (tile instanceof BaseMachineTileEntity) {
            BaseMachineTileEntity machine = (BaseMachineTileEntity) tile;
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_machine",
                    face, machine.getSideMode(direction).getDisplayName());
        }

        return null;
    }

}
