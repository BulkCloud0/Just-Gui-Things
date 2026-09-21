package com.bulkcloud0.justguithings.world.tile;

import com.bulkcloud0.justguithings.machine.BaseMachineTileEntity;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import com.bulkcloud0.justguithings.registry.ModTileEntities;
import com.bulkcloud0.justguithings.world.container.AutomatedAssemblerContainer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public class AutomatedAssemblerTileEntity extends BaseMachineTileEntity {
    public static final int CAPACITY = 150_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TICKS = 100;
    public static final int DEFAULT_ENERGY_PER_TICK = 40;
    public static final int MAX_MODULES_PER_TYPE = 4;

    public static final int INPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int SPEED_MODULE_SLOT = 10;
    public static final int EFFICIENCY_MODULE_SLOT = 11;

    private static final String LOCKED_RECIPE_KEY = "LockedCraftingRecipe";
    private static final String TEMPLATE_KEY = "CraftingTemplate";

    private final NonNullList<ItemStack> recipeTemplate =
            NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
    private final Container craftingMenu = new Container(null, 0) {
        @Override
        public boolean stillValid(PlayerEntity player) {
            return false;
        }
    };

    private ResourceLocation lockedRecipeId;
    private int progress;
    private int currentProcessTicks = DEFAULT_PROCESS_TICKS;
    private int currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
    private int syncedRecipeLocked;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return progress;
                case 1: return energyStorage.getEnergyStored() & 0xFFFF;
                case 2: return (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case 3: return currentProcessTicks;
                case 4: return currentEnergyPerTick;
                case 5: return getSpeedUpgradeCount();
                case 6: return getEfficiencyUpgradeCount();
                case 7:
                    return level != null && level.isClientSide
                            ? syncedRecipeLocked
                            : (lockedRecipeId != null ? 1 : 0);
                default: return 0;
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
                case 7:
                    syncedRecipeLocked = value;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public AutomatedAssemblerTileEntity() {
        super(ModTileEntities.AUTOMATED_ASSEMBLER.get(), CAPACITY, MAX_RECEIVE,
                12, 0, INPUT_SLOT_COUNT, OUTPUT_SLOT, 1);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT || stack.isEmpty()) {
            return false;
        }
        if (lockedRecipeId == null) {
            return true;
        }

        ItemStack template = recipeTemplate.get(slot);
        return !template.isEmpty()
                && ItemStack.isSame(template, stack)
                && ItemStack.tagMatches(template, stack);
    }

    @Nullable
    @Override
    protected ResourceLocation getModuleTypeForSlot(int slot) {
        switch (slot) {
            case SPEED_MODULE_SLOT: return MachineModuleTypes.SPEED;
            case EFFICIENCY_MODULE_SLOT: return MachineModuleTypes.EFFICIENCY;
            default: return null;
        }
    }

    @Override
    protected int getModuleSlotLimit(int slot, ResourceLocation moduleType) {
        return MAX_MODULES_PER_TYPE;
    }

    @Override
    protected int getMachineSlotLimit(int slot) {
        if (slot >= 0 && slot < INPUT_SLOT_COUNT && lockedRecipeId != null) {
            return 1;
        }
        return super.getMachineSlotLimit(slot);
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
        if (!isOperationEnabled()) {
            return;
        }
        if (lockedRecipeId == null) {
            resetProcessing();
            return;
        }

        Optional<ICraftingRecipe> recipeOptional = getLockedRecipe();
        if (!recipeOptional.isPresent()) {
            clearRecipeLock();
            return;
        }

        ICraftingRecipe recipe = recipeOptional.get();
        CraftingInventory grid = createCraftingGrid();
        ItemStack result = recipe.matches(grid, level) ? recipe.assemble(grid) : ItemStack.EMPTY;
        if (result.isEmpty() || hasCraftingRemainders(recipe, grid) || !canFitOutput(result)) {
            resetProcessing();
            return;
        }

        currentProcessTicks = getEffectiveProcessingTime();
        currentEnergyPerTick = getEffectiveEnergyPerTick();
        if (energyStorage.getEnergyStored() < currentEnergyPerTick) {
            return;
        }

        energyStorage.consumeEnergy(currentEnergyPerTick);
        progress++;

        if (progress >= currentProcessTicks) {
            processCraft(recipe);
            progress = 0;
        }

        setChanged();
    }

    public boolean lockCurrentRecipe() {
        if (level == null || level.isClientSide) {
            return false;
        }

        CraftingInventory grid = createCraftingGrid();
        Optional<ICraftingRecipe> recipeOptional =
                level.getRecipeManager().getRecipeFor(IRecipeType.CRAFTING, grid, level);
        if (!recipeOptional.isPresent()) {
            return false;
        }

        ICraftingRecipe recipe = recipeOptional.get();
        ItemStack result = recipe.assemble(grid);
        if (result.isEmpty() || hasCraftingRemainders(recipe, grid)) {
            return false;
        }

        lockedRecipeId = recipe.getId();
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack sample = inventory.getStackInSlot(slot);
            if (sample.isEmpty()) {
                recipeTemplate.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack stored = sample.copy();
                stored.setCount(1);
                recipeTemplate.set(slot, stored);
            }
        }

        progress = 0;
        currentProcessTicks = getEffectiveProcessingTime();
        currentEnergyPerTick = getEffectiveEnergyPerTick();
        setChanged();
        syncToClient();
        return true;
    }

    public void clearRecipeLock() {
        if (lockedRecipeId == null && recipeTemplate.stream().allMatch(ItemStack::isEmpty)) {
            return;
        }

        lockedRecipeId = null;
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            recipeTemplate.set(slot, ItemStack.EMPTY);
        }
        resetProcessing();
        setChanged();
        syncToClient();
    }

    public boolean isRecipeLocked() {
        return lockedRecipeId != null;
    }

    public boolean canAcceptInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (lockedRecipeId == null) {
            return true;
        }
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            if (isItemValidForSlot(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    private Optional<ICraftingRecipe> getLockedRecipe() {
        if (level == null || lockedRecipeId == null) {
            return Optional.empty();
        }

        Optional<? extends IRecipe<?>> recipeOptional =
                level.getRecipeManager().byKey(lockedRecipeId);
        if (!recipeOptional.isPresent() || !(recipeOptional.get() instanceof ICraftingRecipe)) {
            return Optional.empty();
        }

        ICraftingRecipe recipe = (ICraftingRecipe) recipeOptional.get();
        return recipe.getType() == IRecipeType.CRAFTING
                ? Optional.of(recipe)
                : Optional.empty();
    }

    private CraftingInventory createCraftingGrid() {
        CraftingInventory grid = new CraftingInventory(craftingMenu, 3, 3);
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            grid.setItem(slot, inventory.getStackInSlot(slot).copy());
        }
        return grid;
    }

    private boolean hasCraftingRemainders(ICraftingRecipe recipe, CraftingInventory grid) {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid);
        for (ItemStack stack : remaining) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean canFitOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        if (!ItemStack.isSame(output, result) || !ItemStack.tagMatches(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount()
                <= Math.min(output.getMaxStackSize(), inventory.getSlotLimit(OUTPUT_SLOT));
    }

    private void processCraft(ICraftingRecipe recipe) {
        CraftingInventory grid = createCraftingGrid();
        if (!recipe.matches(grid, level)) {
            return;
        }

        ItemStack result = recipe.assemble(grid);
        if (result.isEmpty() || hasCraftingRemainders(recipe, grid) || !canFitOutput(result)) {
            return;
        }

        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                inventory.extractItem(slot, 1, false);
            }
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            ItemStack combined = output.copy();
            combined.grow(result.getCount());
            inventory.setStackInSlot(OUTPUT_SLOT, combined);
        }
    }

    private int getEffectiveProcessingTime() {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        int efficiencyTimeMultiplier = 100 + 10 * getEfficiencyUpgradeCount();
        long scaled = (long) DEFAULT_PROCESS_TICKS * efficiencyTimeMultiplier;
        return Math.max(20, (int) ((scaled + speedMultiplier - 1L) / speedMultiplier));
    }

    private int getEffectiveEnergyPerTick() {
        int speedMultiplier = 100 + 50 * getSpeedUpgradeCount();
        int efficiencyMultiplier = Math.max(40, 100 - 15 * getEfficiencyUpgradeCount());
        long scaled = (long) DEFAULT_ENERGY_PER_TICK * speedMultiplier * efficiencyMultiplier;
        return Math.max(1, (int) ((scaled + 9_999L) / 10_000L));
    }

    private void resetProcessing() {
        boolean changed = progress != 0
                || currentProcessTicks != DEFAULT_PROCESS_TICKS
                || currentEnergyPerTick != DEFAULT_ENERGY_PER_TICK;
        progress = 0;
        currentProcessTicks = DEFAULT_PROCESS_TICKS;
        currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
        if (changed) {
            setChanged();
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
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        progress = Math.max(0, nbt.getInt("Progress"));
        currentProcessTicks = DEFAULT_PROCESS_TICKS;
        currentEnergyPerTick = DEFAULT_ENERGY_PER_TICK;
        lockedRecipeId = null;

        String recipeId = nbt.getString(LOCKED_RECIPE_KEY);
        if (!recipeId.isEmpty()) {
            try {
                lockedRecipeId = new ResourceLocation(recipeId);
            } catch (RuntimeException ignored) {
                lockedRecipeId = null;
            }
        }

        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            recipeTemplate.set(slot, ItemStack.EMPTY);
        }
        if (nbt.contains(TEMPLATE_KEY)) {
            CompoundNBT templateTag = nbt.getCompound(TEMPLATE_KEY);
            for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                String key = "Slot" + slot;
                if (templateTag.contains(key)) {
                    ItemStack sample = ItemStack.of(templateTag.getCompound(key));
                    if (!sample.isEmpty()) {
                        sample.setCount(1);
                        recipeTemplate.set(slot, sample);
                    }
                }
            }
        }

        syncedRecipeLocked = lockedRecipeId != null ? 1 : 0;
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("Progress", progress);

        if (lockedRecipeId != null) {
            nbt.putString(LOCKED_RECIPE_KEY, lockedRecipeId.toString());
        }

        CompoundNBT templateTag = new CompoundNBT();
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack sample = recipeTemplate.get(slot);
            if (!sample.isEmpty()) {
                templateTag.put("Slot" + slot, sample.save(new CompoundNBT()));
            }
        }
        if (!templateTag.isEmpty()) {
            nbt.put(TEMPLATE_KEY, templateTag);
        }
        return nbt;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.justguithings.automated_assembler");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AutomatedAssemblerContainer(windowId, playerInventory, this);
    }
}
