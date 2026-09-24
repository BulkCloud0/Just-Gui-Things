package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.AnalogSignalHelper;
import com.bulkcloud0.justguithings.world.container.ItemBufferContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class ItemBufferTileEntity extends BaseMachineTileEntity {
    public static final int INVENTORY_SIZE = 18;
    public static final int AUTO_EJECT_RATE = 8;

    private int lastAnalogSignal = -1;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.OUTPUT
    };

    public ItemBufferTileEntity() {
        super(ModTileEntities.ITEM_BUFFER.get(), 0, 0,
                INVENTORY_SIZE, 0, INVENTORY_SIZE, 0, INVENTORY_SIZE);

        setSideMode(Direction.UP, MachineSideMode.INPUT);
        setSideMode(Direction.DOWN, MachineSideMode.OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.DISABLED);
        setSideMode(Direction.SOUTH, MachineSideMode.DISABLED);
        setSideMode(Direction.WEST, MachineSideMode.DISABLED);
        setSideMode(Direction.EAST, MachineSideMode.DISABLED);
    }

    public int getAnalogSignal() {
        return AnalogSignalHelper.fromInventory(inventory);
    }

    @Override
    protected void onInventoryChanged(int slot) {
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
        return slot >= 0 && slot < INVENTORY_SIZE && !stack.isEmpty();
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
    }

    @Override
    public boolean supportsItemAutoEject() {
        return true;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide || !isOperationEnabled()) {
            return;
        }
        pushOutputItemsToNeighborsFairly(AUTO_EJECT_RATE);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.item_buffer");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemBufferContainer(windowId, playerInventory, this);
    }
}
