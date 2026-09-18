package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class RoutingControllerItem extends TooltipItem {
    private static final String MODE_KEY = "RoutingMode";

    public RoutingControllerItem(Properties properties) {
        super(properties,
                "tooltip.justguithings.routing_controller.mode",
                "tooltip.justguithings.routing_controller.use");
    }

    public static RoutingControllerMode getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return RoutingControllerMode.PRIORITY;
        }
        return RoutingControllerMode.fromOrdinal(stack.getTag().getInt(MODE_KEY));
    }

    public static void setMode(ItemStack stack, RoutingControllerMode mode) {
        stack.getOrCreateTag().putInt(MODE_KEY, mode.ordinal());
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            RoutingControllerMode mode = getMode(stack).next();
            setMode(stack, mode);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.routing_controller.mode",
                            mode.getDisplayName()),
                    true);
        }

        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.routing_controller.current_mode",
                getMode(stack).getDisplayName()).withStyle(TextFormatting.AQUA));
    }
}
