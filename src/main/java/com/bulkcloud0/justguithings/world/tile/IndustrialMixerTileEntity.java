package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.MachineSidedItemHandler;
import com.bulkcloud0.justguithings.machine.SidedEnergyInputHandler;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.IndustrialMixerContainer;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class IndustrialMixerTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 150_000;
    public static final int MAX_RECEIVE = 1_500;
    public static final int DEFAULT_PROCESS_TICKS = 160;
    public static final int DEFAULT_ENERGY_PER_TICK = 45;

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

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 0) {
                return canAcceptPrimary(stack);
            }
            if (slot == 1) {
                return canAcceptSecondary(stack);
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> sidedItemCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities = new EnumMap<>(Direction.class);

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

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);

    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private ResourceLocation activeRecipeId;

    public IndustrialMixerTileEntity() {
        super(ModTileEntities.INDUSTRIAL_MIXER.get());
        sideModes.put(Direction.UP, MachineSideMode.INPUT);
        sideModes.put(Direction.DOWN, MachineSideMode.OUTPUT);
        sideModes.put(Direction.NORTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.SOUTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.WEST, MachineSideMode.ENERGY);
        sideModes.put(Direction.EAST, MachineSideMode.ENERGY);
        initializeSidedCapabilities();
    }

    private void initializeSidedCapabilities() {
        for (Direction direction : Direction.values()) {
            final Direction side = direction;
            sidedItemCapabilities.put(side, LazyOptional.of(() -> new MachineSidedItemHandler(
                    inventory, 0, 2, 2, 1, () -> getSideMode(side))));
            sidedEnergyCapabilities.put(side, LazyOptional.of(() -> new SidedEnergyInputHandler(
                    energyStorage, () -> getSideMode(side) == MachineSideMode.ENERGY)));
        }
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

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IIntArray getDataAccess() {
        return dataAccess;
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
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        energyStorage.setEnergy(nbt.getInt("Energy"));
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

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    sideModes.put(direction, MachineSideMode.fromOrdinal(config.getInt(key)));
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
        if (activeRecipeId != null && progress > 0) {
            nbt.putString("ActiveRecipe", activeRecipeId.toString());
        }
        CompoundNBT config = new CompoundNBT();
        for (Direction direction : Direction.values()) {
            config.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", config);
        return nbt;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (side == null) {
                return energyCapability.cast();
            }
            if (getSideMode(side) == MachineSideMode.ENERGY) {
                LazyOptional<IEnergyStorage> sided = sidedEnergyCapabilities.get(side);
                return sided == null ? LazyOptional.empty() : sided.cast();
            }
            return LazyOptional.empty();
        }
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (side == null) {
                return itemCapability.cast();
            }
            MachineSideMode mode = getSideMode(side);
            if (mode == MachineSideMode.INPUT || mode == MachineSideMode.OUTPUT) {
                LazyOptional<IItemHandler> sided = sidedItemCapabilities.get(side);
                return sided == null ? LazyOptional.empty() : sided.cast();
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
        for (LazyOptional<IItemHandler> capability : sidedItemCapabilities.values()) {
            capability.invalidate();
        }
        for (LazyOptional<IEnergyStorage> capability : sidedEnergyCapabilities.values()) {
            capability.invalidate();
        }
    }
}
