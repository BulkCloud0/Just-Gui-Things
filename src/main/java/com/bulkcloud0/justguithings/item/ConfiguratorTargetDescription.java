package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.logistics.ConduitTransferMode;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.world.DirectionText;
import com.bulkcloud0.justguithings.world.block.AbstractConduitBlock;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public final class ConfiguratorTargetDescription {
    private ConfiguratorTargetDescription() {
    }

    public static boolean isSupported(@Nullable TileEntity tile) {
        return tile instanceof BasicItemPipeTileEntity
                || tile instanceof BasicFluidPipeTileEntity
                || tile instanceof BasicEnergyCableTileEntity
                || tile instanceof BaseMachineTileEntity;
    }

    @Nullable
    public static ITextComponent getDescription(@Nullable TileEntity tile,
                                                BlockState state,
                                                Direction direction) {
        if (!isSupported(tile)) {
            return null;
        }

        ITextComponent face = DirectionText.getDisplayName(direction);
        ITextComponent connection = getConnectionStateName(
                AbstractConduitBlock.isConnected(state, direction));

        if (tile instanceof BasicItemPipeTileEntity) {
            ConduitTransferMode mode = ((BasicItemPipeTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_item_pipe",
                    face, mode.getDisplayName(), connection);
        }

        if (tile instanceof BasicFluidPipeTileEntity) {
            ConduitTransferMode mode = ((BasicFluidPipeTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_fluid_pipe",
                    face, mode.getDisplayName(), connection);
        }

        if (tile instanceof BasicEnergyCableTileEntity) {
            ConduitTransferMode mode = ((BasicEnergyCableTileEntity) tile).getSideMode(direction);
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_energy_cable",
                    face, mode.getEnergyDisplayName(), connection);
        }

        BaseMachineTileEntity machine = (BaseMachineTileEntity) tile;
        if (machine.supportsRedstoneControl()) {
            return new TranslationTextComponent(
                    "message.justguithings.configurator.inspect_machine_redstone",
                    face,
                    machine.getSideMode(direction).getDisplayName(),
                    machine.getRedstoneMode().getDisplayName());
        }
        return new TranslationTextComponent(
                "message.justguithings.configurator.inspect_machine",
                face, machine.getSideMode(direction).getDisplayName());
    }

    private static ITextComponent getConnectionStateName(boolean connected) {
        return new TranslationTextComponent(
                connected
                        ? "connection.justguithings.connected"
                        : "connection.justguithings.disconnected");
    }
}
