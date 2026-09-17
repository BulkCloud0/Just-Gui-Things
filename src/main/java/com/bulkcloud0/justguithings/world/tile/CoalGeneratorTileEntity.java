package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoalGeneratorTileEntity extends TileEntity implements ITickableTileEntity {
    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int COAL_BURN_TICKS = 1_600;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(CAPACITY, 0, 200);
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
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
            tryConsumeCoal();
        }

        if (burnTicksRemaining > 0) {
            int generated = energyStorage.addEnergy(GENERATION_PER_TICK);
            if (generated > 0) {
                burnTicksRemaining--;
                setChanged();
            }
        }
    }

    private void tryConsumeCoal() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (!fuel.isEmpty() && fuel.getItem() == Items.COAL) {
            inventory.extractItem(0, 1, false);
            burnTicksRemaining = COAL_BURN_TICKS;
            setChanged();
        }
    }

    public int getBurnTicksRemaining() {
        return burnTicksRemaining;
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
