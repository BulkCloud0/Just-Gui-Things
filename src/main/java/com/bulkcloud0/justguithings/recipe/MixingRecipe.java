package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MixingRecipe implements IRecipe<IInventory>, MachineProcessingRecipe {
    private final ResourceLocation id;
    private final Ingredient primary;
    private final int primaryCount;
    private final Ingredient secondary;
    private final int secondaryCount;
    private final RecipeOutput result;
    private final int processingTime;
    private final int energyPerTick;

    public MixingRecipe(ResourceLocation id,
                        Ingredient primary,
                        int primaryCount,
                        Ingredient secondary,
                        int secondaryCount,
                        RecipeOutput result,
                        int processingTime,
                        int energyPerTick) {
        this.id = id;
        this.primary = primary;
        this.primaryCount = primaryCount;
        this.secondary = secondary;
        this.secondaryCount = secondaryCount;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        ItemStack primaryStack = inventory.getItem(0);
        ItemStack secondaryStack = inventory.getItem(1);
        return primaryStack.getCount() >= primaryCount
                && secondaryStack.getCount() >= secondaryCount
                && primary.test(primaryStack)
                && secondary.test(secondaryStack);
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return getResultForPrimary(inventory.getItem(0));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return result.resolve();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(primary);
        ingredients.add(secondary);
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.MIXING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.MIXING_TYPE;
    }

    public Ingredient getPrimary() {
        return primary;
    }

    public int getPrimaryCount() {
        return primaryCount;
    }

    public Ingredient getSecondary() {
        return secondary;
    }

    public int getSecondaryCount() {
        return secondaryCount;
    }

    public ItemStack getResultForPrimary(ItemStack primaryStack) {
        return result.resolve(primaryStack);
    }

    public List<ItemStack> getPrimaryDisplayStacks() {
        return getDisplayStacks(primary, primaryCount);
    }

    public List<ItemStack> getSecondaryDisplayStacks() {
        return getDisplayStacks(secondary, secondaryCount);
    }

    public List<ItemStack> getResultDisplayStacks() {
        return result.getDisplayStacks();
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    private static List<ItemStack> getDisplayStacks(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(MixingRecipe::getRegistryName))
                .map(stack -> withCount(stack, count))
                .collect(Collectors.toList());
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static String getRegistryName(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<MixingRecipe> {
        @Override
        public MixingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("primary") || !json.has("secondary")) {
                throw new JsonSyntaxException("Mixing recipe " + recipeId + " requires primary and secondary ingredients");
            }

            Ingredient primary = Ingredient.fromJson(json.get("primary"));
            int primaryCount = JSONUtils.getAsInt(json, "primary_count", 1);
            Ingredient secondary = Ingredient.fromJson(json.get("secondary"));
            int secondaryCount = JSONUtils.getAsInt(json, "secondary_count", 1);
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 45);

            if (primary.isEmpty() || secondary.isEmpty()) {
                throw new JsonSyntaxException("Mixing recipe " + recipeId + " has an empty ingredient");
            }
            if (primaryCount <= 0 || secondaryCount <= 0) {
                throw new JsonSyntaxException("Mixing recipe ingredient counts must be greater than zero in " + recipeId);
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Mixing recipe time and FE/t must be greater than zero in " + recipeId);
            }

            return new MixingRecipe(recipeId, primary, primaryCount, secondary, secondaryCount,
                    result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public MixingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient primary = Ingredient.fromNetwork(buffer);
            int primaryCount = buffer.readVarInt();
            Ingredient secondary = Ingredient.fromNetwork(buffer);
            int secondaryCount = buffer.readVarInt();
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new MixingRecipe(recipeId, primary, primaryCount, secondary, secondaryCount,
                    result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, MixingRecipe recipe) {
            recipe.primary.toNetwork(buffer);
            buffer.writeVarInt(recipe.primaryCount);
            recipe.secondary.toNetwork(buffer);
            buffer.writeVarInt(recipe.secondaryCount);
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
