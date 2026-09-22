package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CrusherContainer;
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

public class CrusherTileEntity extends BaseProcessingMachineTileEntity<CrusherRecipe> {
    public static final int CAPACITY = 100_000;
    public static final int BUFFER_CAPACITY_PER_MODULE = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_ENERGY_PER_TICK = 20;
    public static final int DEFAULT_PROCESS_TICKS = 100;
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
                case 7: return getBufferUpgradeCount();
                case 8: return getBatchUpgradeCount();
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
            return 9;
        }
    };

    private int activeBatchSize = 1;

    public CrusherTileEntity() {
        super(ModTileEntities.CRUSHER.get(), CAPACITY, MAX_RECEIVE, 6, 0, 1, 1, 1,
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
            case 4: return MachineModuleTypes.BUFFER;
            case 5: return MachineModuleTypes.BATCH;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected void onInventoryChanged(int slot) {
        if (slot == 4) {
            energyStorage.setCapacity(getEnergyCapacity());
        }
    }

    @Override
    protected Optional<CrusherRecipe> findCurrentRecipe() {
        return findRecipe(inventory.getStackInSlot(0));
    }

    @Override
    protected void onRecipeActivated(CrusherRecipe recipe) {
        activeBatchSize = resolveBatchSize(recipe);
    }

    @Override
    protected boolean canProcessRecipe(CrusherRecipe recipe) {
        return activeBatchSize > 0 && canProcess(recipe, activeBatchSize);
    }

    @Override
    protected int getEffectiveProcessingTime(CrusherRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                recipe.getProcessingTime(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(CrusherRecipe recipe) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                recipe.getEnergyPerTick(),
                getSpeedUpgradeCount(),
                getEfficiencyUpgradeCount(),
                activeBatchSize);
    }

    @Override
    protected void processRecipe(CrusherRecipe recipe) {
        processItem(recipe, activeBatchSize);
    }

    @Override
    protected void onProcessingReset() {
        activeBatchSize = 1;
    }

    @Override
    protected void onRecipeCompleted(CrusherRecipe recipe) {
        activeBatchSize = 1;
    }

    private Optional<CrusherRecipe> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }

        Inventory recipeInventory = new Inventory(input.copy());
        return level.getRecipeManager().getRecipeFor(ModRecipes.CRUSHING_TYPE, recipeInventory, level);
    }

    private int resolveBatchSize(CrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack result = recipe.assemble(new Inventory(input.copy()));
        if (input.isEmpty() || result.isEmpty() || result.getCount() <= 0) {
            return 0;
        }

        ItemStack output = inventory.getStackInSlot(1);
        int availableOutput;
        if (output.isEmpty()) {
            availableOutput = result.getMaxStackSize();
        } else {
            if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
                return 0;
            }
            availableOutput = output.getMaxStackSize() - output.getCount();
        }

        int byOutput = availableOutput / result.getCount();
        int desired = 1 + getBatchUpgradeCount();
        return Math.max(0, Math.min(desired, Math.min(input.getCount(), byOutput)));
    }

    private boolean canProcess(CrusherRecipe recipe, int batchSize) {
        if (batchSize <= 0 || inventory.getStackInSlot(0).getCount() < batchSize) {
            return false;
        }

        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty()) {
            return false;
        }

        int producedCount = result.getCount() * batchSize;
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            return producedCount <= result.getMaxStackSize();
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + producedCount <= output.getMaxStackSize();
    }

    private void processItem(CrusherRecipe recipe, int batchSize) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty() || batchSize <= 0) {
            return;
        }

        ItemStack produced = result.copy();
        produced.setCount(result.getCount() * batchSize);
        inventory.extractItem(0, batchSize, false);

        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, produced);
        } else {
            ItemStack combined = output.copy();
            combined.grow(produced.getCount());
            inventory.setStackInSlot(1, combined);
        }
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public int getBufferUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.BUFFER));
    }

    public int getBatchUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.BATCH));
    }

    @Override
    public int getEnergyCapacity() {
        return CAPACITY + BUFFER_CAPACITY_PER_MODULE * getBufferUpgradeCount();
    }

    public boolean canAcceptInput(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void loadAdditionalProcessingData(CompoundNBT nbt) {
        activeBatchSize = hasActiveRecipe() ? Math.max(1, nbt.getInt("ActiveBatchSize")) : 1;
    }

    @Override
    protected void saveAdditionalProcessingData(CompoundNBT nbt) {
        if (hasActiveRecipe() && progress > 0) {
            nbt.putInt("ActiveBatchSize", Math.max(1, activeBatchSize));
        }
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.crusher");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CrusherContainer(windowId, playerInventory, this);
    }
}
