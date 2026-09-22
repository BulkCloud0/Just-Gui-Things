package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.machine.SidedFluidOutputHandler;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.WashingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialWasherContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
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
import java.util.Optional;

public class IndustrialWasherTileEntity extends BaseProcessingMachineTileEntity<WashingRecipe> {
    public static final int CAPACITY = 120_000;
    public static final int MAX_RECEIVE = 1_200;
    public static final int DEFAULT_PROCESS_TICKS = 100;
    public static final int DEFAULT_ENERGY_PER_TICK = 30;
    public static final int MAX_MODULES_PER_TYPE = 4;
    public static final int TANK_CAPACITY = 4_000;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SPEED_MODULE_SLOT = 2;
    public static final int EFFICIENCY_MODULE_SLOT = 3;
    public static final int INVENTORY_SIZE = 4;

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
    private int syncedFluidAmount;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            if (index <= 4) {
                return getProcessingData(index);
            }
            switch (index) {
                case 5: return getSpeedUpgradeCount();
                case 6: return getEfficiencyUpgradeCount();
                case 7: return level != null && level.isClientSide
                        ? syncedFluidAmount
                        : fluidTank.getFluidAmount();
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            if (index <= 4) {
                setProcessingData(index, value);
            } else if (index == 7) {
                syncedFluidAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public IndustrialWasherTileEntity() {
        super(ModTileEntities.INDUSTRIAL_WASHER.get(), CAPACITY, MAX_RECEIVE,
                INVENTORY_SIZE, INPUT_SLOT, 1, OUTPUT_SLOT, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);

        setSideMode(Direction.UP, MachineSideMode.INPUT);
        setSideMode(Direction.DOWN, MachineSideMode.OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.FLUID_INPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.ENERGY);
        setSideMode(Direction.WEST, MachineSideMode.ENERGY);
        setSideMode(Direction.EAST, MachineSideMode.ENERGY);

        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && canAcceptInput(stack);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        if (slot == SPEED_MODULE_SLOT) {
            return MachineModuleTypes.SPEED;
        }
        if (slot == EFFICIENCY_MODULE_SLOT) {
            return MachineModuleTypes.EFFICIENCY;
        }
        return null;
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
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
    protected Optional<WashingRecipe> findCurrentRecipe() {
        if (level == null || inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            return Optional.empty();
        }

        Inventory recipeInventory = new Inventory(inventory.getStackInSlot(INPUT_SLOT).copy());
        for (WashingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.WASHING_TYPE)) {
            if (recipe.matches(recipeInventory, level) && recipe.matchesFluid(fluidTank.getFluid())) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean canProcessRecipe(WashingRecipe recipe) {
        if (!recipe.matchesFluid(fluidTank.getFluid())) {
            return false;
        }

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack result = recipe.getResultForInput(input);
        if (input.isEmpty() || result.isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected int getEffectiveProcessingTime(WashingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(WashingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount(),
                1);
    }

    @Override
    protected void processRecipe(WashingRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack result = recipe.getResultForInput(input);
        if (input.isEmpty() || result.isEmpty() || !recipe.matchesFluid(fluidTank.getFluid())) {
            return;
        }

        inventory.extractItem(INPUT_SLOT, 1, false);
        fluidTank.drain(recipe.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(OUTPUT_SLOT, combined);
        }
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (WashingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.WASHING_TYPE)) {
            if (recipe.getInput().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (WashingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.WASHING_TYPE)) {
            if (recipe.matchesFluidType(stack.getFluid())) {
                return true;
            }
        }
        return false;
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

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.industrial_washer");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IndustrialWasherContainer(windowId, playerInventory, this);
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
