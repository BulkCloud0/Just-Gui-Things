package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class RoutingControllerItem extends TooltipItem {
    private static final String LEGACY_MODE_KEY = "RoutingMode";
    private static final String SCOPE_KEY = "RoutingScope";
    private static final String TARGET_MODE_KEY = "TargetRoutingMode";
    private static final String SOURCE_MODE_KEY = "SourceRoutingMode";

    public RoutingControllerItem(Properties properties) {
        super(properties,
                "tooltip.justguithings.routing_controller.scope",
                "tooltip.justguithings.routing_controller.mode",
                "tooltip.justguithings.routing_controller.use");
    }

    public static RoutingControllerScope getScope(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return RoutingControllerScope.TARGET;
        }
        return RoutingControllerScope.fromOrdinal(stack.getTag().getInt(SCOPE_KEY));
    }

    public static void setScope(ItemStack stack, RoutingControllerScope scope) {
        stack.getOrCreateTag().putInt(SCOPE_KEY, scope.ordinal());
    }

    public static RoutingControllerMode getMode(ItemStack stack) {
        return getMode(stack, getScope(stack));
    }

    public static RoutingControllerMode getMode(ItemStack stack, RoutingControllerScope scope) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return scope.getDefaultMode();
        }

        CompoundNBT tag = stack.getTag();
        String key = scope == RoutingControllerScope.TARGET ? TARGET_MODE_KEY : SOURCE_MODE_KEY;

        RoutingControllerMode mode;
        if (tag.contains(key)) {
            mode = RoutingControllerMode.fromOrdinal(tag.getInt(key));
        } else if (scope == RoutingControllerScope.TARGET && tag.contains(LEGACY_MODE_KEY)) {
            mode = RoutingControllerMode.fromOrdinal(tag.getInt(LEGACY_MODE_KEY));
        } else {
            mode = scope.getDefaultMode();
        }

        return scope.normalize(mode);
    }

    public static void setMode(ItemStack stack, RoutingControllerScope scope, RoutingControllerMode mode) {
        String key = scope == RoutingControllerScope.TARGET ? TARGET_MODE_KEY : SOURCE_MODE_KEY;
        stack.getOrCreateTag().putInt(key, scope.normalize(mode).ordinal());
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            RoutingControllerScope scope = getScope(stack);

            if (player.isShiftKeyDown()) {
                scope = scope.next();
                setScope(stack, scope);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.scope",
                                scope.getDisplayName(), getMode(stack, scope).getDisplayName()),
                        true);
            } else {
                RoutingControllerMode mode = scope.nextMode(getMode(stack, scope));
                setMode(stack, scope, mode);
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.routing_controller.mode",
                                scope.getDisplayName(), mode.getDisplayName()),
                        true);
            }
        }

        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        RoutingControllerScope scope = getScope(stack);
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.routing_controller.current_scope",
                scope.getDisplayName()).withStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.routing_controller.current_mode",
                getMode(stack, scope).getDisplayName()).withStyle(TextFormatting.AQUA));
    }
}
