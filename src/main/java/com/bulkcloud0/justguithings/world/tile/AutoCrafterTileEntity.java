package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.api.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.machine.BaseProcessingMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineUpgradeScaling;
import com.bulkcloud0.justguithings.recipe.MachineProcessingRecipe;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.AutoCrafterContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Optional;

public class AutoCrafterTileEntity extends BaseProcessingMachineTileEntity<AutoCrafterTileEntity.CraftingOperation> {
    public static final int GRID_SIZE = 9;
    public static final int PRIMARY_OUTPUT_SLOT = 9;
    public static final int RETURN_START = 10;
    public static final int RETURN_COUNT = 9;
    public static final int SPEED_MODULE_SLOT = 19;
    public static final int EFFICIENCY_MODULE_SLOT = 20;
    public static final int INVENTORY_SIZE = 21;
    public static final int CAPACITY = 80_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TICKS = 60;
    public static final int DEFAULT_ENERGY_PER_TICK = 30;
    public static final int MAX_MODULES_PER_TYPE = 4;

    private static final Container CRAFTING_CONTAINER = new Container(null, -1) {
        @Override
        public boolean stillValid(PlayerEntity player) {
            return false;
        }
    };

    private final NonNullList<ItemStack> lockedTemplate = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
    @Nullable
    private ResourceLocation lockedRecipeId;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            if (index <= 4) {
                return getProcessingData(index);
            }
            switch (index) {
                case 5: return getSpeedUpgradeCount();
                case 6: return getEfficiencyUpgradeCount();
                case 7: return isRecipeLocked() ? 1 : 0;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            if (index <= 4) {
                setProcessingData(index, value);
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public AutoCrafterTileEntity() {
        super(ModTileEntities.AUTO_CRAFTER.get(), CAPACITY, MAX_RECEIVE,
                INVENTORY_SIZE, 0, GRID_SIZE, PRIMARY_OUTPUT_SLOT, 1 + RETURN_COUNT,
                DEFAULT_PROCESS_TICKS, DEFAULT_ENERGY_PER_TICK);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SIZE || stack.isEmpty()) {
            return false;
        }
        if (!isRecipeLocked()) {
            return true;
        }
        ItemStack expected = lockedTemplate.get(slot);
        return !expected.isEmpty()
                && ItemStack.isSame(expected, stack)
                && ItemStack.tagMatches(expected, stack);
    }

    @Override
    protected int getMachineSlotLimit(int slot) {
        return slot >= 0 && slot < GRID_SIZE ? 1 : super.getMachineSlotLimit(slot);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        if (slot == SPEED_MODULE_SLOT) {
            return MachineModuleTypes.SPEED;
        }
        if (slot == EFFICIENCY_MODULE_SLOT) {
            return MachineModuleTypes.EFFICIENCY;
        }
        return null;
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    public boolean canAcceptInput(int slot, ItemStack stack) {
        return slot >= 0 && slot < GRID_SIZE
                && inventory.isItemValid(slot, stack)
                && inventory.getStackInSlot(slot).isEmpty();
    }

    public boolean isRecipeLocked() {
        return lockedRecipeId != null;
    }

    public boolean tryLockRecipe() {
        if (level == null) {
            return false;
        }
        CraftingInventory grid = createCraftingInventory();
        Optional<ICraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(IRecipeType.CRAFTING, grid, level);
        if (!recipe.isPresent() || recipe.get().assemble(grid).isEmpty()) {
            return false;
        }
        lockedRecipeId = recipe.get().getId();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack template = inventory.getStackInSlot(slot).copy();
            if (!template.isEmpty()) {
                template.setCount(1);
            }
            lockedTemplate.set(slot, template);
        }
        resetProcessing();
        setChanged();
        syncToClient();
        return true;
    }

    public void clearRecipeLock() {
        lockedRecipeId = null;
        clearTemplate();
        resetProcessing();
        setChanged();
        syncToClient();
    }

    @Override
    protected Optional<CraftingOperation> findCurrentRecipe() {
        if (level == null || lockedRecipeId == null || !matchesLockedTemplate()) {
            return Optional.empty();
        }
        CraftingInventory grid = createCraftingInventory();
        Optional<ICraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(IRecipeType.CRAFTING, grid, level);
        if (!recipe.isPresent() || !lockedRecipeId.equals(recipe.get().getId())) {
            return Optional.empty();
        }
        return Optional.of(new CraftingOperation(recipe.get()));
    }

    @Override
    protected boolean canProcessRecipe(CraftingOperation operation) {
        CraftingInventory grid = createCraftingInventory();
        if (level == null || lockedRecipeId == null
                || !lockedRecipeId.equals(operation.getId())
                || !operation.recipe.matches(grid, level)) {
            return false;
        }
        ItemStack result = operation.recipe.assemble(grid);
        return canFitPrimaryOutput(result)
                && canFitRemainders(operation.recipe.getRemainingItems(grid));
    }

    @Override
    protected int getEffectiveProcessingTime(CraftingOperation operation) {
        return MachineUpgradeScaling.getEffectiveProcessingTime(
                DEFAULT_PROCESS_TICKS, getSpeedUpgradeCount(), getEfficiencyUpgradeCount());
    }

    @Override
    protected int getEffectiveEnergyPerTick(CraftingOperation operation) {
        return MachineUpgradeScaling.getEffectiveEnergyPerTick(
                DEFAULT_ENERGY_PER_TICK, getSpeedUpgradeCount(), getEfficiencyUpgradeCount(), 1);
    }

    @Override
    protected void processRecipe(CraftingOperation operation) {
        if (level == null) {
            return;
        }
        CraftingInventory grid = createCraftingInventory();
        if (!operation.recipe.matches(grid, level)) {
            return;
        }
        ItemStack result = operation.recipe.assemble(grid);
        NonNullList<ItemStack> remainders = operation.recipe.getRemainingItems(grid);
        if (!canFitPrimaryOutput(result) || !canFitRemainders(remainders)) {
            return;
        }
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            if (!grid.getItem(slot).isEmpty()) {
                inventory.extractItem(slot, 1, false);
            }
        }
        insertPrimaryOutput(result);
        insertRemainders(remainders);
    }

    private CraftingInventory createCraftingInventory() {
        CraftingInventory grid = new CraftingInventory(CRAFTING_CONTAINER, 3, 3);
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            grid.setItem(slot, inventory.getStackInSlot(slot).copy());
        }
        return grid;
    }

    private boolean matchesLockedTemplate() {
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack expected = lockedTemplate.get(slot);
            ItemStack actual = inventory.getStackInSlot(slot);
            if (expected.isEmpty()) {
                if (!actual.isEmpty()) {
                    return false;
                }
                continue;
            }
            if (actual.isEmpty()
                    || !ItemStack.isSame(expected, actual)
                    || !ItemStack.tagMatches(expected, actual)) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitPrimaryOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack current = inventory.getStackInSlot(PRIMARY_OUTPUT_SLOT);
        if (current.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        return ItemStack.isSame(current, result)
                && ItemStack.tagMatches(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void insertPrimaryOutput(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(PRIMARY_OUTPUT_SLOT);
        if (current.isEmpty()) {
            inventory.setStackInSlot(PRIMARY_OUTPUT_SLOT, result.copy());
            return;
        }
        ItemStack combined = current.copy();
        combined.grow(result.getCount());
        inventory.setStackInSlot(PRIMARY_OUTPUT_SLOT, combined);
    }

    private boolean canFitRemainders(NonNullList<ItemStack> remainders) {
        ItemStack[] simulated = new ItemStack[RETURN_COUNT];
        for (int index = 0; index < RETURN_COUNT; index++) {
            simulated[index] = inventory.getStackInSlot(RETURN_START + index).copy();
        }
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty() && !insertIntoCopies(simulated, remainder.copy())) {
                return false;
            }
        }
        return true;
    }

    private boolean insertIntoCopies(ItemStack[] slots, ItemStack stack) {
        for (int index = 0; index < slots.length && !stack.isEmpty(); index++) {
            ItemStack current = slots[index];
            if (current.isEmpty()
                    || !ItemStack.isSame(current, stack)
                    || !ItemStack.tagMatches(current, stack)) {
                continue;
            }
            int moved = Math.min(stack.getCount(),
                    Math.max(0, current.getMaxStackSize() - current.getCount()));
            if (moved > 0) {
                current.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int index = 0; index < slots.length && !stack.isEmpty(); index++) {
            if (!slots[index].isEmpty()) {
                continue;
            }
            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack placed = stack.copy();
            placed.setCount(moved);
            slots[index] = placed;
            stack.shrink(moved);
        }
        return stack.isEmpty();
    }

    private void insertRemainders(NonNullList<ItemStack> remainders) {
        for (ItemStack remainder : remainders) {
            ItemStack pending = remainder.copy();
            if (pending.isEmpty()) {
                continue;
            }
            for (int slot = RETURN_START; slot < RETURN_START + RETURN_COUNT && !pending.isEmpty(); slot++) {
                ItemStack current = inventory.getStackInSlot(slot);
                if (current.isEmpty()
                        || !ItemStack.isSame(current, pending)
                        || !ItemStack.tagMatches(current, pending)) {
                    continue;
                }
                int moved = Math.min(pending.getCount(),
                        Math.max(0, current.getMaxStackSize() - current.getCount()));
                if (moved > 0) {
                    ItemStack combined = current.copy();
                    combined.grow(moved);
                    inventory.setStackInSlot(slot, combined);
                    pending.shrink(moved);
                }
            }
            for (int slot = RETURN_START; slot < RETURN_START + RETURN_COUNT && !pending.isEmpty(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                int moved = Math.min(pending.getCount(), pending.getMaxStackSize());
                ItemStack placed = pending.copy();
                placed.setCount(moved);
                inventory.setStackInSlot(slot, placed);
                pending.shrink(moved);
            }
        }
    }

    public int getSpeedUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.SPEED));
    }

    public int getEfficiencyUpgradeCount() {
        return Math.min(MAX_MODULES_PER_TYPE, getModuleCount(MachineModuleTypes.EFFICIENCY));
    }

    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.auto_crafter");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AutoCrafterContainer(windowId, playerInventory, this);
    }

    @Override
    protected void loadAdditionalProcessingData(CompoundNBT nbt) {
        clearTemplate();
        lockedRecipeId = null;
        String recipeId = nbt.getString("LockedRecipe");
        if (!recipeId.isEmpty()) {
            try {
                lockedRecipeId = new ResourceLocation(recipeId);
            } catch (RuntimeException ignored) {
                lockedRecipeId = null;
            }
        }
        boolean anyTemplate = false;
        if (lockedRecipeId != null && nbt.contains("LockedTemplate")) {
            ListNBT list = nbt.getList("LockedTemplate", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                CompoundNBT entry = list.getCompound(index);
                int slot = entry.getInt("Slot");
                if (slot < 0 || slot >= GRID_SIZE || !entry.contains("Stack")) {
                    continue;
                }
                ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                    lockedTemplate.set(slot, stack);
                    anyTemplate = true;
                }
            }
        }
        if (lockedRecipeId != null && !anyTemplate) {
            lockedRecipeId = null;
        }
    }

    @Override
    protected void saveAdditionalProcessingData(CompoundNBT nbt) {
        if (lockedRecipeId == null) {
            return;
        }
        nbt.putString("LockedRecipe", lockedRecipeId.toString());
        ListNBT list = new ListNBT();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = lockedTemplate.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundNBT entry = new CompoundNBT();
            entry.putInt("Slot", slot);
            entry.put("Stack", stack.save(new CompoundNBT()));
            list.add(entry);
        }
        nbt.put("LockedTemplate", list);
    }

    private void clearTemplate() {
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            lockedTemplate.set(slot, ItemStack.EMPTY);
        }
    }

    static final class CraftingOperation implements MachineProcessingRecipe {
        private final ICraftingRecipe recipe;

        private CraftingOperation(ICraftingRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public ResourceLocation getId() {
            return recipe.getId();
        }

        @Override
        public int getProcessingTime() {
            return DEFAULT_PROCESS_TICKS;
        }

        @Override
        public int getEnergyPerTick() {
            return DEFAULT_ENERGY_PER_TICK;
        }
    }
}
