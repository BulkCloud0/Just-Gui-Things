package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidOutputHandler;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.FluidPumpContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
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

public class FluidPumpTileEntity extends BaseMachineTileEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 500;
    public static final int TANK_CAPACITY = 4_000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int CYCLE_TICKS = 20;
    public static final int WATER_PER_CYCLE = 200;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.ENERGY,
            MachineSideMode.FLUID_OUTPUT
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

    private final IFluidHandler fluidOutputHandler =
            new SidedFluidOutputHandler(fluidTank, () -> true);

    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities =
            new EnumMap<>(Direction.class);

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return level != null && level.isClientSide ? syncedProgress : progress;
                case 1:
                    return energyStorage.getEnergyStored() & 0xFFFF;
                case 2:
                    return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 3:
                    return level != null && level.isClientSide ? syncedFluidAmount : fluidTank.getFluidAmount();
                case 4:
                    return level != null && level.isClientSide ? syncedSourceValid : (hasWaterSource() ? 1 : 0);
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    syncedProgress = value;
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
                case 4:
                    syncedSourceValid = value;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidOutputHandler);
    private int progress;
    private int syncedProgress;
    private int syncedFluidAmount;
    private int syncedSourceValid;

    public FluidPumpTileEntity() {
        super(ModTileEntities.FLUID_PUMP.get(), ENERGY_CAPACITY, MAX_RECEIVE,
                0, 0, 0, 0, 0);

        setSideMode(Direction.UP, MachineSideMode.FLUID_OUTPUT);
        setSideMode(Direction.DOWN, MachineSideMode.DISABLED);
        setSideMode(Direction.NORTH, MachineSideMode.ENERGY);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY);

        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean supportsItemCapability() {
        return false;
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    private void initializeFluidCapabilities() {
        for (Direction direction : Direction.values()) {
            final Direction side = direction;
            sidedFluidCapabilities.put(side, LazyOptional.of(() ->
                    new SidedFluidOutputHandler(
                            fluidTank,
                            () -> getSideMode(side) == MachineSideMode.FLUID_OUTPUT)));
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!hasWaterSource() || fluidTank.getSpace() < WATER_PER_CYCLE) {
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

        if (progress >= CYCLE_TICKS) {
            FluidStack produced = new FluidStack(Fluids.WATER, WATER_PER_CYCLE);
            fluidTank.fill(produced, IFluidHandler.FluidAction.EXECUTE);
            progress = 0;
        }

        setChanged();
    }

    private boolean hasWaterSource() {
        if (level == null) {
            return false;
        }

        FluidState state = level.getFluidState(worldPosition.below());
        return state.getType() == Fluids.WATER && state.isSource();
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.fluid_pump");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FluidPumpContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        progress = Math.max(0, Math.min(CYCLE_TICKS - 1, nbt.getInt("Progress")));
        if (nbt.contains("Tank")) {
            fluidTank.readFromNBT(nbt.getCompound("Tank"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Progress", progress);
        nbt.put("Tank", fluidTank.writeToNBT(new CompoundNBT()));
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == null) {
                return fluidCapability.cast();
            }
            if (getSideMode(side) == MachineSideMode.FLUID_OUTPUT) {
                LazyOptional<IFluidHandler> sided = sidedFluidCapabilities.get(side);
                return sided == null ? LazyOptional.empty() : sided.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        fluidCapability.invalidate();
        for (LazyOptional<IFluidHandler> capability : sidedFluidCapabilities.values()) {
            capability.invalidate();
        }
    }
}
