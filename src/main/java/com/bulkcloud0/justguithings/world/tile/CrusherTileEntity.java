package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.registry.ModItems;
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
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public class CrusherTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 100_000;
    public static final int BUFFER_CAPACITY_PER_MODULE = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_ENERGY_PER_TICK = 20;
    public static final int DEFAULT_PROCESS_TICKS = 100;
    public static final int MAX_MODULES_PER_TYPE = 4;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return progress;
                case 1:
                    return energyStorage.getEnergyStored() & 0xFFFF;
                case 2:
                    return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 3:
                    return currentProcessTicks;
                case 4:
                    return currentEnergyPerTick;
                case 5:
                    return getSpeedUpgradeCount();
                case 6:
                    return getEfficiencyUpgradeCount();
                case 7:
                    return getBufferUpgradeCount();
                case 8:
                    return getBatchUpgradeCount();
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    progress = value;
                    break;
                case 1:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0xFFFF0000) | (value & 0xFFFF));
                    break;
                case 2:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                    break;
                case 3:
                    currentProcessTicks = value;
                    break;
                case 4:
                    currentEnergyPerTick = value;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private int activeBatchSize = 1;
    private ResourceLocation activeRecipeId;

    public CrusherTileEntity() {
        super(ModTileEntities.CRUSHER.get(), CAPACITY, MAX_RECEIVE, 6, 0, 1, 1, 1);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case 0:
                return canAcceptInput(stack);
            case 2:
                return stack.getItem() == ModItems.SPEED_UPGRADE.get();
            case 3:
                return stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get();
            case 4:
                return stack.getItem() == ModItems.BUFFER_UPGRADE.get();
            case 5:
                return stack.getItem() == ModItems.BATCH_UPGRADE.get();
            default:
                return false;
        }
    }

    @Override
    protected int getMachineSlotLimit(int slot) {
        return slot >= 2 && slot <= 5 ? MAX_MODULES_PER_TYPE : super.getMachineSlotLimit(slot);
    }

    @Override
    protected void onInventoryChanged(int slot) {
        if (slot == 4) {
            energyStorage.setCapacity(getEnergyCapacity());
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<CrusherRecipe> recipeOptional = findRecipe(inventory.getStackInSlot(0));
        if (!recipeOptional.isPresent()) {
            resetProcessing();
            return;
        }

        CrusherRecipe recipe = recipeOptional.get();
        if (activeRecipeId == null || !activeRecipeId.equals(recipe.getId())) {
            progress = 0;
            activeRecipeId = recipe.getId();
            activeBatchSize = resolveBatchSize(recipe);
        }

        if (activeBatchSize <= 0 || !canProcess(recipe, activeBatchSize)) {
            resetProcessing();
            return;
        }

        currentProcessTicks = getEffectiveProcessingTime(recipe);
        currentEnergyPerTick = getEffectiveEnergyPerTick(recipe, activeBatchSize);

        if (energyStorage.getEnergyStored() < currentEnergyPerTick) {
            return;
        }

        energyStorage.consumeEnergy(currentEnergyPerTick);
        progress++;

        if (progress >= currentProcessTicks) {
            processItem(recipe, activeBatchSize);
            progress = 0;
            activeRecipeId = null;
            activeBatchSize = 1;
        }

        setChanged();
    }

    private void resetProcessing() {
        if (progress != 0 || activeRecipeId != null || activeBatchSize != 1) {
            progress = 0;
            activeRecipeId = null;
            activeBatchSize = 1;
            setChanged();
        }
        currentProcessTicks = DEFAULT_PROCESS_TICKS;
        currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
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
        ItemStack result = recipe.getResultItem();
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

        ItemStack result = recipe.getResultItem();
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

    private int getEffectiveProcessingTime(CrusherRecipe recipe) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        return Math.max(20, (recipe.getProcessingTime() * 100 + speedMultiplier - 1) / speedMultiplier);
    }

    private int getEffectiveEnergyPerTick(CrusherRecipe recipe, int batchSize) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        int efficiencyMultiplier = Math.max(20, 100 - 20 * getEfficiencyUpgradeCount());
        long scaled = (long) recipe.getEnergyPerTick() * speedMultiplier * efficiencyMultiplier * Math.max(1, batchSize);
        return Math.max(1, (int) ((scaled + 9_999L) / 10_000L));
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, inventory.getStackInSlot(2).getCount());
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, inventory.getStackInSlot(3).getCount());
    }

    public int getBufferUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, inventory.getStackInSlot(4).getCount());
    }

    public int getBatchUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, inventory.getStackInSlot(5).getCount());
    }

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
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.crusher");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CrusherContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        progress = Math.max(0, nbt.getInt("Progress"));
        activeRecipeId = null;
        activeBatchSize = 1;

        String activeRecipe = nbt.getString("ActiveRecipe");
        if (!activeRecipe.isEmpty()) {
            try {
                activeRecipeId = new ResourceLocation(activeRecipe);
                activeBatchSize = Math.max(1, nbt.getInt("ActiveBatchSize"));
            } catch (RuntimeException ignored) {
                activeRecipeId = null;
                progress = 0;
                activeBatchSize = 1;
            }
        }

        if (activeRecipeId == null) {
            progress = 0;
            activeBatchSize = 1;
        }

    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Progress", progress);
        if (activeRecipeId != null && progress > 0) {
            nbt.putString("ActiveRecipe", activeRecipeId.toString());
            nbt.putInt("ActiveBatchSize", Math.max(1, activeBatchSize));
        }

        return nbt;
    }

}
