package com.bulkcloud0.justguithings.world;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;

public final class AnalogSignalHelper {
    private AnalogSignalHelper() {}

    public static int fromFill(int stored, int capacity) {
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        int clamped = Math.min(stored, capacity);
        return Math.min(15, 1 + (int) ((long) clamped * 14L / capacity));
    }

    public static int fromInventory(IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() <= 0) {
            return 0;
        }

        double fullness = 0.0D;
        int occupied = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int limit = Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize());
            if (limit <= 0) {
                continue;
            }

            fullness += (double) stack.getCount() / (double) limit;
            occupied++;
        }

        fullness /= inventory.getSlots();
        int signal = (int) Math.floor(fullness * 14.0D) + (occupied > 0 ? 1 : 0);
        return Math.max(0, Math.min(15, signal));
    }

    public static void notifyOutputChanged(TileEntity tileEntity) {
        if (tileEntity == null
                || tileEntity.getLevel() == null
                || tileEntity.getLevel().isClientSide) {
            return;
        }

        tileEntity.getLevel().updateNeighbourForOutputSignal(
                tileEntity.getBlockPos(),
                tileEntity.getBlockState().getBlock());
    }
}
