package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
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
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public class IndustrialMixerTileEntity extends BaseProcessingMachineTileEntity<MixingRecipe> {
    public static final int CAPACITY = 150_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;

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

    public IndustrialMixerTileEntity() {
        super(ModTileEntities.INDUSTRIAL_MIXER.get(), CAPACITY, MAX_RECEIVE, 4, 0, 3, 3, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
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

    @Override
    protected Optional<MixingRecipe> findCurrentRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty() || inventory.getStackInSlot(1).isEmpty()) {
            return Optional.empty();
        }

        Inventory recipeInventory = new Inventory(
                inventory.getStackInSlot(0).copy(),
                inventory.getStackInSlot(1).copy(),
                inventory.getStackInSlot(2).copy());
        return level.getRecipeManager().getRecipeFor(ModRecipes.MIXING_TYPE, recipeInventory, level);
    }

    @Override
    protected boolean canProcessRecipe(MixingRecipe recipe) {
        if (inventory.getStackInSlot(0).getCount() < recipe.getPrimaryCount()
                || inventory.getStackInSlot(1).getCount() < recipe.getSecondaryCount()
                || (recipe.hasTertiary()
                && inventory.getStackInSlot(2).getCount() < recipe.getTertiaryCount())) {
            return false;
        }

        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void processRecipe(MixingRecipe recipe) {
        ItemStack result = recipe.getResultForPrimary(inventory.getStackInSlot(0));
        if (result.isEmpty()) {
            return;
        }

        inventory.extractItem(0, recipe.getPrimaryCount(), false);
        inventory.extractItem(1, recipe.getSecondaryCount(), false);

        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) {
            inventory.setStackInSlot(2, result);
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(2, combined);
        }
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
}
