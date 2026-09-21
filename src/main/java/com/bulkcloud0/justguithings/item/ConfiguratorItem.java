package com.bulkcloud0.justguithings.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;

public class ConfiguratorItem extends TooltipItem {
    public ConfiguratorItem(Properties properties) {
        super(properties,
                "tooltip.justguithings.configurator.use",
                "tooltip.justguithings.configurator.redstone",
                "tooltip.justguithings.configurator.inspect",
                "tooltip.justguithings.configurator.overlay");
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }

        TileEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!ConfiguratorTargetDescription.isSupported(tile)) {
            return ActionResultType.PASS;
        }

        if (!context.getLevel().isClientSide) {
            ITextComponent inspection = ConfiguratorTargetDescription.getDescription(
                    tile,
                    context.getLevel().getBlockState(context.getClickedPos()),
                    context.getClickedFace());
            if (inspection != null) {
                player.displayClientMessage(inspection, false);
            }
        }

        return context.getLevel().isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }
}
