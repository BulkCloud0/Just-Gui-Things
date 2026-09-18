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
import com.bulkcloud0.justguithings.world.DirectionText;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.StringJoiner;

public class RoutingControllerItem extends TooltipItem {
    private static final String LEGACY_MODE_KEY = "RoutingMode";
    private static final String SCOPE_KEY = "RoutingScope";
    private static final String TARGET_MODE_KEY = "TargetRoutingMode";
    private static final String SOURCE_MODE_KEY = "SourceRoutingMode";
    private static final String TARGET_CLIPBOARD_KEY = "TargetRuleClipboard";
    private static final String SOURCE_CLIPBOARD_KEY = "SourceRuleClipboard";
    private static final String CLIPBOARD_RESOURCE_KEY = "Resource";
    private static final String CLIPBOARD_RULE_KEY = "Rule";
    private static final String RESOURCE_ITEM = "item";
    private static final String RESOURCE_FLUID = "fluid";
    private static final String RESOURCE_ENERGY = "energy";

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

    public enum ClipboardPasteResult {
        SUCCESS,
        EMPTY,
        RESOURCE_MISMATCH
    }

    public static void copyItemRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                           BasicItemPipeTileEntity pipe, Direction direction) {
        CompoundNBT rule = scope == RoutingControllerScope.SOURCE
                ? pipe.getSourceRule(direction).save()
                : pipe.getTargetRule(direction).save();
        storeClipboard(controller, scope, RESOURCE_ITEM, rule);
    }

    public static ClipboardPasteResult pasteItemRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                                            BasicItemPipeTileEntity pipe, Direction direction) {
        CompoundNBT clipboard = getClipboard(controller, scope);
        ClipboardPasteResult validation = validateClipboard(clipboard, RESOURCE_ITEM);
        if (validation != ClipboardPasteResult.SUCCESS) {
            return validation;
        }

        CompoundNBT rule = clipboard.getCompound(CLIPBOARD_RULE_KEY);
        if (scope == RoutingControllerScope.SOURCE) {
            pipe.setSourceRule(direction, ItemRoutingSourceRule.load(rule));
        } else {
            pipe.setTargetRule(direction, ItemRoutingTargetRule.load(rule));
        }
        return ClipboardPasteResult.SUCCESS;
    }

    public static void copyFluidRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                            BasicFluidPipeTileEntity pipe, Direction direction) {
        CompoundNBT rule = scope == RoutingControllerScope.SOURCE
                ? pipe.getSourceRule(direction).save()
                : pipe.getTargetRule(direction).save();
        storeClipboard(controller, scope, RESOURCE_FLUID, rule);
    }

    public static ClipboardPasteResult pasteFluidRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                                             BasicFluidPipeTileEntity pipe, Direction direction) {
        CompoundNBT clipboard = getClipboard(controller, scope);
        ClipboardPasteResult validation = validateClipboard(clipboard, RESOURCE_FLUID);
        if (validation != ClipboardPasteResult.SUCCESS) {
            return validation;
        }

        CompoundNBT rule = clipboard.getCompound(CLIPBOARD_RULE_KEY);
        if (scope == RoutingControllerScope.SOURCE) {
            pipe.setSourceRule(direction, FluidRoutingSourceRule.load(rule));
        } else {
            pipe.setTargetRule(direction, FluidRoutingTargetRule.load(rule));
        }
        return ClipboardPasteResult.SUCCESS;
    }

    public static void copyEnergyRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                             BasicEnergyCableTileEntity cable, Direction direction) {
        CompoundNBT rule = scope == RoutingControllerScope.SOURCE
                ? cable.getSourceRule(direction).save()
                : cable.getTargetRule(direction).save();
        storeClipboard(controller, scope, RESOURCE_ENERGY, rule);
    }

    public static ClipboardPasteResult pasteEnergyRoutingRule(ItemStack controller, RoutingControllerScope scope,
                                                              BasicEnergyCableTileEntity cable, Direction direction) {
        CompoundNBT clipboard = getClipboard(controller, scope);
        ClipboardPasteResult validation = validateClipboard(clipboard, RESOURCE_ENERGY);
        if (validation != ClipboardPasteResult.SUCCESS) {
            return validation;
        }

        CompoundNBT rule = clipboard.getCompound(CLIPBOARD_RULE_KEY);
        if (scope == RoutingControllerScope.SOURCE) {
            cable.setSourceRule(direction, EnergyRoutingSourceRule.load(rule));
        } else {
            cable.setTargetRule(direction, EnergyRoutingTargetRule.load(rule));
        }
        return ClipboardPasteResult.SUCCESS;
    }

    private static void storeClipboard(ItemStack controller, RoutingControllerScope scope,
                                       String resource, CompoundNBT rule) {
        CompoundNBT clipboard = new CompoundNBT();
        clipboard.putString(CLIPBOARD_RESOURCE_KEY, resource);
        clipboard.put(CLIPBOARD_RULE_KEY, rule.copy());
        controller.getOrCreateTag().put(getClipboardKey(scope), clipboard);
    }

    @Nullable
    private static CompoundNBT getClipboard(ItemStack controller, RoutingControllerScope scope) {
        if (controller.isEmpty() || !controller.hasTag()) {
            return null;
        }

        String key = getClipboardKey(scope);
        CompoundNBT tag = controller.getTag();
        if (tag == null || !tag.contains(key, 10)) {
            return null;
        }
        return tag.getCompound(key);
    }

    private static ClipboardPasteResult validateClipboard(@Nullable CompoundNBT clipboard, String resource) {
        if (clipboard == null || !clipboard.contains(CLIPBOARD_RULE_KEY, 10)) {
            return ClipboardPasteResult.EMPTY;
        }
        if (!resource.equals(clipboard.getString(CLIPBOARD_RESOURCE_KEY))) {
            return ClipboardPasteResult.RESOURCE_MISMATCH;
        }
        return ClipboardPasteResult.SUCCESS;
    }

    private static String getClipboardKey(RoutingControllerScope scope) {
        return scope == RoutingControllerScope.SOURCE ? SOURCE_CLIPBOARD_KEY : TARGET_CLIPBOARD_KEY;
    }

    public static void displayRuleCopied(PlayerEntity player, RoutingControllerScope scope,
                                         Direction direction) {
        ITextComponent face = DirectionText.getDisplayName(direction);
        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.routing_controller.rule_copied",
                        scope.getDisplayName(), face),
                true);
    }

    public static void displayRulePasteResult(PlayerEntity player, RoutingControllerScope scope,
                                              Direction direction, ClipboardPasteResult result) {
        ITextComponent face = DirectionText.getDisplayName(direction);
        String key;
        switch (result) {
            case SUCCESS:
                key = "message.justguithings.routing_controller.rule_pasted";
                break;
            case RESOURCE_MISMATCH:
                key = "message.justguithings.routing_controller.rule_clipboard_resource_mismatch";
                break;
            case EMPTY:
            default:
                key = "message.justguithings.routing_controller.rule_clipboard_empty";
                break;
        }

        player.displayClientMessage(
                new TranslationTextComponent(key, scope.getDisplayName(), face),
                true);
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
            boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
            ITextComponent inspection = getInspectionText(tile, direction, scope, powered);
            if (inspection != null) {
                player.displayClientMessage(inspection, false);
            }
        }

        return context.getLevel().isClientSide ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    @Nullable
    private ITextComponent getInspectionText(TileEntity tile, Direction direction,
                                             RoutingControllerScope scope, boolean powered) {
        if (tile instanceof BasicItemPipeTileEntity) {
            return getItemInspection((BasicItemPipeTileEntity) tile, direction, scope, powered);
        }
        if (tile instanceof BasicFluidPipeTileEntity) {
            return getFluidInspection((BasicFluidPipeTileEntity) tile, direction, scope, powered);
        }
        if (tile instanceof BasicEnergyCableTileEntity) {
            return getEnergyInspection((BasicEnergyCableTileEntity) tile, direction, scope, powered);
        }
        return null;
    }

    private ITextComponent getItemInspection(BasicItemPipeTileEntity pipe, Direction direction,
                                             RoutingControllerScope scope, boolean powered) {
        ITextComponent face = DirectionText.getDisplayName(direction);

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
                    rule.getMinStock(),
                    getItemSampleIds(filter),
                    getEndpointStateName(pipe.getSideMode(direction).canPull()
                            && rule.allowsRedstone(powered)));
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
                rule.getRedstoneMode().getDisplayName(),
                getItemSampleIds(filter),
                getEndpointStateName(pipe.getSideMode(direction).canPush()
                        && rule.allowsRedstone(powered)));
    }

    private ITextComponent getFluidInspection(BasicFluidPipeTileEntity pipe, Direction direction,
                                              RoutingControllerScope scope, boolean powered) {
        ITextComponent face = DirectionText.getDisplayName(direction);

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
                    rule.getMinStock(),
                    getFluidSampleIds(filter),
                    getEndpointStateName(pipe.getSideMode(direction).canPull()
                            && rule.allowsRedstone(powered)));
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
                rule.getRedstoneMode().getDisplayName(),
                getFluidSampleIds(filter),
                getEndpointStateName(pipe.getSideMode(direction).canPush()
                        && rule.allowsRedstone(powered)));
    }

    private ITextComponent getEnergyInspection(BasicEnergyCableTileEntity cable, Direction direction,
                                               RoutingControllerScope scope, boolean powered) {
        ITextComponent face = DirectionText.getDisplayName(direction);
        ITextComponent sideMode = cable.getSideMode(direction).getEnergyDisplayName();

        if (scope == RoutingControllerScope.SOURCE) {
            EnergyRoutingSourceRule rule = cable.getSourceRule(direction);
            return new TranslationTextComponent(
                    "message.justguithings.routing_controller.inspect_energy_source",
                    face, sideMode, rule.getRedstoneMode().getDisplayName(),
                    getEndpointStateName(cable.getSideMode(direction).canPull()
                            && rule.allowsRedstone(powered)));
        }

        EnergyRoutingTargetRule rule = cable.getTargetRule(direction);
        return new TranslationTextComponent(
                "message.justguithings.routing_controller.inspect_energy_target",
                face, sideMode,
                rule.getPriority().getDisplayName(),
                rule.getRedstoneMode().getDisplayName(),
                getEndpointStateName(cable.getSideMode(direction).canPush()
                        && rule.allowsRedstone(powered)));
    }

    private String getItemSampleIds(ItemRouteFilter filter) {
        StringJoiner ids = new StringJoiner(", ");
        for (ItemStack sample : filter.getSamples()) {
            String id = Registry.ITEM.getKey(sample.getItem()).toString();
            ids.add(sample.hasTag() ? id + "[NBT]" : id);
        }
        String value = ids.toString();
        return value.isEmpty() ? "-" : value;
    }

    private String getFluidSampleIds(FluidRouteFilter filter) {
        StringJoiner ids = new StringJoiner(", ");
        for (net.minecraftforge.fluids.FluidStack sample : filter.getSamples()) {
            String id = Registry.FLUID.getKey(sample.getFluid()).toString();
            ids.add(sample.getTag() != null ? id + "[NBT]" : id);
        }
        String value = ids.toString();
        return value.isEmpty() ? "-" : value;
    }

    private ITextComponent getEndpointStateName(boolean active) {
        return new TranslationTextComponent(
                active
                        ? "routing.justguithings.endpoint_state.active"
                        : "routing.justguithings.endpoint_state.inactive");
    }

    private ITextComponent getNbtModeName(boolean matchNbt) {
        return new TranslationTextComponent(
                matchNbt
                        ? "routing.justguithings.nbt_mode.exact"
                        : "routing.justguithings.nbt_mode.ignored");
    }

    private ITextComponent getClipboardResourceName(ItemStack stack, RoutingControllerScope scope) {
        CompoundNBT clipboard = getClipboard(stack, scope);
        if (clipboard == null || !clipboard.contains(CLIPBOARD_RULE_KEY, 10)) {
            return new TranslationTextComponent("routing.justguithings.clipboard_resource.empty");
        }

        String resource = clipboard.getString(CLIPBOARD_RESOURCE_KEY);
        String key;
        if (RESOURCE_ITEM.equals(resource)) {
            key = "routing.justguithings.clipboard_resource.item";
        } else if (RESOURCE_FLUID.equals(resource)) {
            key = "routing.justguithings.clipboard_resource.fluid";
        } else if (RESOURCE_ENERGY.equals(resource)) {
            key = "routing.justguithings.clipboard_resource.energy";
        } else {
            key = "routing.justguithings.clipboard_resource.unknown";
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
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.routing_controller.clipboard",
                RoutingControllerScope.TARGET.getDisplayName(),
                getClipboardResourceName(stack, RoutingControllerScope.TARGET))
                .withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent(
                "tooltip.justguithings.routing_controller.clipboard",
                RoutingControllerScope.SOURCE.getDisplayName(),
                getClipboardResourceName(stack, RoutingControllerScope.SOURCE))
                .withStyle(TextFormatting.GRAY));
    }
}
