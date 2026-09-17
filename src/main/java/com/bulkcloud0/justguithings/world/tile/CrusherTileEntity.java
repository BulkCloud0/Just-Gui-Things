package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CrusherContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CrusherTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int PROCESS_TICKS = 100;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && isProcessableInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return progress;
                case 1:
                    return energyStorage.getEnergyStored() & 0xFFFF;
                case 2:
                    return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    progress = value;
                    break;
                case 1:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0xFFFF0000) | (value & 0xFFFF));
                    break;
                case 2:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private int progress;

    public CrusherTileEntity() {
        super(ModTileEntities.CRUSHER.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!canProcess()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) {
            return;
        }

        energyStorage.consumeEnergy(ENERGY_PER_TICK);
        progress++;

        if (progress >= PROCESS_TICKS) {
            processItem();
            progress = 0;
        }

        setChanged();
    }

    private boolean canProcess() {
        ItemStack result = getProcessingResult(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSame(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void processItem() {
        ItemStack result = getProcessingResult(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return;
        }

        inventory.extractItem(0, 1, false);
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, result.copy());
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(1, output);
        }
    }

    public static boolean isProcessableInput(ItemStack stack) {
        return !getProcessingResult(stack).isEmpty();
    }

    private static ItemStack getProcessingResult(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.getItem() == Items.IRON_ORE) {
            return new ItemStack(ModItems.IRON_DUST.get(), 2);
        }
        if (input.getItem() == Items.GOLD_ORE) {
            return new ItemStack(ModItems.GOLD_DUST.get(), 2);
        }
        if (input.getItem() == Items.COBBLESTONE) {
            return new ItemStack(Items.GRAVEL);
        }
        if (input.getItem() == Items.GRAVEL) {
            return new ItemStack(Items.SAND);
        }
        return ItemStack.EMPTY;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.crusher");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CrusherContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        energyStorage.setEnergy(nbt.getInt("Energy"));
        progress = nbt.getInt("Progress");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Energy", energyStorage.getEnergyStored());
        nbt.putInt("Progress", progress);
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.util.Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return energyCapability.cast();
        }
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCapability.invalidate();
        itemCapability.invalidate();
    }
}
