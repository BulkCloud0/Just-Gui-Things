package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
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

public class IndustrialMixerTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 150_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return progress;
                case 1: return energyStorage.getEnergyStored() & 0xFFFF;
                case 2: return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 3: return currentProcessTicks;
                case 4: return currentEnergyPerTick;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0: progress = value; break;
                case 1: energyStorage.setEnergy((energyStorage.getEnergyStored() & 0xFFFF0000) | (value & 0xFFFF)); break;
                case 2: energyStorage.setEnergy((energyStorage.getEnergyStored() & 0x0000FFFF) | ((value & 0xFFFF) << 16)); break;
                case 3: currentProcessTicks = value; break;
                case 4: currentEnergyPerTick = value; break;
                default: break;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };


    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private ResourceLocation activeRecipeId;

    public IndustrialMixerTileEntity() {
        super(ModTileEntities.INDUSTRIAL_MIXER.get(), CAPACITY, MAX_RECEIVE, 3, 0, 2, 2, 1);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            return canAcceptPrimary(stack);
        }
        if (slot == 1) {
            return canAcceptSecondary(stack);
        }
        return false;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<MixingRecipe> recipeOptional = findRecipe();
        if (!recipeOptional.isPresent()) {
            resetProgress();
            return;
        }

        MixingRecipe recipe = recipeOptional.get();
        if (activeRecipeId == null || !activeRecipeId.equals(recipe.getId())) {
            progress = 0;
            activeRecipeId = recipe.getId();
        }

        currentProcessTicks = recipe.getProcessingTime();
        currentEnergyPerTick = recipe.getEnergyPerTick();

        if (!canProcess(recipe)) {
            resetProgress();
            return;
        }
        if (energyStorage.getEnergyStored() < currentEnergyPerTick) {
            return;
        }

        energyStorage.consumeEnergy(currentEnergyPerTick);
        progress++;
        if (progress >= currentProcessTicks) {
            process(recipe);
            progress = 0;
            activeRecipeId = null;
        }
        setChanged();
    }

    private void resetProgress() {
        if (progress != 0 || activeRecipeId != null) {
            progress = 0;
            activeRecipeId = null;
            setChanged();
        }
        currentProcessTicks = DEFAULT_PROCESS_TICKS;
        currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    }

    private Optional<MixingRecipe> findRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty() || inventory.getStackInSlot(1).isEmpty()) {
            return Optional.empty();
        }
        Inventory recipeInventory = new Inventory(inventory.getStackInSlot(0).copy(), inventory.getStackInSlot(1).copy());
        return level.getRecipeManager().getRecipeFor(ModRecipes.MIXING_TYPE, recipeInventory, level);
    }

    private boolean canProcess(MixingRecipe recipe) {
        ItemStack result = recipe.getResultItem();
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

    private void process(MixingRecipe recipe) {
        ItemStack result = recipe.getResultItem().copy();
        if (result.isEmpty()) {
            return;
        }
        inventory.extractItem(0, 1, false);
        inventory.extractItem(1, 1, false);
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

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        progress = Math.max(0, nbt.getInt("Progress"));
        activeRecipeId = null;

        String activeRecipe = nbt.getString("ActiveRecipe");
        if (!activeRecipe.isEmpty()) {
            try {
                activeRecipeId = new ResourceLocation(activeRecipe);
            } catch (RuntimeException ignored) {
                activeRecipeId = null;
                progress = 0;
            }
        }
        if (activeRecipeId == null) {
            progress = 0;
        }

    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Progress", progress);
        if (activeRecipeId != null && progress > 0) {
            nbt.putString("ActiveRecipe", activeRecipeId.toString());
        }
        return nbt;
    }

}
