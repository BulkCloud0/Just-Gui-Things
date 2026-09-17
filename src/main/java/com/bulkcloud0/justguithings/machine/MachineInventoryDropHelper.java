package com.bulkcloud0.justguithings.machine;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public final class MachineInventoryDropHelper {
    private MachineInventoryDropHelper() {
    }

    public static void dropContents(World world, BlockPos pos, IItemHandler inventory) {
        if (world.isClientSide) {
            return;
        }

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                Block.popResource(world, pos, stack);
            }
        }
    }
}
