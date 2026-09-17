package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.MachineTier;
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
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class CrusherTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_ENERGY_PER_TICK = 20;
    public static final int DEFAULT_PROCESS_TICKS = 100;
    public static final MachineTier TIER = MachineTier.MK_I;

    private final ModEnergyStorage energyStorage = new ModEnergyStorage(CAPACITY, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            switch (slot) {
                case 0:
                    return canAcceptInput(stack);
                case 2:
                    return stack.getItem() == ModItems.SPEED_UPGRADE.get();
                case 3:
                    return stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get();
                default:
                    return false;
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 2 || slot == 3) {
                return TIER.getMaxModulesPerType();
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final RangedWrapper inputHandler = new RangedWrapper(inventory, 0, 1);
    private final RangedWrapper outputHandler = new RangedWrapper(inventory, 1, 2);
    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);

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
            return 7;
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private LazyOptional<IItemHandler> inputCapability = LazyOptional.of(() -> inputHandler);
    private LazyOptional<IItemHandler> outputCapability = LazyOptional.of(() -> outputHandler);
    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private ResourceLocation activeRecipeId;

    public CrusherTileEntity() {
        super(ModTileEntities.CRUSHER.get());
        initializeDefaultSides();
    }

    private void initializeDefaultSides() {
        sideModes.put(Direction.UP, MachineSideMode.INPUT);
        sideModes.put(Direction.DOWN, MachineSideMode.OUTPUT);
        sideModes.put(Direction.NORTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.SOUTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.WEST, MachineSideMode.ENERGY);
        sideModes.put(Direction.EAST, MachineSideMode.ENERGY);
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
        }

        currentProcessTicks = getEffectiveProcessingTime(recipe);
        currentEnergyPerTick = getEffectiveEnergyPerTick(recipe);

        if (!canProcess(recipe) || energyStorage.getEnergyStored() < currentEnergyPerTick) {
            return;
        }

        energyStorage.consumeEnergy(currentEnergyPerTick);
        progress++;

        if (progress >= currentProcessTicks) {
            processItem(recipe);
            progress = 0;
            activeRecipeId = null;
        }

        setChanged();
    }

    private void resetProcessing() {
        if (progress != 0 || activeRecipeId != null) {
            progress = 0;
            activeRecipeId = null;
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

    private boolean canProcess(CrusherRecipe recipe) {
        ItemStack result = recipe.getResultItem();
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

    private void processItem(CrusherRecipe recipe) {
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

    private int getEffectiveProcessingTime(CrusherRecipe recipe) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        return Math.max(20, (recipe.getProcessingTime() * 100 + speedMultiplier - 1) / speedMultiplier);
    }

    private int getEffectiveEnergyPerTick(CrusherRecipe recipe) {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        int efficiencyMultiplier = Math.max(20, 100 - 20 * getEfficiencyUpgradeCount());
        long scaled = (long) recipe.getEnergyPerTick() * speedMultiplier * efficiencyMultiplier;
        return Math.max(1, (int) ((scaled + 9_999L) / 10_000L));
    }

    public int getSpeedUpgradeCount() {
        return Math.min(TIER.getMaxModulesPerType(), inventory.getStackInSlot(2).getCount());
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(TIER.getMaxModulesPerType(), inventory.getStackInSlot(3).getCount());
    }

    public MachineSideMode getSideMode(Direction side) {
        return sideModes.getOrDefault(side, MachineSideMode.DISABLED);
    }

    public MachineSideMode cycleSideMode(Direction side) {
        MachineSideMode mode = getSideMode(side).next();
        sideModes.put(side, mode);
        setChanged();
        return mode;
    }

    public boolean canAcceptInput(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    public ItemStackHandler getInventory() {
        return inventory;
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
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        energyStorage.setEnergy(nbt.getInt("Energy"));
        progress = nbt.getInt("Progress");
        activeRecipeId = null;

        if (nbt.contains("SideConfig")) {
            CompoundNBT sideConfig = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (sideConfig.contains(key)) {
                    sideModes.put(direction, MachineSideMode.fromOrdinal(sideConfig.getInt(key)));
                }
            }
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Energy", energyStorage.getEnergyStored());
        nbt.putInt("Progress", progress);

        CompoundNBT sideConfig = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            sideConfig.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", sideConfig);
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (side == null || getSideMode(side) == MachineSideMode.ENERGY) {
                return energyCapability.cast();
            }
            return LazyOptional.empty();
        }
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (side == null) {
                return itemCapability.cast();
            }
            MachineSideMode mode = getSideMode(side);
            if (mode == MachineSideMode.INPUT) {
                return inputCapability.cast();
            }
            if (mode == MachineSideMode.OUTPUT) {
                return outputCapability.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCapability.invalidate();
        itemCapability.invalidate();
        inputCapability.invalidate();
        outputCapability.invalidate();
    }
}
