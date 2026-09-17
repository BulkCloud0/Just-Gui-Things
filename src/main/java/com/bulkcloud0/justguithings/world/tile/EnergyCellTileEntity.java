package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.EnergyCellContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class EnergyCellTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 1_000_000;
    public static final int MAX_TRANSFER = 2_000;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };

    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities = new EnumMap<>(Direction.class);

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

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public EnergyCellTileEntity() {
        super(ModTileEntities.ENERGY_CELL.get());
        initializeDefaultSides();
        for (Direction direction : Direction.values()) {
            sidedEnergyCapabilities.put(direction, LazyOptional.of(() -> new SidedEnergyStorage(direction)));
        }
    }

    private void initializeDefaultSides() {
        sideModes.put(Direction.UP, MachineSideMode.INPUT);
        sideModes.put(Direction.DOWN, MachineSideMode.OUTPUT);
        sideModes.put(Direction.NORTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.SOUTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.WEST, MachineSideMode.ENERGY);
        sideModes.put(Direction.EAST, MachineSideMode.ENERGY);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide || energyStorage.getEnergyStored() <= 0) {
            return;
        }

        int budget = Math.min(MAX_TRANSFER, energyStorage.getEnergyStored());
        for (Direction direction : Direction.values()) {
            MachineSideMode mode = getSideMode(direction);
            if (mode != MachineSideMode.OUTPUT && mode != MachineSideMode.ENERGY) {
                continue;
            }
            if (budget <= 0 || energyStorage.getEnergyStored() <= 0) {
                break;
            }

            TileEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null || neighbor instanceof EnergyCellTileEntity) {
                continue;
            }

            IEnergyStorage receiver = neighbor
                    .getCapability(CapabilityEnergy.ENERGY, direction.getOpposite())
                    .orElse(null);
            if (receiver == null || !receiver.canReceive()) {
                continue;
            }

            int offer = Math.min(budget, energyStorage.getEnergyStored());
            int accepted = receiver.receiveEnergy(offer, true);
            if (accepted <= 0) {
                continue;
            }

            int extracted = energyStorage.extractEnergy(accepted, false);
            int inserted = receiver.receiveEnergy(extracted, false);
            if (inserted < extracted) {
                energyStorage.addEnergy(extracted - inserted);
            }
            budget -= inserted;
        }
    }

    public MachineSideMode getSideMode(Direction side) {
        return sideModes.getOrDefault(side, MachineSideMode.DISABLED);
    }

    public MachineSideMode cycleSideMode(Direction side) {
        MachineSideMode mode = getSideMode(side).next();
        sideModes.put(side, mode);
        setChanged();
        return mode;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.energy_cell");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnergyCellContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        energyStorage.setEnergy(nbt.getInt("Energy"));

        if (nbt.contains("SideConfig")) {
            CompoundNBT sideConfig = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (sideConfig.contains(key)) {
                    sideModes.put(direction, MachineSideMode.fromOrdinal(sideConfig.getInt(key)));
                }
            }
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Energy", energyStorage.getEnergyStored());

        CompoundNBT sideConfig = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            sideConfig.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", sideConfig);
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (side == null) {
                return energyCapability.cast();
            }
            if (getSideMode(side) == MachineSideMode.DISABLED) {
                return LazyOptional.empty();
            }
            LazyOptional<IEnergyStorage> sided = sidedEnergyCapabilities.get(side);
            return sided == null ? LazyOptional.empty() : sided.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCapability.invalidate();
        for (LazyOptional<IEnergyStorage> capability : sidedEnergyCapabilities.values()) {
            capability.invalidate();
        }
    }

    private final class SidedEnergyStorage implements IEnergyStorage {
        private final Direction side;

        private SidedEnergyStorage(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            MachineSideMode mode = getSideMode(side);
            if (mode != MachineSideMode.INPUT && mode != MachineSideMode.ENERGY) {
                return 0;
            }
            return energyStorage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            MachineSideMode mode = getSideMode(side);
            if (mode != MachineSideMode.OUTPUT && mode != MachineSideMode.ENERGY) {
                return 0;
            }
            return energyStorage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            MachineSideMode mode = getSideMode(side);
            return mode == MachineSideMode.OUTPUT || mode == MachineSideMode.ENERGY;
        }

        @Override
        public boolean canReceive() {
            MachineSideMode mode = getSideMode(side);
            return mode == MachineSideMode.INPUT || mode == MachineSideMode.ENERGY;
        }
    }
}
