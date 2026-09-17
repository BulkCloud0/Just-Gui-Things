package dev.bulkcloud0.justguithings.tile;

import dev.bulkcloud0.justguithings.menu.CrusherContainer;
import dev.bulkcloud0.justguithings.recipe.CrusherRecipe;
import dev.bulkcloud0.justguithings.registry.ModRecipes;
import dev.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CrusherTileEntity extends BaseMachineTileEntity implements ITickableTileEntity, net.minecraft.inventory.container.INamedContainerProvider {
    public static final int ENERGY_CAPACITY = 20000;
    public static final int DEFAULT_PROCESS_TIME = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private int progress;
    private int maxProgress = DEFAULT_PROCESS_TIME;

    private final IIntArray data = new IIntArray() {
        public int get(int index) {
            switch (index) {
                case 0: return progress;
                case 1: return maxProgress;
                case 2: return energyStorage.getEnergyStored();
                case 3: return energyStorage.getMaxEnergyStored();
                default: return 0;
            }
        }
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) maxProgress = value;
        }
        public int getCount() { return 4; }
    };

    public CrusherTileEntity() {
        super(ModTileEntities.CRUSHER.get(), ENERGY_CAPACITY, 256, 0);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            resetProgress();
            return;
        }

        CrusherRecipe recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.CRUSHING, new Inventory(input), level)
                .orElse(null);

        if (recipe == null || !canAccept(recipe.getResultItem())) {
            resetProgress();
            return;
        }

        maxProgress = recipe.getProcessingTime();
        if (!energyStorage.consumeEnergy(recipe.getEnergyPerTick())) return;

        progress++;
        if (progress >= maxProgress) {
            inventory.extractItem(0, 1, false);
            ItemStack result = recipe.getResultItem().copy();
            ItemStack output = inventory.getStackInSlot(1);
            if (output.isEmpty()) {
                inventory.setStackInSlot(1, result);
            } else {
                ItemStack combined = output.copy();
                combined.grow(result.getCount());
                inventory.setStackInSlot(1, combined);
            }
            progress = 0;
            setChanged();
        }
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void resetProgress() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    public ItemStackHandler getInventory() { return inventory; }
    public IIntArray getData() { return data; }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Progress", progress);
        return nbt;
    }

    @Override
    public void load(net.minecraft.block.BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        progress = nbt.getInt("Progress");
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
        return new TranslationTextComponent("container.justguithings.crusher");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
        return new CrusherContainer(id, inventory, this);
    }
}
