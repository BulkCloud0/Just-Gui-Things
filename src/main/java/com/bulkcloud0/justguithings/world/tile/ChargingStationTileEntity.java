package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.ChargingStationContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class ChargingStationTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 500;
    public static final int CHARGE_RATE = 500;

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

    public ChargingStationTileEntity() {
        super(ModTileEntities.CHARGING_STATION.get(), CAPACITY, MAX_RECEIVE,
                2, 0, 1, 1, 1);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canCharge(stack);
    }

    @Override
    protected int getMachineSlotLimit(int slot) {
        return 1;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }

        IEnergyStorage target = input.getCapability(CapabilityEnergy.ENERGY).orElse(null);
        if (target == null) {
            moveInputToOutput();
            return;
        }

        boolean changed = false;
        int available = Math.min(CHARGE_RATE, energyStorage.getEnergyStored());
        if (available > 0 && target.canReceive()) {
            int accepted = Math.max(0, target.receiveEnergy(available, false));
            if (accepted > 0) {
                energyStorage.consumeEnergy(Math.min(accepted, available));
                changed = true;
            }
        }

        if ((!target.canReceive() || target.receiveEnergy(1, true) <= 0) && moveInputToOutput()) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean moveInputToOutput() {
        if (!inventory.getStackInSlot(1).isEmpty()) {
            return false;
        }

        ItemStack moved = inventory.extractItem(0, 1, false);
        if (moved.isEmpty()) {
            return false;
        }
        inventory.setStackInSlot(1, moved);
        return true;
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY).orElse(null);
        return storage != null && storage.canReceive();
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.charging_station");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ChargingStationContainer(windowId, playerInventory, this);
    }
}
