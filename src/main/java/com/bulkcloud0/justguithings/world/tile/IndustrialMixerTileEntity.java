package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.machine.SidedFluidOutputHandler;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialMixerContainer;
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

public class IndustrialMixerTileEntity extends BaseProcessingMachineTileEntity<MixingRecipe> {
    public static final int CAPACITY = 150_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;
    public static final int MAX_MODULES_PER_TYPE = 4;
    public static final int TANK_CAPACITY = MixingRecipe.MAX_FLUID_AMOUNT;

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

    public IndustrialMixerTileEntity() {
        super(ModTileEntities.INDUSTRIAL_MIXER.get(), CAPACITY, MAX_RECEIVE, 6, 0, 3, 3, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
        initializeFluidCapabilities();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            return canAcceptPrimary(stack);
        }
        if (slot == 1) {
            return canAcceptSecondary(stack);
        }
        if (slot == 2) {
            return canAcceptTertiary(stack);
        }
        return false;
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        switch (slot) {
            case 4: return MachineModuleTypes.SPEED;
            case 5: return MachineModuleTypes.EFFICIENCY;
            default: return null;
        }
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
    protected Optional<MixingRecipe> findCurrentRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty() || inventory.getStackInSlot(1).isEmpty()) {
            return Optional.empty();
        }

        Inventory recipeInventory = new Inventory(
                inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy());

        MixingRecipe best = null;
        for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE)) {
            if (!recipe.matches(recipeInventory, level) || !recipe.matchesFluid(fluidTank.getFluid())) {
                continue;
            }
            if (best == null || compareMixingRecipes(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    private int compareMixingRecipes(MixingRecipe left, MixingRecipe right) {
        if (left.hasFluidIngredient() != right.hasFluidIngredient()) {
            return left.hasFluidIngredient() ? -1 : 1;
        }

        int specificity = RecipeSelectionHelper.compareIngredientSets(
                left.getIngredients(), right.getIngredients());
        if (specificity != 0) {
            return specificity;
        }

        if (left.hasFluidIngredient() && left.isFluidTagBased() != right.isFluidTagBased()) {
            return left.isFluidTagBased() ? 1 : -1;
        }
        return RecipeSelectionHelper.compareIds(left.getId(), right.getId());
    }

    @Override
    protected boolean canProcessRecipe(MixingRecipe recipe) {
        if (inventory.getStackInSlot(0).getCount() < recipe.getPrimaryCount()
                || inventory.getStackInSlot(1).getCount() < recipe.getSecondaryCount()
                || (recipe.hasTertiary()
                && inventory.getStackInSlot(2).getCount() < recipe.getTertiaryCount())
                || !recipe.matchesFluid(fluidTank.getFluid())) {
            return false;
        }

        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return false;
        }

        return canFitItemOutput(3, result);
    }

    @Override
    protected int getEffectiveProcessingTime(MixingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(MixingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount(),
                1);
    }

    @Override
    protected void processRecipe(MixingRecipe recipe) {
        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return;
        }

        inventory.extractItem(0, recipe.getPrimaryCount(), false);
        inventory.extractItem(1, recipe.getSecondaryCount(), false);
        if (recipe.hasTertiary()) {
            inventory.extractItem(2, recipe.getTertiaryCount(), false);
        }
        if (recipe.hasFluidIngredient()) {
            fluidTank.drain(recipe.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) {
            inventory.setStackInSlot(3, result);
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(3, combined);
        }
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public boolean canAcceptFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE)) {
            if (recipe.hasFluidIngredient() && recipe.matchesFluidType(stack.getFluid())) {
                return true;
            }
        }
        return false;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public boolean canAcceptPrimary(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE)) {
            if (recipe.getPrimary().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptSecondary(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE)) {
            if (recipe.getSecondary().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptTertiary(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE)) {
            if (recipe.hasTertiary() && recipe.getTertiary() != null && recipe.getTertiary().test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        int savedInventorySize = nbt.contains("Inventory")
                ? nbt.getCompound("Inventory").getInt("Size")
                : 0;
        super.load(state, nbt);

        if (savedInventorySize == 3
                && !inventory.getStackInSlot(2).isEmpty()
                && inventory.getStackInSlot(3).isEmpty()) {
            inventory.setStackInSlot(3, inventory.getStackInSlot(2).copy());
            inventory.setStackInSlot(2, ItemStack.EMPTY);
        }

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
        return new TranslationTextComponent("container.justguithings.industrial_mixer");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IndustrialMixerContainer(windowId, playerInventory, this);
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
