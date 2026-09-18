package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.recipe.PressingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.StampingPressContainer;
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

public class StampingPressTileEntity extends BaseProcessingMachineTileEntity<PressingRecipe> {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TICKS = 120;
    public static final int DEFAULT_ENERGY_PER_TICK = 30;
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

    public StampingPressTileEntity() {
        super(ModTileEntities.STAMPING_PRESS.get(), CAPACITY, MAX_RECEIVE, 4, 0, 1, 1, 1,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptInput(stack);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        switch (slot) {
            case 2: return MachineModuleTypes.SPEED;
            case 3: return MachineModuleTypes.EFFICIENCY;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected Optional<PressingRecipe> findCurrentRecipe() {
        return findRecipe(inventory.getStackInSlot(0));
    }

    @Override
    protected boolean canProcessRecipe(PressingRecipe recipe) {
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
    protected int getEffectiveProcessingTime(PressingRecipe recipe) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        return Math.max(20, (recipe.getProcessingTime() * 100 + speedMultiplier - 1) / speedMultiplier);
    }

    @Override
    protected int getEffectiveEnergyPerTick(PressingRecipe recipe) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        int efficiencyMultiplier = Math.max(20, 100 - 20 * getEfficiencyUpgradeCount());
        long scaled = (long) recipe.getEnergyPerTick() * speedMultiplier * efficiencyMultiplier;
        return Math.max(1, (int) ((scaled + 9_999L) / 10_000L));
    }

    @Override
    protected void processRecipe(PressingRecipe recipe) {
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

    private Optional<PressingRecipe> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.PRESSING_TYPE, new Inventory(input.copy()), level);
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public boolean canAcceptInput(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.stamping_press");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new StampingPressContainer(windowId, playerInventory, this);
    }
}
