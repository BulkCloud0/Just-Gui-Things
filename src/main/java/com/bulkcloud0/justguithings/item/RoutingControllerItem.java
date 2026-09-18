package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.logistics.EnergyRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.EnergyRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.FluidRouteFilter;
import com.bulkcloud0.justguithings.logistics.FluidRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.FluidRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.ItemRouteFilter;
import com.bulkcloud0.justguithings.logistics.ItemRoutingSourceRule;
import com.bulkcloud0.justguithings.logistics.ItemRoutingTargetRule;
import com.bulkcloud0.justguithings.logistics.RoutingControllerMode;
import com.bulkcloud0.justguithings.logistics.RoutingControllerScope;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
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
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }

        TileEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(tile instanceof BasicItemPipeTileEntity)
                && !(tile instanceof BasicFluidPipeTileEntity)
                && !(tile instanceof BasicEnergyCableTileEntity)) {
            return ActionResultType.PASS;
        }

        if (!context.getLevel().isClientSide) {
            RoutingControllerScope scope = getScope(context.getItemInHand());
            Direction direction = context.getClickedFace();
            ITextComponent inspection = getInspectionText(tile, direction, scope);
            if (inspection != null) {
                player.displayClientMessage(inspection, false);
            }
        }

        return context.getLevel().isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    @Nullable
    private ITextComponent getInspectionText(TileEntity tile, Direction direction,
                                             RoutingControllerScope scope) {
        if (tile instanceof BasicItemPipeTileEntity) {
            return getItemInspection((BasicItemPipeTileEntity) tile, direction, scope);
        }
        if (tile instanceof BasicFluidPipeTileEntity) {
            return getFluidInspection((BasicFluidPipeTileEntity) tile, direction, scope);
        }
        if (tile instanceof BasicEnergyCableTileEntity) {
            return getEnergyInspection((BasicEnergyCableTileEntity) tile, direction, scope);
        }
        return null;
    }

    private ITextComponent getItemInspection(BasicItemPipeTileEntity pipe, Direction direction,
                                             RoutingControllerScope scope) {
        String face = direction.toString().toUpperCase(java.util.Locale.ROOT);

        if (scope == RoutingControllerScope.SOURCE) {
            ItemRoutingSourceRule rule = pipe.getSourceRule(direction);
            ItemRouteFilter filter = rule.getFilter();
            return new TranslationTextComponent(
                    "message.justguithings.routing_controller.inspect_item_source",
                    face, pipe.getSideMode(direction).getDisplayName(),
                    filter.getMode().getDisplayName(),
                    filter.getSampleCount(), ItemRouteFilter.MAX_SAMPLES,
                    getNbtModeName(filter.isMatchNbt()),
                    rule.getRedstoneMode().getDisplayName(),
                    rule.getMinStock());
        }

        ItemRoutingTargetRule rule = pipe.getTargetRule(direction);
        ItemRouteFilter filter = rule.getFilter();
        return new TranslationTextComponent(
                "message.justguithings.routing_controller.inspect_item_target",
                face, pipe.getSideMode(direction).getDisplayName(),
                rule.getPriority().getDisplayName(),
                filter.getMode().getDisplayName(),
                filter.getSampleCount(), ItemRouteFilter.MAX_SAMPLES,
                getNbtModeName(filter.isMatchNbt()),
                rule.getRedstoneMode().getDisplayName());
    }

    private ITextComponent getFluidInspection(BasicFluidPipeTileEntity pipe, Direction direction,
                                              RoutingControllerScope scope) {
        String face = direction.toString().toUpperCase(java.util.Locale.ROOT);

        if (scope == RoutingControllerScope.SOURCE) {
            FluidRoutingSourceRule rule = pipe.getSourceRule(direction);
            FluidRouteFilter filter = rule.getFilter();
            return new TranslationTextComponent(
                    "message.justguithings.routing_controller.inspect_fluid_source",
                    face, pipe.getSideMode(direction).getDisplayName(),
                    filter.getMode().getDisplayName(),
                    filter.getSampleCount(), FluidRouteFilter.MAX_SAMPLES,
                    getNbtModeName(filter.isMatchNbt()),
                    rule.getRedstoneMode().getDisplayName(),
                    rule.getMinStock());
        }

        FluidRoutingTargetRule rule = pipe.getTargetRule(direction);
        FluidRouteFilter filter = rule.getFilter();
        return new TranslationTextComponent(
                "message.justguithings.routing_controller.inspect_fluid_target",
                face, pipe.getSideMode(direction).getDisplayName(),
                rule.getPriority().getDisplayName(),
                filter.getMode().getDisplayName(),
                filter.getSampleCount(), FluidRouteFilter.MAX_SAMPLES,
                getNbtModeName(filter.isMatchNbt()),
                rule.getRedstoneMode().getDisplayName());
    }

    private ITextComponent getEnergyInspection(BasicEnergyCableTileEntity cable, Direction direction,
                                               RoutingControllerScope scope) {
        String face = direction.toString().toUpperCase(java.util.Locale.ROOT);
        ITextComponent sideMode = getEnergySideModeName(cable.getSideMode(direction));

        if (scope == RoutingControllerScope.SOURCE) {
            EnergyRoutingSourceRule rule = cable.getSourceRule(direction);
            return new TranslationTextComponent(
                    "message.justguithings.routing_controller.inspect_energy_source",
                    face, sideMode, rule.getRedstoneMode().getDisplayName());
        }

        EnergyRoutingTargetRule rule = cable.getTargetRule(direction);
        return new TranslationTextComponent(
                "message.justguithings.routing_controller.inspect_energy_target",
                face, sideMode,
                rule.getPriority().getDisplayName(),
                rule.getRedstoneMode().getDisplayName());
    }

    private ITextComponent getNbtModeName(boolean matchNbt) {
        return new TranslationTextComponent(
                matchNbt
                        ? "routing.justguithings.nbt_mode.exact"
                        : "routing.justguithings.nbt_mode.ignored");
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
