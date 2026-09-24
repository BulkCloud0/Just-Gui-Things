package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.IndustrialWasherTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

public class IndustrialWasherContainer extends Container {
    private static final int MACHINE_SLOT_COUNT = IndustrialWasherTileEntity.INVENTORY_SIZE;
    private static final int PLAYER_MAIN_END = MACHINE_SLOT_COUNT + 27;
    private static final int PLAYER_END = PLAYER_MAIN_END + 9;

    private final IndustrialWasherTileEntity tileEntity;
    private final IIntArray data;
    private final SideConfigContainerData sideData;

    public IndustrialWasherContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public IndustrialWasherContainer(int windowId, PlayerInventory playerInventory,
                                     IndustrialWasherTileEntity tileEntity) {
        super(ModContainers.INDUSTRIAL_WASHER.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();
        this.sideData = new SideConfigContainerData(tileEntity, tileEntity::getSideMode);

        addSlot(new SlotItemHandler(tileEntity.getInventory(),
                IndustrialWasherTileEntity.INPUT_SLOT, 44, 35));
        addSlot(new SlotItemHandler(tileEntity.getInventory(),
                IndustrialWasherTileEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(tileEntity.getInventory(),
                IndustrialWasherTileEntity.SPEED_MODULE_SLOT, 72, 56));
        addSlot(new SlotItemHandler(tileEntity.getInventory(),
                IndustrialWasherTileEntity.EFFICIENCY_MODULE_SLOT, 90, 56));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
        addDataSlots(sideData);
    }

    private static IndustrialWasherTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof IndustrialWasherTileEntity)) {
            throw new IllegalStateException("Industrial Washer tile entity not found at " + pos);
        }
        return (IndustrialWasherTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        if (tileEntity.getLevel() == null
                || tileEntity.getLevel().getBlockEntity(tileEntity.getBlockPos()) != tileEntity) {
            return false;
        }
        BlockPos pos = tileEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, MACHINE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            int moduleSlot = tileEntity.findModuleSlot(stack);
            if (moduleSlot >= 0) {
                moved = moveItemStackTo(stack, moduleSlot, moduleSlot + 1, false);
            }
            if (!moved && tileEntity.canAcceptInput(stack)) {
                moved = moveItemStackTo(stack,
                        IndustrialWasherTileEntity.INPUT_SLOT,
                        IndustrialWasherTileEntity.INPUT_SLOT + 1,
                        false);
            }
            if (!moved) {
                if (index < PLAYER_MAIN_END) {
                    moved = moveItemStackTo(stack, PLAYER_MAIN_END, PLAYER_END, false);
                } else {
                    moved = moveItemStackTo(stack, MACHINE_SLOT_COUNT, PLAYER_MAIN_END, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    public int getEnergyStored() {
        return (data.get(1) & 0xFFFF) | ((data.get(2) & 0xFFFF) << 16);
    }

    public int getEnergyScaled(int pixels) {
        return (int) ((long) getEnergyStored() * pixels / IndustrialWasherTileEntity.CAPACITY);
    }

    public int getProgressScaled(int pixels) {
        int processTicks = Math.max(1, data.get(3));
        return data.get(0) * pixels / processTicks;
    }

    public int getCurrentEnergyPerTick() {
        return data.get(4);
    }

    public int getSpeedUpgradeCount() {
        return data.get(5);
    }

    public int getEfficiencyUpgradeCount() {
        return data.get(6);
    }

    public int getFluidAmount() {
        return data.get(7);
    }

    public int getFluidScaled(int pixels) {
        return (int) ((long) getFluidAmount() * pixels / IndustrialWasherTileEntity.TANK_CAPACITY);
    }

    public MachineSideMode getSideMode(Direction direction) {
        return sideData.getMode(direction);
    }
}
