package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.ExtrudingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.PrecisionExtruderContainer;
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

public class PrecisionExtruderTileEntity extends BaseProcessingMachineTileEntity<ExtrudingRecipe> {
    public static final int CAPACITY = 140_000;
    public static final int MAX_RECEIVE = 1_600;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 55;

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

    public PrecisionExtruderTileEntity() {
        super(ModTileEntities.PRECISION_EXTRUDER.get(), CAPACITY, MAX_RECEIVE,
                2, 0, 1, 1, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptInput(stack);
    }

    @Override
    protected Optional<ExtrudingRecipe> findCurrentRecipe() {
        return findRecipe(inventory.getStackInSlot(0));
    }

    @Override
    protected boolean canProcessRecipe(ExtrudingRecipe recipe) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
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
    protected void processRecipe(ExtrudingRecipe recipe) {
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

    private Optional<ExtrudingRecipe> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.EXTRUDING_TYPE, new Inventory(input.copy()), level);
    }

    public boolean canAcceptInput(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.precision_extruder");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PrecisionExtruderContainer(windowId, playerInventory, this);
    }
}
