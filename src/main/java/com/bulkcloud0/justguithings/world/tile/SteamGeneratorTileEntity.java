package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.SteamGeneratorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class SteamGeneratorTileEntity extends BaseMachineTileEntity {
    public static final int ENERGY_CAPACITY = 300_000;
    public static final int GENERATION_PER_TICK = 120;
    public static final int MAX_OUTPUT_PER_TICK = 500;
    public static final int FUEL_BURN_TICKS = 1_600;
    public static final int TANK_CAPACITY = 16_000;
    public static final int WATER_PER_TICK = 5;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.FLUID_INPUT,
            MachineSideMode.ENERGY_OUTPUT
    };

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities =
            new EnumMap<>(Direction.class);

    private int burnTicksRemaining;
    private int syncedFluidAmount;

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
                case 3:
                    return level != null && level.isClientSide ? syncedFluidAmount : fluidTank.getFluidAmount();
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
                case 3:
                    syncedFluidAmount = value;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public SteamGeneratorTileEntity() {
        super(ModTileEntities.STEAM_GENERATOR.get(), ENERGY_CAPACITY, 0, MAX_OUTPUT_PER_TICK,
                1, 0, 1, 1, 0);

        setSideMode(Direction.UP, MachineSideMode.INPUT);
        setSideMode(Direction.DOWN, MachineSideMode.FLUID_INPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY_OUTPUT);

        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && isFuel(stack);
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return false;
    }

    @Override
    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY_OUTPUT;
    }

    private void initializeFluidCapabilities() {
        for (Direction direction : Direction.values()) {
            sidedFluidCapabilities.put(direction, createSidedFluidCapability(direction));
        }
    }

    private LazyOptional<IFluidHandler> createSidedFluidCapability(Direction side) {
        return LazyOptional.of(() ->
                new SidedFluidInputHandler(
                        fluidTank,
                        () -> getSideMode(side) == MachineSideMode.FLUID_INPUT));
    }

    @Override
    protected void refreshAdditionalSidedCapabilities(Direction side) {
        LazyOptional<IFluidHandler> old = sidedFluidCapabilities.put(side, createSidedFluidCapability(side));
        if (old != null) {
            old.invalidate();
        }
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;

        if (isOperationEnabled()) {
            if (burnTicksRemaining <= 0 && canGenerateTick() && tryConsumeFuel()) {
                burnTicksRemaining = FUEL_BURN_TICKS;
                changed = true;
            }

            if (burnTicksRemaining > 0 && canGenerateTick()) {
                fluidTank.drain(WATER_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
                energyStorage.addEnergy(GENERATION_PER_TICK);
                burnTicksRemaining--;
                changed = true;
            }
        }

        if (pushEnergyToNeighborsFairly(MAX_OUTPUT_PER_TICK) > 0) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean canGenerateTick() {
        return fluidTank.getFluidAmount() >= WATER_PER_TICK
                && energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored() >= GENERATION_PER_TICK;
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.isEmpty() || !isFuel(fuel)) {
            return false;
        }
        inventory.extractItem(0, 1, false);
        return true;
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && ItemTags.COALS.contains(stack.getItem());
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

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.steam_generator");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SteamGeneratorContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        burnTicksRemaining = Math.max(0, nbt.getInt("BurnTicks"));
        if (nbt.contains("Tank")) {
            fluidTank.readFromNBT(nbt.getCompound("Tank"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("BurnTicks", burnTicksRemaining);
        nbt.put("Tank", fluidTank.writeToNBT(new CompoundNBT()));
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == null || getSideMode(side) != MachineSideMode.FLUID_INPUT) {
                return LazyOptional.empty();
            }
            LazyOptional<IFluidHandler> capability = sidedFluidCapabilities.get(side);
            return capability == null ? LazyOptional.empty() : capability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IFluidHandler> capability : sidedFluidCapabilities.values()) {
            capability.invalidate();
        }
    }

    @Override
    protected void reviveCaps() {
        super.reviveCaps();
        initializeFluidCapabilities();
    }
}
