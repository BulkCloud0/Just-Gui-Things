package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.logistics.ItemTransferHelper;
import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.VacuumCollectorContainer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class VacuumCollectorTileEntity extends BaseMachineTileEntity {
    public static final int INVENTORY_SIZE = 9;
    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 400;
    public static final int COLLECTION_RANGE = 4;
    public static final int SCAN_INTERVAL_TICKS = 5;
    public static final int MAX_ITEMS_PER_SCAN = 16;
    public static final int ENERGY_PER_ITEM = 20;
    public static final int AUTO_EJECT_RATE = 8;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.OUTPUT,
            MachineSideMode.ENERGY
    };

    private int scanTicker;

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

    public VacuumCollectorTileEntity() {
        super(ModTileEntities.VACUUM_COLLECTOR.get(), CAPACITY, MAX_RECEIVE,
                INVENTORY_SIZE, 0, 0, 0, INVENTORY_SIZE);

        setSideMode(Direction.UP, MachineSideMode.DISABLED);
        setSideMode(Direction.DOWN, MachineSideMode.OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ENERGY);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY);
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
        if (level == null || level.isClientSide) {
            return;
        }

        pushOutputItemsToNeighborsFairly(AUTO_EJECT_RATE);

        scanTicker++;
        if (scanTicker < SCAN_INTERVAL_TICKS) {
            return;
        }
        scanTicker = 0;

        if (!isOperationEnabled()) {
            return;
        }

        collectNearbyItems();
    }

    private void collectNearbyItems() {
        int affordable = energyStorage.getEnergyStored() / ENERGY_PER_ITEM;
        int remainingBudget = Math.min(MAX_ITEMS_PER_SCAN, affordable);
        if (remainingBudget <= 0) {
            return;
        }

        AxisAlignedBB bounds = new AxisAlignedBB(worldPosition).inflate(COLLECTION_RANGE);
        List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, bounds);
        if (nearby.isEmpty()) {
            return;
        }

        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + 0.5D;
        double centerZ = worldPosition.getZ() + 0.5D;
        nearby.sort(Comparator.comparingDouble(entity ->
                entity.distanceToSqr(centerX, centerY, centerZ)));

        boolean changed = false;
        for (ItemEntity entity : nearby) {
            if (remainingBudget <= 0 || energyStorage.getEnergyStored() < ENERGY_PER_ITEM) {
                break;
            }
            if (!entity.isAlive()) {
                continue;
            }

            ItemStack source = entity.getItem();
            if (source.isEmpty()) {
                continue;
            }

            int offeredCount = Math.min(source.getCount(), remainingBudget);
            ItemStack offered = source.copy();
            offered.setCount(offeredCount);

            ItemStack simulatedRemainder = ItemTransferHelper.insert(inventory, offered, true);
            int insertable = offeredCount - simulatedRemainder.getCount();
            if (insertable <= 0) {
                continue;
            }

            int affordableNow = energyStorage.getEnergyStored() / ENERGY_PER_ITEM;
            int requested = Math.min(insertable, Math.min(remainingBudget, affordableNow));
            if (requested <= 0) {
                break;
            }

            ItemStack toInsert = source.copy();
            toInsert.setCount(requested);
            ItemStack executionRemainder = ItemTransferHelper.insert(inventory, toInsert, false);
            int inserted = requested - executionRemainder.getCount();
            if (inserted <= 0) {
                continue;
            }

            energyStorage.consumeEnergy(inserted * ENERGY_PER_ITEM);
            remainingBudget -= inserted;

            ItemStack remainingEntityStack = source.copy();
            remainingEntityStack.shrink(inserted);
            if (remainingEntityStack.isEmpty()) {
                entity.remove();
            } else {
                entity.setItem(remainingEntityStack);
            }
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.vacuum_collector");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new VacuumCollectorContainer(windowId, playerInventory, this);
    }
}
