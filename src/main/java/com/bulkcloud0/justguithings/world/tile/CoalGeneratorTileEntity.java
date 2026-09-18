package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class CoalGeneratorTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int COAL_BURN_TICKS = 1_600;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.ENERGY_OUTPUT,
            MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT
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

    private int burnTicksRemaining;

    public CoalGeneratorTileEntity() {
        super(ModTileEntities.COAL_GENERATOR.get(), CAPACITY, 0, MAX_OUTPUT_PER_TICK,
                1, 0, 1, 1, 0);

        setSideMode(Direction.UP, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.DOWN, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.WEST, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.EAST, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && isCoalFuel(stack);
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    protected MachineSideMode getItemSideMode(Direction side, MachineSideMode mode) {
        if (mode == MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT) {
            return MachineSideMode.INPUT;
        }
        return mode;
    }

    @Override
    protected MachineSideMode normalizeLoadedSideMode(Direction side, MachineSideMode mode, int configVersion) {
        if (configVersion < 5 && mode == MachineSideMode.ENERGY_OUTPUT) {
            return MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT;
        }
        return super.normalizeLoadedSideMode(side, mode, configVersion);
    }

    @Override
    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return false;
    }

    @Override
    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY_OUTPUT
                || mode == MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT;
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

            MachineSideMode mode = getSideMode(direction);
            if (!canExtractEnergyFrom(direction, mode)) {
                continue;
            }

            TileEntity neighbor = getLoadedBlockEntity(worldPosition.relative(direction));
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
            int accepted = receiver.receiveEnergy(offer, true);
            if (accepted <= 0) {
                continue;
            }

            int extracted = energyStorage.extractEnergy(accepted, false);
            int inserted = receiver.receiveEnergy(extracted, false);
            if (inserted < extracted) {
                energyStorage.addEnergy(extracted - inserted);
            }

            remainingOutput -= inserted;
            transferred += inserted;
        }

        return transferred;
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
        burnTicksRemaining = nbt.getInt("BurnTicks");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("BurnTicks", burnTicksRemaining);
        return nbt;
    }
}
