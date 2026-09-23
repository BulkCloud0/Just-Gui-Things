package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.recipe.SeparatingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialSeparatorContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IndustrialSeparatorTileEntity extends BaseProcessingMachineTileEntity<SeparatingRecipe> {
    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int SECOND_OUTPUT_SLOT = 2;
    public static final int THIRD_OUTPUT_SLOT = 3;
    public static final int SPEED_MODULE_SLOT = 4;
    public static final int EFFICIENCY_MODULE_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;
    public static final int CAPACITY = 80_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;
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
            if (index <= 4) {
                setProcessingData(index, value);
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public IndustrialSeparatorTileEntity() {
        super(ModTileEntities.INDUSTRIAL_SEPARATOR.get(), CAPACITY, MAX_RECEIVE,
                INVENTORY_SIZE, INPUT_SLOT, 1, FIRST_OUTPUT_SLOT, 3,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
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
    protected Optional<SeparatingRecipe> findCurrentRecipe() {
        return findProcessableRecipe(inventory.getStackInSlot(INPUT_SLOT));
    }

    private Optional<SeparatingRecipe> findProcessableRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }

        SeparatingRecipe best = null;
        for (SeparatingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SEPARATING_TYPE)) {
            if (!recipe.getInput().test(input) || input.getCount() < recipe.getInputCount()) {
                continue;
            }
            if (best == null || compareRecipes(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    private int compareRecipes(SeparatingRecipe left, SeparatingRecipe right) {
        int specificity = RecipeSelectionHelper.compareIngredients(left.getInput(), right.getInput());
        if (specificity != 0) {
            return specificity;
        }

        int countSpecificity = Integer.compare(right.getInputCount(), left.getInputCount());
        if (countSpecificity != 0) {
            return countSpecificity;
        }
        return RecipeSelectionHelper.compareIds(left.getId(), right.getId());
    }

    @Override
    protected boolean canProcessRecipe(SeparatingRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || input.getCount() < recipe.getInputCount()) {
            return false;
        }

        List<ItemStack> outputs = resolveOutputs(recipe, input);
        if (outputs.size() != recipe.getOutputCount()) {
            return false;
        }

        for (int index = 0; index < outputs.size(); index++) {
            if (!canFit(FIRST_OUTPUT_SLOT + index, outputs.get(index))) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> resolveOutputs(SeparatingRecipe recipe, ItemStack input) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int index = 0; index < recipe.getOutputCount(); index++) {
            ItemStack output = recipe.getOutputForInput(index, input);
            if (output.isEmpty()) {
                return new ArrayList<>();
            }
            outputs.add(output);
        }
        return outputs;
    }

    private boolean canFit(int slot, ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            return result.getCount() <= Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize());
        }
        return ItemStack.isSame(output, result)
                && ItemStack.tagMatches(output, result)
                && output.getCount() + result.getCount() <= Math.min(inventory.getSlotLimit(slot), output.getMaxStackSize());
    }

    @Override
    protected int getEffectiveProcessingTime(SeparatingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(), getSpeedUpgradeCount(), getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(SeparatingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(), getSpeedUpgradeCount(), getEfficiencyUpgradeCount(), 1);
    }

    @Override
    protected void processRecipe(SeparatingRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT).copy();
        if (input.isEmpty() || input.getCount() < recipe.getInputCount()) {
            return;
        }

        List<ItemStack> outputs = resolveOutputs(recipe, input);
        if (outputs.size() != recipe.getOutputCount()) {
            return;
        }
        for (int index = 0; index < outputs.size(); index++) {
            if (!canFit(FIRST_OUTPUT_SLOT + index, outputs.get(index))) {
                return;
            }
        }

        inventory.extractItem(INPUT_SLOT, recipe.getInputCount(), false);
        for (int index = 0; index < outputs.size(); index++) {
            insertOutput(FIRST_OUTPUT_SLOT + index, outputs.get(index));
        }
    }

    private void insertOutput(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, result.copy());
            return;
        }
        ItemStack combined = current.copy();
        combined.grow(result.getCount());
        inventory.setStackInSlot(slot, combined);
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (SeparatingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SEPARATING_TYPE)) {
            if (recipe.getInput().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.industrial_separator");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IndustrialSeparatorContainer(windowId, playerInventory, this);
    }
}
