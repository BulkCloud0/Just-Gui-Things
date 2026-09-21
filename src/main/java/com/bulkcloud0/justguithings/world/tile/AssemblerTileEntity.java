package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.AssemblyRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.AssemblerContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public class AssemblerTileEntity extends BaseProcessingMachineTileEntity<AssemblyRecipe> {
    public static final int CAPACITY = 180_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 60;
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = 4;

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

    public AssemblerTileEntity() {
        super(ModTileEntities.ASSEMBLER.get(), CAPACITY, MAX_RECEIVE,
                INPUT_SLOTS + 1, 0, INPUT_SLOTS, OUTPUT_SLOT, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < INPUT_SLOTS && canAcceptInput(slot, stack);
    }

    private Inventory createRecipeInventory() {
        return new Inventory(
                inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy(),
                inventory.getStackInSlot(3).copy());
    }

    @Override
    protected Optional<AssemblyRecipe> findCurrentRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                ModRecipes.ASSEMBLING_TYPE, createRecipeInventory(), level);
    }

    @Override
    protected boolean canProcessRecipe(AssemblyRecipe recipe) {
        if (level == null || !recipe.matches(createRecipeInventory(), level)) {
            return false;
        }

        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
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
    protected void processRecipe(AssemblyRecipe recipe) {
        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return;
        }

        for (int slot = 0; slot < recipe.getInputCount(); slot++) {
            inventory.extractItem(slot, recipe.getRequiredCount(slot), false);
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(OUTPUT_SLOT, combined);
        }
    }

    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (level == null || stack.isEmpty() || slot < 0 || slot >= INPUT_SLOTS) {
            return false;
        }
        for (AssemblyRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLING_TYPE)) {
            if (slot < recipe.getInputCount() && recipe.getInputIngredient(slot).test(stack)) {
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
        return new TranslationTextComponent("container.justguithings.assembler");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AssemblerContainer(windowId, playerInventory, this);
    }
}
