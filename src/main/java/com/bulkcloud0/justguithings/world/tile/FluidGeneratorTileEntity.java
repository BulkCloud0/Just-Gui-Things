package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.machine.SidedFluidOutputHandler;
import com.bulkcloud0.justguithings.recipe.FluidFuelRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.FluidGeneratorContainer;
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
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class FluidGeneratorTileEntity extends BaseMachineTileEntity {
    public static final int ENERGY_CAPACITY = 400_000;
    public static final int GENERATION_PER_TICK = 200;
    public static final int MAX_OUTPUT_PER_TICK = 500;
    public static final int TANK_CAPACITY = 16_000;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.FLUID_INPUT,
            MachineSideMode.FLUID_OUTPUT,
            MachineSideMode.ENERGY_OUTPUT
    };

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return canAcceptFluid(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities =
            new EnumMap<>(Direction.class);

    private int batchEnergyRemaining;
    private int syncedFluidAmount;
    private int syncedBatchEnergyRemaining;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getEnergyStored();
            int fluid = level != null && level.isClientSide
                    ? syncedFluidAmount
                    : fluidTank.getFluidAmount();
            int batch = level != null && level.isClientSide
                    ? syncedBatchEnergyRemaining
                    : batchEnergyRemaining;
            switch (index) {
                case 0: return energy & 0xFFFF;
                case 1: return (energy >>> 16) & 0xFFFF;
                case 2: return fluid;
                case 3: return batch & 0xFFFF;
                case 4: return (batch >>> 16) & 0xFFFF;
                default: return 0;
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
                case 3:
                    syncedBatchEnergyRemaining = (syncedBatchEnergyRemaining & 0xFFFF0000) | (value & 0xFFFF);
                    break;
                case 4:
                    syncedBatchEnergyRemaining = (syncedBatchEnergyRemaining & 0x0000FFFF) | ((value & 0xFFFF) << 16);
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

    public FluidGeneratorTileEntity() {
        super(ModTileEntities.FLUID_GENERATOR.get(), ENERGY_CAPACITY, 0, MAX_OUTPUT_PER_TICK,
                0, 0, 0, 0, 0);

        setSideMode(Direction.UP, MachineSideMode.FLUID_INPUT);
        setSideMode(Direction.DOWN, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY_OUTPUT);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY_OUTPUT);

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

    @Override
    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return false;
    }

    @Override
    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY_OUTPUT;
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
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

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;

        if (isOperationEnabled()) {
            if (batchEnergyRemaining <= 0
                    && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()
                    && tryStartFuelBatch()) {
                changed = true;
            }

            if (batchEnergyRemaining > 0) {
                int free = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
                int generated = Math.min(GENERATION_PER_TICK, Math.min(batchEnergyRemaining, free));
                if (generated > 0) {
                    energyStorage.addEnergy(generated);
                    batchEnergyRemaining -= generated;
                    changed = true;
                }
            }
        }

        if (pushEnergyToNeighborsFairly(MAX_OUTPUT_PER_TICK) > 0) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean tryStartFuelBatch() {
        FluidFuelRecipe recipe = findFuelRecipe();
        if (recipe == null) {
            return false;
        }

        FluidStack drained = fluidTank.drain(recipe.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != recipe.getFluidAmount()) {
            if (!drained.isEmpty()) {
                fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
            return false;
        }

        batchEnergyRemaining = recipe.getEnergy();
        return true;
    }

    @Nullable
    private FluidFuelRecipe findFuelRecipe() {
        if (level == null || fluidTank.getFluid().isEmpty()) {
            return null;
        }

        FluidFuelRecipe best = null;
        for (FluidFuelRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.FLUID_FUEL_TYPE)) {
            if (!recipe.matchesFluid(fluidTank.getFluid())) {
                continue;
            }
            if (best == null || compareFuelRecipes(recipe, best) < 0) {
                best = recipe;
            }
        }
        return best;
    }

    private int compareFuelRecipes(FluidFuelRecipe left, FluidFuelRecipe right) {
        if (left.isTagBased() != right.isTagBased()) {
            return left.isTagBased() ? 1 : -1;
        }
        return left.getId().toString().compareTo(right.getId().toString());
    }

    public boolean canAcceptFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (FluidFuelRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.FLUID_FUEL_TYPE)) {
            if (recipe.matchesFluidType(stack.getFluid())) {
                return true;
            }
        }
        return false;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getBatchEnergyRemaining() {
        return batchEnergyRemaining;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.fluid_generator");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FluidGeneratorContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        batchEnergyRemaining = Math.max(0, nbt.getInt("BatchEnergy"));
        if (nbt.contains("Tank")) {
            fluidTank.readFromNBT(nbt.getCompound("Tank"));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("BatchEnergy", batchEnergyRemaining);
        nbt.put("Tank", fluidTank.writeToNBT(new CompoundNBT()));
        return nbt;
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
