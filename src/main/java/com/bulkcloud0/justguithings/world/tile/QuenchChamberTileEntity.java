package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.recipe.QuenchingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.QuenchChamberContainer;
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

public class QuenchChamberTileEntity extends BaseProcessingMachineTileEntity<QuenchingRecipe> {
    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.OUTPUT,
            MachineSideMode.ENERGY,
            MachineSideMode.FLUID_INPUT
    };
    public static final int CAPACITY = 120_000;
    public static final int MAX_RECEIVE = 1_200;
    public static final int TANK_CAPACITY = 4_000;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;

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

    private final IFluidHandler fluidInputHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return fluidTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fluidTank.fill(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities = new EnumMap<>(Direction.class);

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            if (index <= 4) {
                return getProcessingData(index);
            }
            return index == 5
                    ? (level != null && level.isClientSide ? syncedFluidAmount : fluidTank.getFluidAmount())
                    : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index <= 4) {
                setProcessingData(index, value);
            } else if (index == 5) {
                syncedFluidAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidInputHandler);
    private int syncedFluidAmount;

    public QuenchChamberTileEntity() {
        super(ModTileEntities.QUENCH_CHAMBER.get(), CAPACITY, MAX_RECEIVE, 2, 0, 1, 1, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
        setSideMode(Direction.WEST, MachineSideMode.FLUID_INPUT);
        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptInput(stack);
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    protected MachineSideMode normalizeLoadedSideMode(Direction side, MachineSideMode mode, int configVersion) {
        if (configVersion < 2 && side == Direction.WEST && mode == MachineSideMode.INPUT) {
            return MachineSideMode.FLUID_INPUT;
        }
        return super.normalizeLoadedSideMode(side, mode, configVersion);
    }

    private void initializeFluidCapabilities() {
        for (Direction direction : Direction.values()) {
            final Direction side = direction;
            sidedFluidCapabilities.put(side, LazyOptional.of(() -> new SidedFluidInputHandler(
                    fluidInputHandler, () -> getSideMode(side) == MachineSideMode.FLUID_INPUT)));
        }
    }

    @Override
    protected Optional<QuenchingRecipe> findCurrentRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty() || fluidTank.isEmpty()) {
            return Optional.empty();
        }

        ItemStack input = inventory.getStackInSlot(0);
        FluidStack fluid = fluidTank.getFluid();
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
            if (recipe.matches(input, fluid)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean canProcessRecipe(QuenchingRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void processRecipe(QuenchingRecipe recipe) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty()) {
            return;
        }

        FluidStack drained = fluidTank.drain(recipe.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < recipe.getFluidAmount()) {
            return;
        }

        inventory.extractItem(0, 1, false);
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, result.copy());
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(1, combined);
        }
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
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
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
            if (recipe.matchesFluid(stack)) {
                return true;
            }
        }
        return false;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.quench_chamber");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new QuenchChamberContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        fluidTank.readFromNBT(nbt.getCompound("Tank"));
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
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
            if (getSideMode(side) == MachineSideMode.FLUID_INPUT) {
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
