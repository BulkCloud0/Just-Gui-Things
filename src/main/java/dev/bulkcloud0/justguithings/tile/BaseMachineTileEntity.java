package dev.bulkcloud0.justguithings.tile;

import dev.bulkcloud0.justguithings.energy.ModEnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseMachineTileEntity extends TileEntity {
    protected final ModEnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyCapability;

    protected BaseMachineTileEntity(TileEntityType<?> type, int capacity, int maxReceive, int maxExtract) {
        super(type);
        this.energyStorage = new ModEnergyStorage(capacity, maxReceive, maxExtract, this::setChanged);
        this.energyCapability = LazyOptional.of(() -> energyStorage);
    }

    public ModEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Energy", energyStorage.getEnergyStored());
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        energyStorage.setEnergy(nbt.getInt("Energy"));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCapability.invalidate();
    }
}
