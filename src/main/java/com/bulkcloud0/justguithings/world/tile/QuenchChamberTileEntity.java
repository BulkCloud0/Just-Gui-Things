package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.machine.MachineSideMode;
import com.bulkcloud0.justguithings.machine.MachineSidedItemHandler;
import com.bulkcloud0.justguithings.machine.SidedEnergyInputHandler;
import com.bulkcloud0.justguithings.machine.SidedFluidInputHandler;
import com.bulkcloud0.justguithings.recipe.QuenchingRecipe;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.QuenchChamberContainer;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class QuenchChamberTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int CAPACITY = 120_000;
    public static final int MAX_RECEIVE = 1_200;
    public static final int TANK_CAPACITY = 4_000;
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

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return canAcceptFluid(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IFluidHandler fluidInputHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return fluidTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fluidTank.fill(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && canAcceptInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> sidedItemCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedFluidCapabilities = new EnumMap<>(Direction.class);

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return progress;
                case 1: return energyStorage.getEnergyStored() & 0xFFFF;
                case 2: return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 3: return currentProcessTicks;
                case 4: return currentEnergyPerTick;
                case 5: return level != null && level.isClientSide ? syncedFluidAmount : fluidTank.getFluidAmount();
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
                case 5: syncedFluidAmount = value; break;
                default: break;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidInputHandler);

    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private ResourceLocation activeRecipeId;
    private int syncedFluidAmount;

    public QuenchChamberTileEntity() {
        super(ModTileEntities.QUENCH_CHAMBER.get());
        sideModes.put(Direction.UP, MachineSideMode.INPUT);
        sideModes.put(Direction.DOWN, MachineSideMode.OUTPUT);
        sideModes.put(Direction.NORTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.SOUTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.WEST, MachineSideMode.INPUT);
        sideModes.put(Direction.EAST, MachineSideMode.ENERGY);
        initializeSidedCapabilities();
    }

    private void initializeSidedCapabilities() {
        for (Direction direction : Direction.values()) {
            final Direction side = direction;
            sidedItemCapabilities.put(side, LazyOptional.of(() -> new MachineSidedItemHandler(
                    inventory, 0, 1, 1, 1, () -> getSideMode(side))));
            sidedEnergyCapabilities.put(side, LazyOptional.of(() -> new SidedEnergyInputHandler(
                    energyStorage, () -> getSideMode(side) == MachineSideMode.ENERGY)));
            sidedFluidCapabilities.put(side, LazyOptional.of(() -> new SidedFluidInputHandler(
                    fluidInputHandler, () -> getSideMode(side) == MachineSideMode.INPUT)));
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<QuenchingRecipe> recipeOptional = findRecipe();
        if (!recipeOptional.isPresent()) {
            resetProgress();
            return;
        }

        QuenchingRecipe recipe = recipeOptional.get();
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

    private Optional<QuenchingRecipe> findRecipe() {
        if (level == null || inventory.getStackInSlot(0).isEmpty() || fluidTank.isEmpty()) {
            return Optional.empty();
        }

        ItemStack input = inventory.getStackInSlot(0);
        FluidStack fluid = fluidTank.getFluid();
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
            if (recipe.matches(input, fluid)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private boolean canProcess(QuenchingRecipe recipe) {
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

    private void process(QuenchingRecipe recipe) {
        ItemStack result = recipe.assemble(new Inventory(inventory.getStackInSlot(0).copy()));
        if (result.isEmpty()) {
            return;
        }

        FluidStack drained = fluidTank.drain(recipe.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < recipe.getFluidAmount()) {
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

    public boolean canAcceptInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
            if (recipe.getInput().test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAcceptFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (QuenchingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.QUENCHING_TYPE)) {
            if (recipe.getFluid() == stack.getFluid()) {
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
        return new TranslationTextComponent("container.justguithings.quench_chamber");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new QuenchChamberContainer(windowId, playerInventory, this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        energyStorage.setEnergy(nbt.getInt("Energy"));
        fluidTank.readFromNBT(nbt.getCompound("Tank"));
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
        nbt.put("Tank", fluidTank.writeToNBT(new CompoundNBT()));
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

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == null) {
                return fluidCapability.cast();
            }
            if (getSideMode(side) == MachineSideMode.INPUT) {
                LazyOptional<IFluidHandler> sided = sidedFluidCapabilities.get(side);
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
        fluidCapability.invalidate();
        for (LazyOptional<IItemHandler> capability : sidedItemCapabilities.values()) {
            capability.invalidate();
        }
        for (LazyOptional<IEnergyStorage> capability : sidedEnergyCapabilities.values()) {
            capability.invalidate();
        }
        for (LazyOptional<IFluidHandler> capability : sidedFluidCapabilities.values()) {
            capability.invalidate();
        }
    }
}
