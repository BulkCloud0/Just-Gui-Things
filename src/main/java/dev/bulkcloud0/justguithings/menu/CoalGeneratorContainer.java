package dev.bulkcloud0.justguithings.menu;

import dev.bulkcloud0.justguithings.registry.ModBlocks;
import dev.bulkcloud0.justguithings.registry.ModContainers;
import dev.bulkcloud0.justguithings.tile.CoalGeneratorTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IWorldPosCallable;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.SlotItemHandler;

public class CoalGeneratorContainer extends Container {
    private final CoalGeneratorTileEntity tile;

    public CoalGeneratorContainer(int id, PlayerInventory inventory, PacketBuffer data) {
        this(id, inventory, getTile(inventory, data));
    }

    public CoalGeneratorContainer(int id, PlayerInventory inventory, CoalGeneratorTileEntity tile) {
        super(ModContainers.COAL_GENERATOR.get(), id);
        this.tile = tile;
        addSlot(new SlotItemHandler(tile.getInventory(), 0, 80, 35));
        addPlayerInventory(inventory);
        addDataSlots(tile.getData());
    }

    private static CoalGeneratorTileEntity getTile(PlayerInventory inventory, PacketBuffer data) {
        TileEntity tile = inventory.player.level.getBlockEntity(data.readBlockPos());
        if (!(tile instanceof CoalGeneratorTileEntity)) throw new IllegalStateException("Coal generator tile entity missing");
        return (CoalGeneratorTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new net.minecraft.inventory.container.Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.inventory.container.Slot(inv, col, 8 + col * 18, 142));
    }

    public CoalGeneratorTileEntity getTile() { return tile; }
    public int getBurnTime() { return tile.getData().get(0); }
    public int getMaxBurnTime() { return tile.getData().get(1); }
    public int getEnergy() { return tile.getData().get(2); }
    public int getMaxEnergy() { return tile.getData().get(3); }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(IWorldPosCallable.create(tile.getLevel(), tile.getBlockPos()), player, ModBlocks.COAL_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.inventory.container.Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else if (tile.getInventory().isItemValid(0, stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
