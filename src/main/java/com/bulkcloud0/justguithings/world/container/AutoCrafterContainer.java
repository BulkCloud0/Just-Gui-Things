package com.bulkcloud0.justguithings.world.container;

import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModContainers;
import com.bulkcloud0.justguithings.world.tile.AutoCrafterTileEntity;
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

public class AutoCrafterContainer extends Container {
    public static final int BUTTON_TOGGLE_LOCK = 0;
    private static final int MACHINE_SLOT_COUNT = AutoCrafterTileEntity.INVENTORY_SIZE;
    private static final int PLAYER_MAIN_END = MACHINE_SLOT_COUNT + 27;
    private static final int PLAYER_END = PLAYER_MAIN_END + 9;

    private final AutoCrafterTileEntity tileEntity;
    private final IIntArray data;
    private final SideConfigContainerData sideData;

    public AutoCrafterContainer(int windowId, PlayerInventory playerInventory, PacketBuffer buffer) {
        this(windowId, playerInventory, getTileEntity(playerInventory, buffer));
    }

    public AutoCrafterContainer(int windowId, PlayerInventory playerInventory, AutoCrafterTileEntity tileEntity) {
        super(ModContainers.AUTO_CRAFTER.get(), windowId);
        this.tileEntity = tileEntity;
        this.data = tileEntity.getDataAccess();
        this.sideData = new SideConfigContainerData(tileEntity, tileEntity::getSideMode);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new SlotItemHandler(tileEntity.getInventory(), column + row * 3,
                        8 + column * 18, 32 + row * 18));
            }
        }
        addOutputSlot(AutoCrafterTileEntity.PRIMARY_OUTPUT_SLOT, 106, 50);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addOutputSlot(AutoCrafterTileEntity.RETURN_START + column + row * 3,
                        140 + column * 18, 32 + row * 18);
            }
        }
        addSlot(new SlotItemHandler(tileEntity.getInventory(), AutoCrafterTileEntity.SPEED_MODULE_SLOT, 75, 72));
        addSlot(new SlotItemHandler(tileEntity.getInventory(), AutoCrafterTileEntity.EFFICIENCY_MODULE_SLOT, 93, 72));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
        addDataSlots(sideData);
    }

    private void addOutputSlot(int slot, int x, int y) {
        addSlot(new SlotItemHandler(tileEntity.getInventory(), slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    private static AutoCrafterTileEntity getTileEntity(PlayerInventory playerInventory, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        TileEntity tile = playerInventory.player.level.getBlockEntity(pos);
        if (!(tile instanceof AutoCrafterTileEntity)) {
            throw new IllegalStateException("Auto Crafter tile entity not found at " + pos);
        }
        return (AutoCrafterTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        25 + column * 18, 111 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 25 + column * 18, 169));
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
    public boolean clickMenuButton(PlayerEntity player, int buttonId) {
        if (buttonId != BUTTON_TOGGLE_LOCK) {
            return false;
        }
        if (tileEntity.isRecipeLocked()) {
            tileEntity.clearRecipeLock();
            player.displayClientMessage(new TranslationTextComponent(
                    "message.justguithings.auto_crafter.unlocked"), true);
            return true;
        }
        if (tileEntity.tryLockRecipe()) {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.justguithings.auto_crafter.locked"), true);
        } else {
            player.displayClientMessage(new TranslationTextComponent(
                    "message.justguithings.auto_crafter.invalid_recipe"), true);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            int moduleSlot = tileEntity.findModuleSlot(stack);
            if (moduleSlot >= 0) {
                moved = moveItemStackTo(stack, moduleSlot, moduleSlot + 1, false);
            }
            for (int inputSlot = 0; inputSlot < AutoCrafterTileEntity.GRID_SIZE && !moved; inputSlot++) {
                if (tileEntity.canAcceptInput(inputSlot, stack)) {
                    moved = moveItemStackTo(stack, inputSlot, inputSlot + 1, false);
                }
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
        return (int) ((long) getEnergyStored() * pixels / AutoCrafterTileEntity.CAPACITY);
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
