package dev.bulkcloud0.justguithings.menu;

import dev.bulkcloud0.justguithings.registry.ModBlocks;
import dev.bulkcloud0.justguithings.registry.ModContainers;
import dev.bulkcloud0.justguithings.tile.CrusherTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerLevelAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.SlotItemHandler;

public class CrusherContainer extends Container {
    private final CrusherTileEntity tile;

    public CrusherContainer(int id, PlayerInventory playerInventory, PacketBuffer data) {
        this(id, playerInventory, getTile(playerInventory, data));
    }

    public CrusherContainer(int id, PlayerInventory playerInventory, CrusherTileEntity tile) {
        super(ModContainers.CRUSHER.get(), id);
        this.tile = tile;
        addSlot(new SlotItemHandler(tile.getInventory(), 0, 44, 35));
        addSlot(new SlotItemHandler(tile.getInventory(), 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerInventory(playerInventory);
        addDataSlots(tile.getData());
    }

    private static CrusherTileEntity getTile(PlayerInventory inventory, PacketBuffer data) {
        TileEntity tile = inventory.player.level.getBlockEntity(data.readBlockPos());
        if (!(tile instanceof CrusherTileEntity)) throw new IllegalStateException("Crusher tile entity missing");
        return (CrusherTileEntity) tile;
    }

    private void addPlayerInventory(PlayerInventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new net.minecraft.inventory.container.Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.inventory.container.Slot(inv, col, 8 + col * 18, 142));
    }

    public CrusherTileEntity getTile() { return tile; }
    public int getProgress() { return tile.getData().get(0); }
    public int getMaxProgress() { return tile.getData().get(1); }
    public int getEnergy() { return tile.getData().get(2); }
    public int getMaxEnergy() { return tile.getData().get(3); }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()), player, ModBlocks.CRUSHER.get());
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.inventory.container.Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
