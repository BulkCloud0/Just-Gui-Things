package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.AssemblyRecipe;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialAssemblerContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public class IndustrialAssemblerTileEntity extends BaseProcessingMachineTileEntity<AssemblyRecipe> {
    public static final int CAPACITY = 160_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 120;
    public static final int DEFAULT_ENERGY_PER_TICK = 50;
    public static final int MAX_MODULES_PER_TYPE = 4;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            if (index <= 4) {
                return getProcessingData(index);
            }
            switch (index) {
                case 5: return getSpeedUpgradeCount();
                case 6: return getEfficiencyUpgradeCount();
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            setProcessingData(index, value);
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public IndustrialAssemblerTileEntity() {
        super(ModTileEntities.INDUSTRIAL_ASSEMBLER.get(), CAPACITY, MAX_RECEIVE,
                7, 0, 4, 4, 1, DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < AssemblyRecipe.MAX_INPUTS && canAcceptInput(slot, stack);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        switch (slot) {
            case 5: return MachineModuleTypes.SPEED;
            case 6: return MachineModuleTypes.EFFICIENCY;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected Optional<AssemblyRecipe> findCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        Inventory recipeInventory = createRecipeInventory();
        AssemblyRecipe best = null;
        for (AssemblyRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE)) {
            if (recipe.findMatchingSlots(recipeInventory) == null) {
                continue;
            }
            if (best == null || compareAssemblyRecipes(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    private int compareAssemblyRecipes(AssemblyRecipe left, AssemblyRecipe right) {
        int specificity = RecipeSelectionHelper.compareIngredientSets(
                left.getIngredients(), right.getIngredients());
        if (specificity != 0) {
            return specificity;
        }

        int leftCount = 0;
        for (int index = 0; index < left.getInputCount(); index++) {
            leftCount += left.getRequiredCount(index);
        }
        int rightCount = 0;
        for (int index = 0; index < right.getInputCount(); index++) {
            rightCount += right.getRequiredCount(index);
        }
        int countSpecificity = Integer.compare(rightCount, leftCount);
        if (countSpecificity != 0) {
            return countSpecificity;
        }
        return RecipeSelectionHelper.compareIds(left.getId(), right.getId());
    }

    private Inventory createRecipeInventory() {
        return new Inventory(
                inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy(),
                inventory.getStackInSlot(3).copy());
    }

    @Override
    protected boolean canProcessRecipe(AssemblyRecipe recipe) {
        Inventory recipeInventory = createRecipeInventory();
        if (recipe.findMatchingSlots(recipeInventory) == null) {
            return false;
        }

        ItemStack result = recipe.getResultForInventory(recipeInventory);
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(4);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSame(output, result)
                && ItemStack.tagMatches(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected int getEffectiveProcessingTime(AssemblyRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(AssemblyRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount(),
                1);
    }

    @Override
    protected void processRecipe(AssemblyRecipe recipe) {
        Inventory recipeInventory = createRecipeInventory();
        ItemStack result = recipe.getResultForInventory(recipeInventory);
        if (result.isEmpty()) {
            return;
        }

        int[] ingredientSlots = recipe.findMatchingSlots(recipeInventory);
        if (ingredientSlots == null) {
            return;
        }
        for (int ingredient = 0; ingredient < recipe.getInputCount(); ingredient++) {
            inventory.extractItem(
                    ingredientSlots[ingredient],
                    recipe.getRequiredCount(ingredient),
                    false);
        }

        ItemStack output = inventory.getStackInSlot(4);
        if (output.isEmpty()) {
            inventory.setStackInSlot(4, result);
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(4, combined);
        }
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (level == null || stack.isEmpty() || slot < 0 || slot >= AssemblyRecipe.MAX_INPUTS) {
            return false;
        }

        Inventory partial = createRecipeInventory();
        ItemStack existing = partial.getItem(slot);
        if (existing.isEmpty()) {
            partial.setItem(slot, stack.copy());
        } else if (!ItemStack.isSame(existing, stack) || !ItemStack.tagMatches(existing, stack)) {
            return false;
        }

        for (AssemblyRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE)) {
            if (recipe.canMatchPartial(partial)) {
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
        return new TranslationTextComponent("container.justguithings.industrial_assembler");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IndustrialAssemblerContainer(windowId, playerInventory, this);
    }
}
