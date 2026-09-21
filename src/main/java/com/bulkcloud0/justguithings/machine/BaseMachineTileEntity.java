package com.bulkcloud0.justguithings.machine;

import com.bulkcloud0.justguithings.energy.ModEnergyStorage;
import com.bulkcloud0.justguithings.logistics.FairShareAllocator;
import com.bulkcloud0.justguithings.api.machine.module.IMachineModule;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTags;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

public abstract class BaseMachineTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private static final int SIDE_CONFIG_VERSION = 5;
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
    private final int maxEnergyExtractPerTick;

    private long energyExtractBudgetTick = Long.MIN_VALUE;
    private int energyExtractedThisTick;

    protected final ModEnergyStorage energyStorage;
    protected final ItemStackHandler inventory;

    private final EnumMap<Direction, MachineSideMode> sideModes = new EnumMap<>(Direction.class);
    private MachineRedstoneMode redstoneMode = MachineRedstoneMode.ALWAYS;
    private final EnumMap<Direction, LazyOptional<IItemHandler>> sidedItemCapabilities = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities = new EnumMap<>(Direction.class);

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
        this.maxEnergyExtractPerTick = Math.max(0, maxExtract);

        this.energyStorage = new ModEnergyStorage(energyCapacity, maxReceive, maxExtract) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (!simulate && received > 0) {
                    setChanged();
                }
                return received;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int allowed = getEnergyExtractAllowance(maxExtract);
                if (allowed <= 0) {
                    return 0;
                }

                int extracted = super.extractEnergy(allowed, simulate);
                if (!simulate && extracted > 0) {
                    recordEnergyExtracted(extracted);
                    setChanged();
                }
                return extracted;
            }

            @Override
            public boolean canExtract() {
                return super.canExtract() && getEnergyExtractAllowance(Integer.MAX_VALUE) > 0;
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
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof IMachineModule) {
            return ((IMachineModule) stack.getItem()).getMachineModuleType();
        }
        return MachineModuleTags.findType(stack);
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
        for (Direction direction : Direction.values()) {
            sidedItemCapabilities.put(direction, createSidedItemCapability(direction));
            sidedEnergyCapabilities.put(direction, createSidedEnergyCapability(direction));
        }
    }

    private LazyOptional<IItemHandler> createSidedItemCapability(Direction side) {
        return LazyOptional.of(() -> new MachineSidedItemHandler(
                inventory, inputStart, inputCount, outputStart, outputCount,
                () -> getItemSideMode(side, getSideMode(side))));
    }

    private LazyOptional<IEnergyStorage> createSidedEnergyCapability(Direction side) {
        return LazyOptional.of(() -> new MachineSidedEnergyHandler(
                energyStorage,
                () -> canReceiveEnergyFrom(side, getSideMode(side)),
                () -> canExtractEnergyFrom(side, getSideMode(side))));
    }

    private void refreshSidedCapabilities(Direction side) {
        LazyOptional<IItemHandler> oldItem = sidedItemCapabilities.put(
                side, createSidedItemCapability(side));
        LazyOptional<IEnergyStorage> oldEnergy = sidedEnergyCapabilities.put(
                side, createSidedEnergyCapability(side));

        if (oldItem != null) {
            oldItem.invalidate();
        }
        if (oldEnergy != null) {
            oldEnergy.invalidate();
        }

        refreshAdditionalSidedCapabilities(side);
        notifyCapabilityNeighbors();
    }

    protected void refreshAdditionalSidedCapabilities(Direction side) {
    }

    private void notifyCapabilityNeighbors() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        level.updateNeighborsAt(worldPosition, state.getBlock());
    }

    protected MachineSideMode[] getAllowedSideModes() {
        return DEFAULT_SIDE_MODES;
    }

    protected boolean supportsItemCapability() {
        return true;
    }

    public boolean supportsRedstoneControl() {
        return false;
    }

    public final MachineRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public final MachineRedstoneMode cycleRedstoneMode() {
        if (!supportsRedstoneControl()) {
            return MachineRedstoneMode.ALWAYS;
        }
        redstoneMode = redstoneMode.next();
        setChanged();
        syncToClient();
        return redstoneMode;
    }

    protected final boolean isOperationEnabled() {
        if (!supportsRedstoneControl() || level == null) {
            return true;
        }
        return redstoneMode.allows(level.hasNeighborSignal(worldPosition));
    }

    protected MachineSideMode getItemSideMode(Direction side, MachineSideMode mode) {
        return mode;
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
        MachineSideMode current = getSideMode(side);
        MachineSideMode[] allowed = getAllowedSideModes();
        if (allowed.length == 0) {
            sideModes.put(side, MachineSideMode.DISABLED);
            if (current != MachineSideMode.DISABLED) {
                refreshSidedCapabilities(side);
            }
            setChanged();
            syncToClient();
            return MachineSideMode.DISABLED;
        }

        int currentIndex = -1;
        for (int index = 0; index < allowed.length; index++) {
            if (allowed[index] == current) {
                currentIndex = index;
                break;
            }
        }

        MachineSideMode next = allowed[(currentIndex + 1) % allowed.length];
        sideModes.put(side, next);
        if (next != current) {
            refreshSidedCapabilities(side);
        }
        setChanged();
        syncToClient();
        return next;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Nullable
    protected final TileEntity getLoadedBlockEntity(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }
        return level.getBlockEntity(pos);
    }

    protected final int pushEnergyToNeighborsFairly(int maxOutput) {
        return pushEnergyToNeighborsFairly(maxOutput, neighbor -> true);
    }

    protected final int pushEnergyToNeighborsFairly(int maxOutput, Predicate<TileEntity> neighborFilter) {
        if (level == null || maxOutput <= 0 || energyStorage.getEnergyStored() <= 0) {
            return 0;
        }

        List<EnergyPushTarget> targets = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            MachineSideMode mode = getSideMode(direction);
            if (!canExtractEnergyFrom(direction, mode)) {
                continue;
            }

            TileEntity neighbor = getLoadedBlockEntity(worldPosition.relative(direction));
            if (neighbor == null
                    || !canPushEnergyToNeighbor(direction, mode, neighbor)
                    || (neighborFilter != null && !neighborFilter.test(neighbor))) {
                continue;
            }

            IEnergyStorage receiver = neighbor
                    .getCapability(CapabilityEnergy.ENERGY, direction.getOpposite())
                    .orElse(null);
            if (receiver != null && receiver.canReceive()) {
                targets.add(new EnergyPushTarget(receiver));
            }
        }

        if (targets.isEmpty()) {
            return 0;
        }

        int transferred = 0;
        for (int round = 0; round < 3; round++) {
            int budget = energyStorage.extractEnergy(maxOutput, true);
            if (budget <= 0) {
                break;
            }

            int[] demands = new int[targets.size()];
            for (int index = 0; index < targets.size(); index++) {
                demands[index] = Math.max(0, targets.get(index).handler.receiveEnergy(budget, true));
            }

            int[] allocations = FairShareAllocator.allocate(budget, demands);
            int movedThisRound = 0;

            for (int index = 0; index < targets.size(); index++) {
                int planned = allocations[index];
                if (planned <= 0) {
                    continue;
                }

                int extracted = energyStorage.extractEnergy(planned, false);
                if (extracted <= 0) {
                    break;
                }

                int inserted = targets.get(index).handler.receiveEnergy(extracted, false);
                if (inserted < extracted) {
                    int refunded = extracted - inserted;
                    energyStorage.addEnergy(refunded);
                    refundEnergyExtractBudget(refunded);
                }

                movedThisRound += inserted;
                transferred += inserted;
            }

            if (movedThisRound <= 0) {
                break;
            }
        }

        return transferred;
    }

    protected boolean canPushEnergyToNeighbor(Direction direction,
                                              MachineSideMode mode,
                                              TileEntity neighbor) {
        return true;
    }

    public int getEnergyCapacity() {
        return baseEnergyCapacity;
    }

    private int getEnergyExtractAllowance(int requested) {
        if (requested <= 0 || maxEnergyExtractPerTick <= 0) {
            return 0;
        }
        if (level == null) {
            return Math.min(requested, maxEnergyExtractPerTick);
        }

        refreshEnergyExtractBudget();
        return Math.min(requested, Math.max(0, maxEnergyExtractPerTick - energyExtractedThisTick));
    }

    private void recordEnergyExtracted(int amount) {
        if (amount <= 0 || level == null) {
            return;
        }
        refreshEnergyExtractBudget();
        energyExtractedThisTick = Math.min(maxEnergyExtractPerTick, energyExtractedThisTick + amount);
    }

    protected final void refundEnergyExtractBudget(int amount) {
        if (amount <= 0 || level == null) {
            return;
        }
        refreshEnergyExtractBudget();
        energyExtractedThisTick = Math.max(0, energyExtractedThisTick - amount);
    }

    private void refreshEnergyExtractBudget() {
        long gameTime = level.getGameTime();
        if (energyExtractBudgetTick != gameTime) {
            energyExtractBudgetTick = gameTime;
            energyExtractedThisTick = 0;
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        CompoundNBT inventoryNbt = nbt.getCompound("Inventory").copy();
        inventoryNbt.putInt("Size", inventory.getSlots());
        inventory.deserializeNBT(inventoryNbt);

        energyStorage.setCapacity(getEnergyCapacity());
        energyStorage.setEnergy(nbt.getInt("Energy"));

        if (nbt.contains("RedstoneMode")) {
            redstoneMode = MachineRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        } else {
            redstoneMode = MachineRedstoneMode.ALWAYS;
        }

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
        if (supportsRedstoneControl()) {
            nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        }

        CompoundNBT config = new CompoundNBT();
        config.putInt("Version", SIDE_CONFIG_VERSION);
        for (Direction direction : Direction.values()) {
            config.putInt("Side" + direction.ordinal(), getSideMode(direction).ordinal());
        }
        nbt.put("SideConfig", config);
        return nbt;
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SUpdateTileEntityPacket packet) {
        CompoundNBT tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(getBlockState(), tag);
        }
    }

    protected final void syncToClient() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 2);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (side == null) {
                // Side-configurable block capabilities require a concrete face.
                return LazyOptional.empty();
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
                // Side-configurable block capabilities require a concrete face.
                return LazyOptional.empty();
            }
            MachineSideMode mode = getItemSideMode(side, getSideMode(side));
            if (mode == MachineSideMode.INPUT || mode == MachineSideMode.OUTPUT) {
                LazyOptional<IItemHandler> sided = sidedItemCapabilities.get(side);
                return sided == null ? LazyOptional.empty() : sided.cast();
            }
            return LazyOptional.empty();
        }

        return super.getCapability(cap, side);
    }

    private static final class EnergyPushTarget {
        private final IEnergyStorage handler;

        private EnergyPushTarget(IEnergyStorage handler) {
            this.handler = handler;
        }
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IItemHandler> capability : sidedItemCapabilities.values()) {
            capability.invalidate();
        }
        for (LazyOptional<IEnergyStorage> capability : sidedEnergyCapabilities.values()) {
            capability.invalidate();
        }
    }

    @Override
    protected void reviveCaps() {
        super.reviveCaps();
        initializeCapabilities();
    }
}
