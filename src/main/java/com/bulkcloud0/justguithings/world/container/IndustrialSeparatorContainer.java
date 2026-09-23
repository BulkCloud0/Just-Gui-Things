package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.IndustrialSeparatorTileEntity;
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

public class IndustrialSeparatorContainer extends Container {
    private static final int MACHINE_SLOT_COUNT = IndustrialSeparatorTileEntity.INVENTORY_SIZE;
    private static final int PLAYER_MAIN_END = MACHINE_SLOT_COUNT + 27;
    private static final int PLAYER_END = PLAYER_MAIN_END + 9;

    private final IndustrialSeparatorTileEntity tileEntity;
    private final IIntArray data;
    private final SideConfigContainerData sideData;

    public IndustrialSeparatorContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public IndustrialSeparatorContainer(int windowId, PlayerInventory playerInventory,
                                        IndustrialSeparatorTileEntity tileEntity) {
        super(ModContainers.INDUSTRIAL_SEPARATOR.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();
        this.sideData = new SideConfigContainerData(tileEntity, tileEntity::getSideMode);

        addSlot(new SlotItemHandler(tileEntity.getInventory(), IndustrialSeparatorTileEntity.INPUT_SLOT, 36, 35));
        addSlot(outputSlot(IndustrialSeparatorTileEntity.FIRST_OUTPUT_SLOT, 108, 35));
        addSlot(outputSlot(IndustrialSeparatorTileEntity.SECOND_OUTPUT_SLOT, 126, 35));
        addSlot(outputSlot(IndustrialSeparatorTileEntity.THIRD_OUTPUT_SLOT, 144, 35));
        addSlot(new SlotItemHandler(tileEntity.getInventory(), IndustrialSeparatorTileEntity.SPEED_MODULE_SLOT, 72, 56));
        addSlot(new SlotItemHandler(tileEntity.getInventory(), IndustrialSeparatorTileEntity.EFFICIENCY_MODULE_SLOT, 90, 56));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
        addDataSlots(sideData);
    }

    private SlotItemHandler outputSlot(int slot, int x, int y) {
        return new SlotItemHandler(tileEntity.getInventory(), slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private static IndustrialSeparatorTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof IndustrialSeparatorTileEntity)) {
            throw new IllegalStateException("Industrial Separator tile entity not found at " + pos);
        }
        return (IndustrialSeparatorTileEntity) tile;
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
            int moduleSlot = tileEntity.findModuleSlot(stack);
            boolean moved = moduleSlot >= 0 && moveItemStackTo(stack, moduleSlot, moduleSlot + 1, false);
            if (!moved && tileEntity.canAcceptInput(stack)) {
                moved = moveItemStackTo(stack, IndustrialSeparatorTileEntity.INPUT_SLOT,
                        IndustrialSeparatorTileEntity.INPUT_SLOT + 1, false);
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
        return (int) ((long) getEnergyStored() * pixels / IndustrialSeparatorTileEntity.CAPACITY);
    }

    public int getProgressScaled(int pixels) {
        int ticks = Math.max(1, data.get(3));
        return data.get(0) * pixels / ticks;
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

    public MachineSideMode getSideMode(Direction direction) {
        return sideData.getMode(direction);
    }
}
