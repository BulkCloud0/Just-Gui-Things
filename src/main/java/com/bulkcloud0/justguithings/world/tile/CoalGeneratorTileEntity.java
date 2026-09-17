package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IIntArray;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
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

public class CoalGeneratorTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int COAL_BURN_TICKS = 1_600;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(CAPACITY, 0, MAX_OUTPUT_PER_TICK);
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
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
                    return burnTicksRemaining;
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
                    burnTicksRemaining = value;
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
    private int burnTicksRemaining;

    public CoalGeneratorTileEntity() {
        super(ModTileEntities.COAL_GENERATOR.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (burnTicksRemaining <= 0 && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            tryConsumeFuel();
        }

        boolean changed = false;

        if (burnTicksRemaining > 0) {
            int generated = energyStorage.addEnergy(GENERATION_PER_TICK);
            if (generated > 0) {
                burnTicksRemaining--;
                changed = true;
            }
        }

        if (pushEnergyToNeighbors() > 0) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private void tryConsumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (!fuel.isEmpty() && isCoalFuel(fuel)) {
            inventory.extractItem(0, 1, false);
            burnTicksRemaining = COAL_BURN_TICKS;
            setChanged();
        }
    }

    private boolean isCoalFuel(ItemStack stack) {
        return stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL;
    }

    private int pushEnergyToNeighbors() {
        if (level == null || energyStorage.getEnergyStored() <= 0) {
            return 0;
        }

        int remainingOutput = Math.min(MAX_OUTPUT_PER_TICK, energyStorage.getEnergyStored());
        int transferred = 0;

        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || energyStorage.getEnergyStored() <= 0) {
                break;
            }

            TileEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }

            IEnergyStorage receiver = neighbor
                    .getCapability(CapabilityEnergy.ENERGY, direction.getOpposite())
                    .orElse(null);

            if (receiver == null || !receiver.canReceive()) {
                continue;
            }

            int offer = Math.min(remainingOutput, energyStorage.getEnergyStored());
            int accepted = receiver.receiveEnergy(offer, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                remainingOutput -= accepted;
                transferred += accepted;
            }
        }

        return transferred;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    public int getBurnTicksRemaining() {
        return burnTicksRemaining;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.coal_generator");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CoalGeneratorContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        energyStorage.setEnergy(nbt.getInt("Energy"));
        burnTicksRemaining = nbt.getInt("BurnTicks");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Energy", energyStorage.getEnergyStored());
        nbt.putInt("BurnTicks", burnTicksRemaining);
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
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
