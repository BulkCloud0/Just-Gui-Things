package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.recipe.SawingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialSawmillContainer;
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

public class IndustrialSawmillTileEntity extends BaseProcessingMachineTileEntity<SawingRecipe> {
    public static final int INPUT_SLOT = 0;
    public static final int PRIMARY_OUTPUT_SLOT = 1;
    public static final int SECONDARY_OUTPUT_SLOT = 2;
    public static final int SPEED_MODULE_SLOT = 3;
    public static final int EFFICIENCY_MODULE_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;
    public static final int CAPACITY = 80_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TICKS = 80;
    public static final int DEFAULT_ENERGY_PER_TICK = 25;
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

    public IndustrialSawmillTileEntity() {
        super(ModTileEntities.INDUSTRIAL_SAWMILL.get(), CAPACITY, MAX_RECEIVE,
                INVENTORY_SIZE, INPUT_SLOT, 1, PRIMARY_OUTPUT_SLOT, 2,
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
    protected Optional<SawingRecipe> findCurrentRecipe() {
        return findRecipe(inventory.getStackInSlot(INPUT_SLOT));
    }

    private Optional<SawingRecipe> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        SawingRecipe best = null;
        for (SawingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SAWING_TYPE)) {
            if (!recipe.getInput().test(input)) {
                continue;
            }
            if (best == null || RecipeSelectionHelper.compareSingleInput(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    protected boolean canProcessRecipe(SawingRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        ItemStack primary = recipe.getResultForInput(input);
        if (!canFit(PRIMARY_OUTPUT_SLOT, primary)) {
            return false;
        }
        ItemStack secondary = recipe.getSecondaryResultForInput(input);
        return secondary.isEmpty() || canFit(SECONDARY_OUTPUT_SLOT, secondary);
    }

    private boolean canFit(int slot, ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        return ItemStack.isSame(output, result)
                && ItemStack.tagMatches(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected int getEffectiveProcessingTime(SawingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(), getSpeedUpgradeCount(), getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(SawingRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(), getSpeedUpgradeCount(), getEfficiencyUpgradeCount(), 1);
    }

    @Override
    protected void processRecipe(SawingRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT).copy();
        if (input.isEmpty()) {
            return;
        }
        ItemStack primary = recipe.getResultForInput(input);
        ItemStack secondary = recipe.getSecondaryResultForInput(input);
        if (primary.isEmpty() || !canFit(PRIMARY_OUTPUT_SLOT, primary)
                || (!secondary.isEmpty() && !canFit(SECONDARY_OUTPUT_SLOT, secondary))) {
            return;
        }
        inventory.extractItem(INPUT_SLOT, 1, false);
        insertOutput(PRIMARY_OUTPUT_SLOT, primary);
        if (!secondary.isEmpty()) {
            insertOutput(SECONDARY_OUTPUT_SLOT, secondary);
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
        return findRecipe(stack).isPresent();
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
        return new TranslationTextComponent("container.justguithings.industrial_sawmill");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IndustrialSawmillContainer(windowId, playerInventory, this);
    }
}
