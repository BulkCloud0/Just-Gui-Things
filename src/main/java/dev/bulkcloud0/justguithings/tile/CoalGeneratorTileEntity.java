package dev.bulkcloud0.justguithings.tile;

import dev.bulkcloud0.justguithings.menu.CoalGeneratorContainer;
import dev.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CoalGeneratorTileEntity extends BaseMachineTileEntity implements ITickableTileEntity, net.minecraft.inventory.container.INamedContainerProvider {
    public static final int ENERGY_CAPACITY = 30000;
    public static final int FE_PER_TICK = 40;
    public static final int TRANSFER_RATE = 256;
    public static final int COAL_BURN_TIME = 1600;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private int burnTime;
    private int maxBurnTime;

    private final IIntArray data = new IIntArray() {
        public int get(int index) {
            switch (index) {
                case 0: return burnTime;
                case 1: return maxBurnTime;
                case 2: return energyStorage.getEnergyStored();
                case 3: return energyStorage.getMaxEnergyStored();
                default: return 0;
            }
        }
        public void set(int index, int value) {
            if (index == 0) burnTime = value;
            if (index == 1) maxBurnTime = value;
        }
        public int getCount() { return 4; }
    };

    public CoalGeneratorTileEntity() {
        super(ModTileEntities.COAL_GENERATOR.get(), ENERGY_CAPACITY, 0, TRANSFER_RATE);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (burnTime <= 0 && energyStorage.getEnergyStored() <= ENERGY_CAPACITY - FE_PER_TICK) {
            ItemStack fuel = inventory.getStackInSlot(0);
            if (!fuel.isEmpty() && inventory.isItemValid(0, fuel)) {
                inventory.extractItem(0, 1, false);
                burnTime = COAL_BURN_TIME;
                maxBurnTime = COAL_BURN_TIME;
                setChanged();
            }
        }

        if (burnTime > 0 && energyStorage.getEnergyStored() <= ENERGY_CAPACITY - FE_PER_TICK) {
            burnTime--;
            energyStorage.addEnergy(FE_PER_TICK);
        }

        pushEnergy();
    }

    private void pushEnergy() {
        if (level == null || energyStorage.getEnergyStored() <= 0) return;
        for (Direction direction : Direction.values()) {
            TileEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;
            neighbor.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).ifPresent(target -> {
                int available = energyStorage.extractEnergy(TRANSFER_RATE, true);
                if (available <= 0) return;
                int accepted = target.receiveEnergy(available, false);
                if (accepted > 0) energyStorage.extractEnergy(accepted, false);
            });
            if (energyStorage.getEnergyStored() <= 0) break;
        }
    }

    public ItemStackHandler getInventory() { return inventory; }
    public IIntArray getData() { return data; }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("MaxBurnTime", maxBurnTime);
        return nbt;
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return itemCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCapability.invalidate();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.coal_generator");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
        return new CoalGeneratorContainer(id, inventory, this);
    }
}
