package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssemblyRecipe implements IRecipe<IInventory>, MachineProcessingRecipe {
    public static final int MAX_INPUTS = 4;

    private final ResourceLocation id;
    private final List<Ingredient> ingredients;
    private final List<Integer> counts;
    private final RecipeOutput result;
    private final int processingTime;
    private final int energyPerTick;

    public AssemblyRecipe(ResourceLocation id,
                          List<Ingredient> ingredients,
                          List<Integer> counts,
                          RecipeOutput result,
                          int processingTime,
                          int energyPerTick) {
        this.id = id;
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.counts = Collections.unmodifiableList(new ArrayList<>(counts));
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return findMatchingSlots(inventory) != null;
    }

    @Nullable
    public int[] findMatchingSlots(IInventory inventory) {
        int checkedSlots = Math.min(MAX_INPUTS, inventory.getContainerSize());
        int nonEmptySlots = 0;
        for (int slot = 0; slot < checkedSlots; slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                nonEmptySlots++;
            }
        }
        if (nonEmptySlots != ingredients.size()) {
            return null;
        }

        int[] ingredientSlots = new int[ingredients.size()];
        java.util.Arrays.fill(ingredientSlots, -1);
        boolean[] usedSlots = new boolean[checkedSlots];
        return matchIngredient(0, inventory, usedSlots, ingredientSlots)
                ? ingredientSlots
                : null;
    }

    private boolean matchIngredient(int ingredientIndex,
                                    IInventory inventory,
                                    boolean[] usedSlots,
                                    int[] ingredientSlots) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);
        int required = counts.get(ingredientIndex);
        for (int slot = 0; slot < usedSlots.length; slot++) {
            if (usedSlots[slot]) {
                continue;
            }

            ItemStack stack = inventory.getItem(slot);
            if (stack.getCount() < required || !ingredient.test(stack)) {
                continue;
            }

            usedSlots[slot] = true;
            ingredientSlots[ingredientIndex] = slot;
            if (matchIngredient(ingredientIndex + 1, inventory, usedSlots, ingredientSlots)) {
                return true;
            }
            ingredientSlots[ingredientIndex] = -1;
            usedSlots[slot] = false;
        }
        return false;
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        ItemStack provider = inventory.getContainerSize() > 0 ? inventory.getItem(0) : ItemStack.EMPTY;
        return result.resolve(provider);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem() {
        return result.resolve();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.addAll(ingredients);
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.ASSEMBLY_TYPE;
    }

    public int getInputCount() {
        return ingredients.size();
    }

    public Ingredient getIngredient(int slot) {
        return ingredients.get(slot);
    }

    public int getRequiredCount(int slot) {
        return counts.get(slot);
    }

    public ItemStack getResultForInput(ItemStack provider) {
        return result.resolve(provider);
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<AssemblyRecipe> {
        @Override
        public AssemblyRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray inputArray = JSONUtils.getAsJsonArray(json, "ingredients");
            if (inputArray.size() < 1 || inputArray.size() > MAX_INPUTS) {
                throw new JsonSyntaxException("Assembly recipe " + recipeId
                        + " requires between 1 and " + MAX_INPUTS + " ingredients");
            }

            List<Ingredient> ingredients = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (JsonElement element : inputArray) {
                if (!element.isJsonObject()) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId
                            + " has a non-object ingredient entry");
                }
                JsonObject input = element.getAsJsonObject();
                if (!input.has("ingredient")) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId
                            + " has an ingredient entry without ingredient");
                }
                Ingredient ingredient = Ingredient.fromJson(input.get("ingredient"));
                int count = JSONUtils.getAsInt(input, "count", 1);
                if (ingredient.isEmpty() || count <= 0) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId
                            + " has an empty ingredient or non-positive count");
                }
                ingredients.add(ingredient);
                counts.add(count);
            }

            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 120);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 50);
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Assembly recipe " + recipeId
                        + " must use positive processing_time and energy_per_tick");
            }

            return new AssemblyRecipe(recipeId, ingredients, counts, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public AssemblyRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            int inputCount = buffer.readVarInt();
            if (inputCount < 1 || inputCount > MAX_INPUTS) {
                return null;
            }

            List<Ingredient> ingredients = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (int index = 0; index < inputCount; index++) {
                ingredients.add(Ingredient.fromNetwork(buffer));
                counts.add(buffer.readVarInt());
            }

            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new AssemblyRecipe(recipeId, ingredients, counts, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, AssemblyRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (int index = 0; index < recipe.ingredients.size(); index++) {
                recipe.ingredients.get(index).toNetwork(buffer);
                buffer.writeVarInt(recipe.counts.get(index));
            }
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
