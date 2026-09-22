package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.AnalogSignalHelper;
import com.bulkcloud0.justguithings.world.container.EnergyCellContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class EnergyCellTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_TRANSFER = 2_000;

    private int lastAnalogSignal = -1;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.ENERGY,
            MachineSideMode.ENERGY_OUTPUT,
            MachineSideMode.ENERGY_BOTH
    };

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return energyStorage.getEnergyStored() & 0xFFFF;
                case 1:
                    return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0xFFFF0000) | (value & 0xFFFF));
                    break;
                case 1:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public EnergyCellTileEntity() {
        super(ModTileEntities.ENERGY_CELL.get(), CAPACITY, MAX_TRANSFER, MAX_TRANSFER,
                0, 0, 0, 0, 0);

        setSideMode(Direction.UP, MachineSideMode.ENERGY);
        setSideMode(Direction.DOWN, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ENERGY_BOTH);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY_BOTH);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY_BOTH);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY_BOTH);
    }

    public int getAnalogSignal() {
        return AnalogSignalHelper.fromFill(energyStorage.getEnergyStored(), CAPACITY);
    }

    @Override
    protected void onEnergyChanged() {
        notifyAnalogSignalIfChanged();
    }

    private void notifyAnalogSignalIfChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        int signal = getAnalogSignal();
        if (signal == lastAnalogSignal) {
            return;
        }

        lastAnalogSignal = signal;
        AnalogSignalHelper.notifyOutputChanged(this);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean supportsItemCapability() {
        return false;
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY || mode == MachineSideMode.ENERGY_BOTH;
    }

    @Override
    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY_OUTPUT || mode == MachineSideMode.ENERGY_BOTH;
    }

    @Override
    protected MachineSideMode normalizeLoadedSideMode(Direction side, MachineSideMode mode, int configVersion) {
        if (configVersion < 3) {
            switch (mode) {
                case INPUT:
                    return MachineSideMode.ENERGY;
                case OUTPUT:
                    return MachineSideMode.ENERGY_OUTPUT;
                case ENERGY:
                    return MachineSideMode.ENERGY_BOTH;
                default:
                    break;
            }
        }
        return super.normalizeLoadedSideMode(side, mode, configVersion);
    }

    @Override
    protected boolean canPushEnergyToNeighbor(Direction direction,
                                              MachineSideMode mode,
                                              TileEntity neighbor) {
        if (!(neighbor instanceof EnergyCellTileEntity)) {
            return true;
        }

        EnergyCellTileEntity other = (EnergyCellTileEntity) neighbor;
        MachineSideMode otherMode = other.getSideMode(direction.getOpposite());
        return mode != MachineSideMode.ENERGY_BOTH
                || otherMode != MachineSideMode.ENERGY_BOTH;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        pushEnergyToNeighborsFairly(MAX_TRANSFER);
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.energy_cell");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnergyCellContainer(windowId, playerInventory, this);
    }
}
