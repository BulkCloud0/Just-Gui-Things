package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.recipe.RecipeSelectionHelper;
import com.bulkcloud0.justguithings.recipe.SolidFuelRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class CoalGeneratorTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int LEGACY_COAL_BURN_TICKS = 1_600;
    public static final int LEGACY_COAL_ENERGY = LEGACY_COAL_BURN_TICKS * GENERATION_PER_TICK;

    private static final MachineSideMode[] ALLOWED_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.ENERGY_OUTPUT,
            MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT
    };

    private int batchEnergyRemaining;
    private int batchEnergyTotal;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return batchEnergyRemaining & 0xFFFF;
                case 1: return (batchEnergyRemaining >>> 16) & 0xFFFF;
                case 2: return batchEnergyTotal & 0xFFFF;
                case 3: return (batchEnergyTotal >>> 16) & 0xFFFF;
                case 4: return energyStorage.getEnergyStored() & 0xFFFF;
                case 5: return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    batchEnergyRemaining = (batchEnergyRemaining & 0xFFFF0000) | (value & 0xFFFF);
                    break;
                case 1:
                    batchEnergyRemaining = (batchEnergyRemaining & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                    break;
                case 2:
                    batchEnergyTotal = (batchEnergyTotal & 0xFFFF0000) | (value & 0xFFFF);
                    break;
                case 3:
                    batchEnergyTotal = (batchEnergyTotal & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                    break;
                case 4:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0xFFFF0000) | (value & 0xFFFF));
                    break;
                case 5:
                    energyStorage.setEnergy((energyStorage.getEnergyStored() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public CoalGeneratorTileEntity() {
        super(ModTileEntities.COAL_GENERATOR.get(), CAPACITY, 0, MAX_OUTPUT_PER_TICK,
                1, 0, 1, 1, 0);

        setSideMode(Direction.UP, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.DOWN, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.NORTH, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.SOUTH, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.WEST, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
        setSideMode(Direction.EAST, MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptFuel(stack);
    }

    @Override
    protected MachineSideMode[] getAllowedSideModes() {
        return ALLOWED_SIDE_MODES;
    }

    @Override
    protected MachineSideMode getItemSideMode(Direction side, MachineSideMode mode) {
        if (mode == MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT) {
            return MachineSideMode.INPUT;
        }
        return mode;
    }

    @Override
    protected MachineSideMode normalizeLoadedSideMode(Direction side, MachineSideMode mode, int configVersion) {
        if (configVersion < 5 && mode == MachineSideMode.ENERGY_OUTPUT) {
            return MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT;
        }
        return super.normalizeLoadedSideMode(side, mode, configVersion);
    }

    @Override
    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return false;
    }

    @Override
    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY_OUTPUT
                || mode == MachineSideMode.ITEM_INPUT_ENERGY_OUTPUT;
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;

        if (isOperationEnabled()) {
            if (batchEnergyRemaining <= 0) {
                changed |= tryConsumeFuel();
            }

            if (batchEnergyRemaining > 0) {
                int generated = generateBatchEnergy();
                if (generated > 0) {
                    changed = true;
                }
            }
        }

        if (pushEnergyToNeighborsFairly(MAX_OUTPUT_PER_TICK) > 0) {
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    private int generateBatchEnergy() {
        int amount = Math.min(GENERATION_PER_TICK, batchEnergyRemaining);
        int free = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (amount <= 0 || free < amount) {
            return 0;
        }

        energyStorage.addEnergy(amount);
        batchEnergyRemaining -= amount;
        if (batchEnergyRemaining <= 0) {
            batchEnergyRemaining = 0;
            batchEnergyTotal = 0;
        }
        return amount;
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(0);
        SolidFuelRecipe recipe = findFuelRecipe(fuel);
        if (recipe == null) {
            return false;
        }

        int firstTick = Math.min(GENERATION_PER_TICK, recipe.getEnergy());
        int free = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (free < firstTick) {
            return false;
        }

        inventory.extractItem(0, 1, false);
        batchEnergyRemaining = recipe.getEnergy();
        batchEnergyTotal = recipe.getEnergy();
        return true;
    }

    @Nullable
    private SolidFuelRecipe findFuelRecipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return null;
        }

        SolidFuelRecipe best = null;
        for (SolidFuelRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SOLID_FUEL_TYPE)) {
            if (!recipe.matchesStack(stack)) {
                continue;
            }
            if (best == null || compareFuelRecipes(recipe, best) < 0) {
                best = recipe;
            }
        }
        return best;
    }

    private int compareFuelRecipes(SolidFuelRecipe left, SolidFuelRecipe right) {
        int specificity = RecipeSelectionHelper.compareIngredients(
                left.getIngredient(), right.getIngredient());
        if (specificity != 0) {
            return specificity;
        }
        return RecipeSelectionHelper.compareIds(left.getId(), right.getId());
    }

    public boolean canAcceptFuel(ItemStack stack) {
        return findFuelRecipe(stack) != null;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    public int getBatchEnergyRemaining() {
        return batchEnergyRemaining;
    }

    public int getBatchEnergyTotal() {
        return batchEnergyTotal;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.coal_generator");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CoalGeneratorContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        if (nbt.contains("BatchEnergy")) {
            batchEnergyRemaining = Math.max(0, nbt.getInt("BatchEnergy"));
            batchEnergyTotal = Math.max(batchEnergyRemaining, nbt.getInt("BatchEnergyTotal"));
        } else {
            int legacyTicks = Math.max(0, nbt.getInt("BurnTicks"));
            batchEnergyRemaining = legacyTicks * GENERATION_PER_TICK;
            batchEnergyTotal = legacyTicks > 0
                    ? Math.max(LEGACY_COAL_ENERGY, batchEnergyRemaining)
                    : 0;
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("BatchEnergy", batchEnergyRemaining);
        nbt.putInt("BatchEnergyTotal", batchEnergyTotal);
        nbt.putInt("BurnTicks", (batchEnergyRemaining + GENERATION_PER_TICK - 1) / GENERATION_PER_TICK);
        return nbt;
    }
}
