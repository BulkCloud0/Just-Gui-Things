package com.bulkcloud0.justguithings.recipe;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SingleInputProcessingRecipe implements IRecipe<IInventory>, MachineProcessingRecipe {
    protected final ResourceLocation id;
    protected final Ingredient input;
    protected final RecipeOutput result;
    protected final int processingTime;
    protected final int energyPerTick;

    protected SingleInputProcessingRecipe(ResourceLocation id,
                                          Ingredient input,
                                          RecipeOutput result,
                                          int processingTime,
                                          int energyPerTick) {
        this.id = id;
        this.input = input;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return input.test(inventory.getItem(0));
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return getResultForInput(inventory.getItem(0));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return result.resolve();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getResultForInput(ItemStack inputStack) {
        return result.resolve(inputStack);
    }

    public List<ItemStack> getInputDisplayStacks() {
        return Arrays.stream(input.getItems())
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(SingleInputProcessingRecipe::getRegistryName))
                .map(ItemStack::copy)
                .collect(Collectors.toList());
    }

    public List<ItemStack> getResultDisplayStacks() {
        return getInputDisplayStacks().stream()
                .map(this::getResultForInput)
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toList());
    }

    private static String getRegistryName(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }
}
