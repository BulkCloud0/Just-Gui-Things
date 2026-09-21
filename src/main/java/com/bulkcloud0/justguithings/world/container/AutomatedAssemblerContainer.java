package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.AutomatedAssemblerTileEntity;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.SlotItemHandler;

public class AutomatedAssemblerContainer extends Container {
    private static final int MACHINE_SLOT_COUNT = 12;
    private static final int PLAYER_MAIN_END = MACHINE_SLOT_COUNT + 27;
    private static final int PLAYER_END = PLAYER_MAIN_END + 9;
    private static final int RECIPE_LOCK_BUTTON = 0;

    private final AutomatedAssemblerTileEntity tileEntity;
    private final IIntArray data;
    private final SideConfigContainerData sideData;

    public AutomatedAssemblerContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public AutomatedAssemblerContainer(int windowId, PlayerInventory playerInventory,
                                       AutomatedAssemblerTileEntity tileEntity) {
        super(ModContainers.AUTOMATED_ASSEMBLER.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();
        this.sideData = new SideConfigContainerData(tileEntity, tileEntity::getSideMode);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3;
                this.addSlot(new SlotItemHandler(tileEntity.getInventory(), slot,
                        30 + column * 18, 34 + row * 18));
            }
        }

        this.addSlot(new SlotItemHandler(tileEntity.getInventory(),
                AutomatedAssemblerTileEntity.OUTPUT_SLOT, 116, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(),
                AutomatedAssemblerTileEntity.SPEED_MODULE_SLOT, 148, 34));
        this.addSlot(new SlotItemHandler(tileEntity.getInventory(),
                AutomatedAssemblerTileEntity.EFFICIENCY_MODULE_SLOT, 148, 56));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
        addDataSlots(sideData);
    }

    private static AutomatedAssemblerTileEntity getTileEntity(PlayerInventory playerInventory,
                                                               PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof AutomatedAssemblerTileEntity)) {
            throw new IllegalStateException("Automated Assembler tile entity not found at " + pos);
        }
        return (AutomatedAssemblerTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 109 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column,
                    8 + column * 18, 167));
        }
    }

    @Override
    public boolean clickMenuButton(PlayerEntity player, int buttonId) {
        if (buttonId != RECIPE_LOCK_BUTTON) {
            return false;
        }

        if (!player.level.isClientSide) {
            if (tileEntity.isRecipeLocked()) {
                tileEntity.clearRecipeLock();
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.automated_assembler.recipe_unlocked"),
                        true);
            } else if (tileEntity.lockCurrentRecipe()) {
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.automated_assembler.recipe_locked"),
                        true);
            } else {
                player.displayClientMessage(
                        new TranslationTextComponent(
                                "message.justguithings.automated_assembler.recipe_invalid"),
                        true);
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        if (tileEntity.getLevel() == null
                || tileEntity.getLevel().getBlockEntity(tileEntity.getBlockPos()) != tileEntity) {
            return false;
        }
        BlockPos pos = tileEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= 64.0D;
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
            } else if (tileEntity.findModuleSlot(stack) >= 0) {
                int moduleSlot = tileEntity.findModuleSlot(stack);
                if (!this.moveItemStackTo(stack, moduleSlot, moduleSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (tileEntity.canAcceptInput(stack)) {
                if (!this.moveItemStackTo(stack, 0,
                        AutomatedAssemblerTileEntity.INPUT_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_MAIN_END) {
                if (!this.moveItemStackTo(stack, PLAYER_MAIN_END, PLAYER_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, MACHINE_SLOT_COUNT, PLAYER_MAIN_END, false)) {
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
        return (int) ((long) getEnergyStored() * pixels / AutomatedAssemblerTileEntity.CAPACITY);
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

    public boolean isRecipeLocked() {
        return data.get(7) != 0;
    }

    public MachineSideMode getSideMode(Direction direction) {
        return sideData.getMode(direction);
    }
}
