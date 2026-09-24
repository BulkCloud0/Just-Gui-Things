package com.bulkcloud0.justguithings.machine;

import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.recipe.SingleInputProcessingRecipe;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.IIntArray;

import java.util.Optional;

public abstract class BaseSingleInputProcessingMachineTileEntity<R extends SingleInputProcessingRecipe>
        extends BaseProcessingMachineTileEntity<R> {
    private final IRecipeType<R> recipeType;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            return getProcessingData(index);
        }

        @Override
        public void set(int index, int value) {
            setProcessingData(index, value);
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    protected BaseSingleInputProcessingMachineTileEntity(TileEntityType<?> tileEntityType,
                                                         IRecipeType<R> recipeType,
                                                         int energyCapacity,
                                                         int maxReceive,
                                                         int defaultProcessTicks,
                                                         int defaultEnergyPerTick) {
        this(tileEntityType, recipeType, energyCapacity, maxReceive, 2,
                defaultProcessTicks, defaultEnergyPerTick);
    }

    protected BaseSingleInputProcessingMachineTileEntity(TileEntityType<?> tileEntityType,
                                                         IRecipeType<R> recipeType,
                                                         int energyCapacity,
                                                         int maxReceive,
                                                         int inventorySize,
                                                         int defaultProcessTicks,
                                                         int defaultEnergyPerTick) {
        super(tileEntityType, energyCapacity, maxReceive,
                inventorySize, 0, 1, 1, 1,
                defaultProcessTicks, defaultEnergyPerTick);
        if (inventorySize < 2) {
            throw new IllegalArgumentException("Single-input processing machines require at least 2 inventory slots");
        }
        this.recipeType = recipeType;
    }

    @Override
    protected final boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptInput(stack);
    }

    @Override
    protected final Optional<R> findCurrentRecipe() {
        return findRecipe(inventory.getStackInSlot(0));
    }

    @Override
    protected boolean canProcessRecipe(R recipe) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty()) {
            return false;
        }

        return canFitItemOutput(1, result);
    }

    @Override
    protected void processRecipe(R recipe) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty()) {
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

    private Optional<R> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        R best = null;
        for (R recipe : level.getRecipeManager().getAllRecipesFor(recipeType)) {
            if (!recipe.getInput().test(input)) {
                continue;
            }
            if (best == null || RecipeSelectionHelper.compareSingleInput(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    public final boolean canAcceptInput(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    public final IIntArray getDataAccess() {
        return dataAccess;
    }
}
