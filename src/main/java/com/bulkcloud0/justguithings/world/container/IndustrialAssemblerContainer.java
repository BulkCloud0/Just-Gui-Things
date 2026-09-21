package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.IndustrialAssemblerTileEntity;
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

public class IndustrialAssemblerContainer extends Container {
    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_MAIN_END = MACHINE_SLOT_COUNT + 27;
    private static final int PLAYER_END = PLAYER_MAIN_END + 9;

    private final IndustrialAssemblerTileEntity tileEntity;
    private final IIntArray data;
    private final SideConfigContainerData sideData;

    public IndustrialAssemblerContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public IndustrialAssemblerContainer(int windowId, PlayerInventory playerInventory,
                                        IndustrialAssemblerTileEntity tileEntity) {
        super(ModContainers.INDUSTRIAL_ASSEMBLER.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();
        this.sideData = new SideConfigContainerData(tileEntity, tileEntity::getSideMode);

        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 0, 17, 35));
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 1, 35, 35));
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 2, 53, 35));
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 3, 71, 35));
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 4, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
        addDataSlots(data);
        addDataSlots(sideData);
    }

    private static IndustrialAssemblerTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof IndustrialAssemblerTileEntity)) {
            throw new IllegalStateException("Industrial Assembler tile entity not found at " + pos);
        }
        return (IndustrialAssemblerTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
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
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;
                for (int inputSlot = 0; inputSlot < 4 && !moved; inputSlot++) {
                    if (tileEntity.canAcceptInput(inputSlot, stack)) {
                        moved = this.moveItemStackTo(stack, inputSlot, inputSlot + 1, false);
                    }
                }

                if (!moved) {
                    if (index < PLAYER_MAIN_END) {
                        moved = this.moveItemStackTo(stack, PLAYER_MAIN_END, PLAYER_END, false);
                    } else {
                        moved = this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, PLAYER_MAIN_END, false);
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
        }
        return result;
    }

    public int getEnergyStored() {
        return (data.get(1) & 0xFFFF) | ((data.get(2) & 0xFFFF) << 16);
    }

    public int getEnergyScaled(int pixels) {
        return (int) ((long) getEnergyStored() * pixels / IndustrialAssemblerTileEntity.CAPACITY);
    }

    public int getProgressScaled(int pixels) {
        int processTicks = Math.max(1, data.get(3));
        return data.get(0) * pixels / processTicks;
    }

    public int getCurrentEnergyPerTick() {
        return data.get(4);
    }

    public MachineSideMode getSideMode(Direction direction) {
        return sideData.getMode(direction);
    }
}
