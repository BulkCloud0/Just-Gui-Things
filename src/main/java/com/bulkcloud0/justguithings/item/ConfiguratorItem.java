package com.bulkcloud0.justguithings.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class ConfiguratorItem extends TooltipItem {
    private static final String MODE_KEY = "ConfiguratorMode";

    public ConfiguratorItem(Properties properties) {
        super(properties,
                "tooltip.justguithings.configurator.use",
                "tooltip.justguithings.configurator.mode",
                "tooltip.justguithings.configurator.inspect",
                "tooltip.justguithings.configurator.overlay");
    }

    public static ConfiguratorMode getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return ConfiguratorMode.SIDE_IO;
        }
        return ConfiguratorMode.fromOrdinal(stack.getTag().getInt(MODE_KEY));
    }

    private static void setMode(ItemStack stack, ConfiguratorMode mode) {
        stack.getOrCreateTag().putInt(MODE_KEY, mode.ordinal());
    }

    public static boolean isMachineRedstoneMode(ItemStack stack) {
        return getMode(stack) == ConfiguratorMode.MACHINE_REDSTONE;
    }

    public static boolean isConduitConnectionMode(ItemStack stack) {
        return getMode(stack) == ConfiguratorMode.CONDUIT_CONNECTION;
    }

    public static void displayMachineTargetRequired(PlayerEntity player) {
        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.configurator.machine_target_required"),
                true);
    }

    public static void displayConduitTargetRequired(PlayerEntity player) {
        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.configurator.conduit_target_required"),
                true);
    }

    public static void displayConduitConnection(PlayerEntity player,
                                                ITextComponent face,
                                                boolean connected) {
        ITextComponent state = new TranslationTextComponent(
                connected
                        ? "connection.justguithings.connected"
                        : "connection.justguithings.disconnected");
        player.displayClientMessage(
                new TranslationTextComponent(
                        "message.justguithings.configurator.conduit_connection",
                        face, state),
                true);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            ConfiguratorMode mode = getMode(stack).next();
            setMode(stack, mode);
            player.displayClientMessage(
                    new TranslationTextComponent(
                            "message.justguithings.configurator.mode",
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
                "tooltip.justguithings.configurator.current_mode",
                getMode(stack).getDisplayName()).withStyle(TextFormatting.GRAY));
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

    public enum ConfiguratorMode {
        SIDE_IO,
        MACHINE_REDSTONE,
        CONDUIT_CONNECTION;

        public ConfiguratorMode next() {
            ConfiguratorMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public ITextComponent getDisplayName() {
            return new TranslationTextComponent(
                    "configurator_mode.justguithings." + name().toLowerCase(Locale.ROOT));
        }

        public static ConfiguratorMode fromOrdinal(int ordinal) {
            ConfiguratorMode[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return SIDE_IO;
            }
            return values[ordinal];
        }
    }
}
