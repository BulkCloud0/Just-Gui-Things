package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.IIntArray;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

public class CoalGeneratorContainer extends Container {
    private static final int MACHINE_SLOT_COUNT = 1;

    private final CoalGeneratorTileEntity tileEntity;
    private final IIntArray data;

    public CoalGeneratorContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public CoalGeneratorContainer(int windowId, PlayerInventory playerInventory, CoalGeneratorTileEntity tileEntity) {
        super(ModContainers.COAL_GENERATOR.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();

        this.addSlot(new SlotItemHandler(tileEntity.getInventory(), 0, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });

        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    private static CoalGeneratorTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof CoalGeneratorTileEntity)) {
            throw new IllegalStateException("Coal Generator tile entity not found at " + pos);
        }
        return (CoalGeneratorTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new net.minecraft.inventory.container.Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new net.minecraft.inventory.container.Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    142));
        }
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        if (tileEntity.getLevel() == null || tileEntity.getLevel().getBlockEntity(tileEntity.getBlockPos()) != tileEntity) {
            return false;
        }

        BlockPos pos = tileEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.inventory.container.Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFuel(stack)) {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 28) {
                if (!this.moveItemStackTo(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 1, 28, false)) {
                return ItemStack.EMPTY;
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
        return (int) ((long) getEnergyStored() * pixels / CoalGeneratorTileEntity.CAPACITY);
    }

    public int getBurnScaled(int pixels) {
        return data.get(0) * pixels / CoalGeneratorTileEntity.COAL_BURN_TICKS;
    }

    public boolean isBurning() {
        return data.get(0) > 0;
    }
}
