package com.bulkcloud0.justguithings.machine;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.machine.module.IMachineModule;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
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

public abstract class BaseMachineTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private static final int SIDE_CONFIG_VERSION = 3;
    private static final MachineSideMode[] DEFAULT_SIDE_MODES = {
            MachineSideMode.DISABLED,
            MachineSideMode.INPUT,
            MachineSideMode.OUTPUT,
            MachineSideMode.ENERGY
    };
    private final int baseEnergyCapacity;
    private final int inputStart;
    private final int inputCount;
    private final int outputStart;
    private final int outputCount;

    protected final ModEnergyStorage energyStorage;
    protected final ItemStackHandler inventory;

    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> sidedItemCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities = new EnumMap<>(Direction.class);

    private LazyOptional<IEnergyStorage> energyCapability;
    private LazyOptional<IItemHandler> itemCapability;

    protected BaseMachineTileEntity(TileEntityType<?> tileEntityType,
                                    int energyCapacity,
                                    int maxReceive,
                                    int inventorySize,
                                    int inputStart,
                                    int inputCount,
                                    int outputStart,
                                    int outputCount) {
        this(tileEntityType, energyCapacity, maxReceive, 0,
                inventorySize, inputStart, inputCount, outputStart, outputCount);
    }

    protected BaseMachineTileEntity(TileEntityType<?> tileEntityType,
                                    int energyCapacity,
                                    int maxReceive,
                                    int maxExtract,
                                    int inventorySize,
                                    int inputStart,
                                    int inputCount,
                                    int outputStart,
                                    int outputCount) {
        super(tileEntityType);
        this.baseEnergyCapacity = energyCapacity;
        this.inputStart = inputStart;
        this.inputCount = inputCount;
        this.outputStart = outputStart;
        this.outputCount = outputCount;

        this.energyStorage = new ModEnergyStorage(energyCapacity, maxReceive, maxExtract) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (!simulate && received > 0) {
                    setChanged();
                }
                return received;
            }
        };

        this.inventory = new ItemStackHandler(inventorySize) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                ResourceLocation moduleType = getModuleTypeForSlot(slot);
                if (moduleType != null) {
                    return isModuleOfType(stack, moduleType);
                }
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                ResourceLocation moduleType = getModuleTypeForSlot(slot);
                if (moduleType != null) {
                    return getModuleSlotLimit(slot, moduleType);
                }
                return getMachineSlotLimit(slot);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged(slot);
                setChanged();
            }
        };

        initializeDefaultSides();
        initializeCapabilities();
    }

    protected abstract boolean isItemValidForSlot(int slot, ItemStack stack);

    @Nullable
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        return null;
    }

    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return 64;
    }

    protected int getMachineSlotLimit(int slot) {
        return 64;
    }

    protected void onInventoryChanged(int slot) {
    }

    public final int getModuleCount(ResourceLocation moduleType) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (moduleType.equals(getModuleTypeForSlot(slot))) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (isModuleOfType(stack, moduleType)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    public final int findModuleSlot(ItemStack stack) {
        ResourceLocation moduleType = getModuleType(stack);
        if (moduleType == null) {
            return -1;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (moduleType.equals(getModuleTypeForSlot(slot))) {
                return slot;
            }
        }
        return -1;
    }

    @Nullable
    private ResourceLocation getModuleType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IMachineModule)) {
            return null;
        }
        return ((IMachineModule) stack.getItem()).getMachineModuleType();
    }

    private boolean isModuleOfType(ItemStack stack, ResourceLocation expectedType) {
        ResourceLocation actualType = getModuleType(stack);
        return actualType != null && expectedType.equals(actualType);
    }

    private void initializeDefaultSides() {
        sideModes.put(Direction.UP, MachineSideMode.INPUT);
        sideModes.put(Direction.DOWN, MachineSideMode.OUTPUT);
        sideModes.put(Direction.NORTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.SOUTH, MachineSideMode.ENERGY);
        sideModes.put(Direction.WEST, MachineSideMode.ENERGY);
        sideModes.put(Direction.EAST, MachineSideMode.ENERGY);
    }

    private void initializeCapabilities() {
        energyCapability = LazyOptional.of(() -> energyStorage);
        itemCapability = LazyOptional.of(() -> inventory);

        for (Direction direction : Direction.values()) {
            final Direction side = direction;
            sidedItemCapabilities.put(side, LazyOptional.of(() -> new MachineSidedItemHandler(
                    inventory, inputStart, inputCount, outputStart, outputCount, () -> getSideMode(side))));
            sidedEnergyCapabilities.put(side, LazyOptional.of(() -> new MachineSidedEnergyHandler(
                    energyStorage,
                    () -> canReceiveEnergyFrom(side, getSideMode(side)),
                    () -> canExtractEnergyFrom(side, getSideMode(side)))));
        }
    }

    protected MachineSideMode[] getAllowedSideModes() {
        return DEFAULT_SIDE_MODES;
    }

    protected boolean supportsItemCapability() {
        return true;
    }

    protected boolean canReceiveEnergyFrom(Direction side, MachineSideMode mode) {
        return mode == MachineSideMode.ENERGY;
    }

    protected boolean canExtractEnergyFrom(Direction side, MachineSideMode mode) {
        return false;
    }

    protected MachineSideMode normalizeLoadedSideMode(Direction side, MachineSideMode mode, int configVersion) {
        return isSideModeSupported(mode) ? mode : MachineSideMode.DISABLED;
    }

    public final boolean isSideModeSupported(MachineSideMode mode) {
        if (mode == null) {
            return false;
        }
        for (MachineSideMode allowed : getAllowedSideModes()) {
            if (allowed == mode) {
                return true;
            }
        }
        return false;
    }

    protected final void setSideMode(Direction side, MachineSideMode mode) {
        sideModes.put(side, isSideModeSupported(mode) ? mode : MachineSideMode.DISABLED);
    }

    public MachineSideMode getSideMode(Direction side) {
        return sideModes.getOrDefault(side, MachineSideMode.DISABLED);
    }

    public MachineSideMode cycleSideMode(Direction side) {
        MachineSideMode[] allowed = getAllowedSideModes();
        if (allowed.length == 0) {
            sideModes.put(side, MachineSideMode.DISABLED);
            setChanged();
            return MachineSideMode.DISABLED;
        }

        MachineSideMode current = getSideMode(side);
        int currentIndex = -1;
        for (int index = 0; index < allowed.length; index++) {
            if (allowed[index] == current) {
                currentIndex = index;
                break;
            }
        }

        MachineSideMode next = allowed[(currentIndex + 1) % allowed.length];
        sideModes.put(side, next);
        setChanged();
        return next;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getEnergyCapacity() {
        return baseEnergyCapacity;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        CompoundNBT inventoryNbt = nbt.getCompound("Inventory").copy();
        inventoryNbt.putInt("Size", inventory.getSlots());
        inventory.deserializeNBT(inventoryNbt);

        energyStorage.setCapacity(getEnergyCapacity());
        energyStorage.setEnergy(nbt.getInt("Energy"));

        if (nbt.contains("SideConfig")) {
            CompoundNBT config = nbt.getCompound("SideConfig");
            int configVersion = config.contains("Version") ? config.getInt("Version") : 1;
            for (Direction direction : Direction.values()) {
                String key = "Side" + direction.ordinal();
                if (config.contains(key)) {
                    MachineSideMode loaded = MachineSideMode.fromOrdinal(config.getInt(key));
                    sideModes.put(direction, normalizeLoadedSideMode(direction, loaded, configVersion));
                }
            }
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Energy", energyStorage.getEnergyStored());

        CompoundNBT config = new CompoundNBT();
        config.putInt("Version", SIDE_CONFIG_VERSION);
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
            MachineSideMode mode = getSideMode(side);
            if (canReceiveEnergyFrom(side, mode) || canExtractEnergyFrom(side, mode)) {
                LazyOptional<IEnergyStorage> sided = sidedEnergyCapabilities.get(side);
                return sided == null ? LazyOptional.empty() : sided.cast();
            }
            return LazyOptional.empty();
        }

        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && supportsItemCapability()) {
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
