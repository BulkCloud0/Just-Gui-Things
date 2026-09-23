package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CrusherContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Optional;

public class CrusherTileEntity extends BaseProcessingMachineTileEntity<CrusherRecipe> {
    private static final int INVENTORY_VERSION = 1;

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
        super(ModTileEntities.CRUSHER.get(), CAPACITY, MAX_RECEIVE, 7, 0, 1, 1, 2,
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
            case 3: return MachineModuleTypes.SPEED;
            case 4: return MachineModuleTypes.EFFICIENCY;
            case 5: return MachineModuleTypes.BUFFER;
            case 6: return MachineModuleTypes.BATCH;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected void onInventoryChanged(int slot) {
        if (slot == 5) {
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

        CrusherRecipe best = null;
        for (CrusherRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHING_TYPE)) {
            if (!recipe.getInput().test(input)) {
                continue;
            }
            if (best == null || RecipeSelectionHelper.compareSingleInput(recipe, best) < 0) {
                best = recipe;
            }
        }
        return Optional.ofNullable(best);
    }

    private int resolveBatchSize(CrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            return 0;
        }

        ItemStack primary = recipe.getResultForInput(input);
        if (primary.isEmpty() || primary.getCount() <= 0) {
            return 0;
        }

        int desired = Math.min(1 + getBatchUpgradeCount(), input.getCount());
        desired = Math.min(desired, getAvailableOperationsForOutput(1, primary));

        ItemStack secondary = recipe.getSecondaryResultForInput(input);
        if (!secondary.isEmpty()) {
            desired = Math.min(desired, getAvailableOperationsForOutput(2, secondary));
        }

        return Math.max(0, desired);
    }

    private int getAvailableOperationsForOutput(int slot, ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            return 0;
        }

        ItemStack output = inventory.getStackInSlot(slot);
        int available;
        if (output.isEmpty()) {
            available = result.getMaxStackSize();
        } else {
            if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
                return 0;
            }
            available = output.getMaxStackSize() - output.getCount();
        }
        return Math.max(0, available / result.getCount());
    }

    private boolean canProcess(CrusherRecipe recipe, int batchSize) {
        ItemStack input = inventory.getStackInSlot(0);
        if (batchSize <= 0 || input.getCount() < batchSize) {
            return false;
        }

        ItemStack primary = recipe.getResultForInput(input);
        if (!canFitOutput(1, primary, batchSize)) {
            return false;
        }

        ItemStack secondary = recipe.getSecondaryResultForInput(input);
        return secondary.isEmpty() || canFitOutput(2, secondary, batchSize);
    }

    private boolean canFitOutput(int slot, ItemStack result, int batchSize) {
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        int producedCount = result.getCount() * batchSize;
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            return producedCount <= result.getMaxStackSize();
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + producedCount <= output.getMaxStackSize();
    }

    private void processItem(CrusherRecipe recipe, int batchSize) {
        ItemStack input = inventory.getStackInSlot(0).copy();
        if (input.isEmpty() || batchSize <= 0) {
            return;
        }

        ItemStack primary = recipe.getResultForInput(input);
        ItemStack secondary = recipe.getSecondaryResultForInput(input);
        if (primary.isEmpty()) {
            return;
        }

        inventory.extractItem(0, batchSize, false);
        insertOutput(1, primary, batchSize);
        if (!secondary.isEmpty()) {
            insertOutput(2, secondary, batchSize);
        }
    }

    private void insertOutput(int slot, ItemStack result, int batchSize) {
        ItemStack produced = result.copy();
        produced.setCount(result.getCount() * batchSize);

        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            inventory.setStackInSlot(slot, produced);
            return;
        }

        ItemStack combined = output.copy();
        combined.grow(produced.getCount());
        inventory.setStackInSlot(slot, combined);
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
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, migrateInventory(nbt));
    }

    private CompoundNBT migrateInventory(CompoundNBT nbt) {
        if (nbt.getInt("CrusherInventoryVersion") >= INVENTORY_VERSION) {
            return nbt;
        }

        CompoundNBT migrated = nbt.copy();
        CompoundNBT inventoryNbt = migrated.getCompound("Inventory");
        if (inventoryNbt.getInt("Size") <= 6) {
            ListNBT items = inventoryNbt.getList("Items", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < items.size(); index++) {
                CompoundNBT item = items.getCompound(index);
                int slot = item.getInt("Slot");
                if (slot >= 2 && slot <= 5) {
                    item.putInt("Slot", slot + 1);
                }
            }
            inventoryNbt.putInt("Size", 7);
            migrated.put("Inventory", inventoryNbt);
        }
        migrated.putInt("CrusherInventoryVersion", INVENTORY_VERSION);
        return migrated;
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        CompoundNBT saved = super.save(nbt);
        saved.putInt("CrusherInventoryVersion", INVENTORY_VERSION);
        return saved;
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
