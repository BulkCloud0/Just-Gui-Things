package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.machine.SidedFluidOutputHandler;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.FluidContainerStationContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class FluidContainerStationTileEntity extends BaseMachineTileEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 500;
    public static final int TANK_CAPACITY = 8_000;
    public static final int TRANSFER_RATE = 250;
    public static final int ENERGY_PER_TRANSFER = 10;

    private static final int DRAIN_INPUT_SLOT = 0;
    private static final int FILL_INPUT_SLOT = 1;
    private static final int DRAIN_OUTPUT_SLOT = 2;
    private static final int FILL_OUTPUT_SLOT = 3;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.OUTPUT,
            MachineSideMode.ENERGY,
            MachineSideMode.FLUID_INPUT,
            MachineSideMode.FLUID_OUTPUT
    };

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities =
            new EnumMap<>(Direction.class);
    private int syncedFluidAmount;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return energyStorage.getEnergyStored() & 0xFFFF;
                case 1:
                    return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 2:
                    return level != null && level.isClientSide ? syncedFluidAmount : fluidTank.getFluidAmount();
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
                case 2:
                    syncedFluidAmount = value;
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

    public FluidContainerStationTileEntity() {
        super(ModTileEntities.FLUID_CONTAINER_STATION.get(), ENERGY_CAPACITY, MAX_RECEIVE,
                4, 0, 2, 2, 2);

        setSideMode(Direction.UP, MachineSideMode.FLUID_INPUT);
        setSideMode(Direction.DOWN, MachineSideMode.FLUID_OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.INPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.OUTPUT);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY);

        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == DRAIN_INPUT_SLOT) {
            return hasDrainableFluid(stack);
        }
        if (slot == FILL_INPUT_SLOT) {
            return hasFluidCapacity(stack);
        }
        return false;
    }

    @Override
    protected int getMachineSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
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
            changed |= drainContainerIntoTank();
            changed |= fillContainerFromTank();
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean drainContainerIntoTank() {
        ItemStack input = inventory.getStackInSlot(DRAIN_INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        IFluidHandlerItem handler = getFluidHandler(input);
        if (handler == null) {
            return moveToOutput(DRAIN_INPUT_SLOT, DRAIN_OUTPUT_SLOT);
        }

        FluidStack offered = handler.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return moveToOutput(DRAIN_INPUT_SLOT, DRAIN_OUTPUT_SLOT);
        }

        int accepted = fluidTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        int planned = Math.min(offered.getAmount(), accepted);
        if (planned <= 0 || energyStorage.getEnergyStored() < ENERGY_PER_TRANSFER) {
            return false;
        }

        FluidStack drained = handler.drain(planned, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }

        int inserted = Math.max(0, Math.min(
                drained.getAmount(),
                fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE)));

        if (inserted < drained.getAmount()) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - inserted);
            handler.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }

        inventory.setStackInSlot(DRAIN_INPUT_SLOT, handler.getContainer().copy());
        if (inserted > 0) {
            energyStorage.consumeEnergy(ENERGY_PER_TRANSFER);
        }

        if (handler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
            moveToOutput(DRAIN_INPUT_SLOT, DRAIN_OUTPUT_SLOT);
        }
        return true;
    }

    private boolean fillContainerFromTank() {
        ItemStack input = inventory.getStackInSlot(FILL_INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        IFluidHandlerItem handler = getFluidHandler(input);
        if (handler == null) {
            return moveToOutput(FILL_INPUT_SLOT, FILL_OUTPUT_SLOT);
        }
        if (!hasFluidCapacity(input)) {
            return moveToOutput(FILL_INPUT_SLOT, FILL_OUTPUT_SLOT);
        }

        FluidStack offered = fluidTank.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return false;
        }

        int accepted = handler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        int planned = Math.min(offered.getAmount(), accepted);
        if (planned <= 0) {
            return moveToOutput(FILL_INPUT_SLOT, FILL_OUTPUT_SLOT);
        }
        if (energyStorage.getEnergyStored() < ENERGY_PER_TRANSFER) {
            return false;
        }

        FluidStack drained = fluidTank.drain(planned, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }

        int inserted = Math.max(0, Math.min(
                drained.getAmount(),
                handler.fill(drained, IFluidHandler.FluidAction.EXECUTE)));

        if (inserted < drained.getAmount()) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - inserted);
            fluidTank.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }

        inventory.setStackInSlot(FILL_INPUT_SLOT, handler.getContainer().copy());
        if (inserted > 0) {
            energyStorage.consumeEnergy(ENERGY_PER_TRANSFER);
        }

        FluidStack probe = drained.copy();
        probe.setAmount(1);
        if (handler.fill(probe, IFluidHandler.FluidAction.SIMULATE) <= 0) {
            moveToOutput(FILL_INPUT_SLOT, FILL_OUTPUT_SLOT);
        }
        return true;
    }

    private boolean moveToOutput(int inputSlot, int outputSlot) {
        if (!inventory.getStackInSlot(outputSlot).isEmpty()) {
            return false;
        }

        ItemStack moved = inventory.extractItem(inputSlot, 1, false);
        if (moved.isEmpty()) {
            return false;
        }
        inventory.setStackInSlot(outputSlot, moved);
        return true;
    }

    @Nullable
    private static IFluidHandlerItem getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
    }

    public static boolean hasDrainableFluid(ItemStack stack) {
        IFluidHandlerItem handler = getFluidHandler(stack);
        return handler != null
                && !handler.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE).isEmpty();
    }

    public static boolean hasFluidCapacity(ItemStack stack) {
        IFluidHandlerItem handler = getFluidHandler(stack);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (handler.getTankCapacity(tank) > handler.getFluidInTank(tank).getAmount()) {
                return true;
            }
        }
        return false;
    }

    private void initializeFluidCapabilities() {
        for (Direction direction : Direction.values()) {
            sidedFluidCapabilities.put(direction, createSidedFluidCapability(direction));
        }
    }

    private LazyOptional<IFluidHandler> createSidedFluidCapability(Direction side) {
        if (getSideMode(side) == MachineSideMode.FLUID_OUTPUT) {
            return LazyOptional.of(() ->
                    new SidedFluidOutputHandler(
                            fluidTank,
                            () -> getSideMode(side) == MachineSideMode.FLUID_OUTPUT));
        }
        return LazyOptional.of(() ->
                new SidedFluidInputHandler(
                        fluidTank,
                        () -> getSideMode(side) == MachineSideMode.FLUID_INPUT));
    }

    @Override
    protected void refreshAdditionalSidedCapabilities(Direction side) {
        LazyOptional<IFluidHandler> old = sidedFluidCapabilities.put(
                side, createSidedFluidCapability(side));
        if (old != null) {
            old.invalidate();
        }
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains("Tank")) {
            fluidTank.readFromNBT(nbt.getCompound("Tank"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Tank", fluidTank.writeToNBT(new CompoundNBT()));
        return nbt;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.fluid_container_station");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FluidContainerStationContainer(windowId, playerInventory, this);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == null
                    || (getSideMode(side) != MachineSideMode.FLUID_INPUT
                    && getSideMode(side) != MachineSideMode.FLUID_OUTPUT)) {
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
