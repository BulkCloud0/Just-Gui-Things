package com.bulkcloud0.justguithings.machine;

import com.bulkcloud0.justguithings.recipe.MachineProcessingRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;

public abstract class BaseProcessingMachineTileEntity<R extends MachineProcessingRecipe> extends BaseMachineTileEntity {
    private final int defaultProcessTicks;
    private final int defaultEnergyPerTick;

    protected int progress;
    protected int currentProcessTicks;
    protected int currentEnergyPerTick;

    private ResourceLocation activeRecipeId;

    protected BaseProcessingMachineTileEntity(TileEntityType<?> tileEntityType,
                                              int energyCapacity,
                                              int maxReceive,
                                              int inventorySize,
                                              int inputStart,
                                              int inputCount,
                                              int outputStart,
                                              int outputCount,
                                              int defaultProcessTicks,
                                              int defaultEnergyPerTick) {
        super(tileEntityType, energyCapacity, maxReceive, inventorySize, inputStart, inputCount, outputStart, outputCount);
        this.defaultProcessTicks = defaultProcessTicks;
        this.defaultEnergyPerTick = defaultEnergyPerTick;
        this.currentProcessTicks = defaultProcessTicks;
        this.currentEnergyPerTick = defaultEnergyPerTick;
    }

    protected abstract Optional<R> findCurrentRecipe();

    protected abstract boolean canProcessRecipe(R recipe);

    protected abstract void processRecipe(R recipe);

    protected int getEffectiveProcessingTime(R recipe) {
        return recipe.getProcessingTime();
    }

    protected int getEffectiveEnergyPerTick(R recipe) {
        return recipe.getEnergyPerTick();
    }

    protected void onRecipeActivated(R recipe) {
    }

    protected void onProcessingReset() {
    }

    protected void onRecipeCompleted(R recipe) {
    }

    protected void loadAdditionalProcessingData(CompoundNBT nbt) {
    }

    protected void saveAdditionalProcessingData(CompoundNBT nbt) {
    }

    @Override
    public final void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<R> recipeOptional = findCurrentRecipe();
        if (!recipeOptional.isPresent()) {
            resetProcessing();
            return;
        }

        R recipe = recipeOptional.get();
        ResourceLocation recipeId = recipe.getId();
        if (activeRecipeId == null || !activeRecipeId.equals(recipeId)) {
            progress = 0;
            activeRecipeId = recipeId;
            onRecipeActivated(recipe);
        }

        currentProcessTicks = Math.max(1, getEffectiveProcessingTime(recipe));
        currentEnergyPerTick = Math.max(1, getEffectiveEnergyPerTick(recipe));

        if (!canProcessRecipe(recipe)) {
            resetProcessing();
            return;
        }

        if (energyStorage.getEnergyStored() < currentEnergyPerTick) {
            return;
        }

        energyStorage.consumeEnergy(currentEnergyPerTick);
        progress++;

        if (progress >= currentProcessTicks) {
            processRecipe(recipe);
            progress = 0;
            activeRecipeId = null;
            onRecipeCompleted(recipe);
        }

        setChanged();
    }

    protected final void resetProcessing() {
        boolean changed = progress != 0 || activeRecipeId != null;
        progress = 0;
        activeRecipeId = null;
        currentProcessTicks = defaultProcessTicks;
        currentEnergyPerTick = defaultEnergyPerTick;
        onProcessingReset();

        if (changed) {
            setChanged();
        }
    }

    protected final boolean hasActiveRecipe() {
        return activeRecipeId != null;
    }

    protected final int getProcessingData(int index) {
        switch (index) {
            case 0: return progress;
            case 1: return energyStorage.getEnergyStored() & 0xFFFF;
            case 2: return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
            case 3: return currentProcessTicks;
            case 4: return currentEnergyPerTick;
            default: return 0;
        }
    }

    protected final void setProcessingData(int index, int value) {
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
    public void load(net.minecraft.block.BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        progress = Math.max(0, nbt.getInt("Progress"));
        activeRecipeId = null;

        String activeRecipe = nbt.getString("ActiveRecipe");
        if (!activeRecipe.isEmpty()) {
            try {
                activeRecipeId = new ResourceLocation(activeRecipe);
            } catch (RuntimeException ignored) {
                progress = 0;
            }
        }

        if (activeRecipeId == null) {
            progress = 0;
        }

        loadAdditionalProcessingData(nbt);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Progress", progress);

        if (activeRecipeId != null && progress > 0) {
            nbt.putString("ActiveRecipe", activeRecipeId.toString());
        }

        saveAdditionalProcessingData(nbt);
        return nbt;
    }
}
