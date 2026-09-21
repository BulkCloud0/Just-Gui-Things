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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AssemblyRecipe implements IRecipe<IInventory>, MachineProcessingRecipe {
    public static final int MAX_INPUTS = 4;

    private final ResourceLocation id;
    private final List<AssemblyInput> inputs;
    private final RecipeOutput result;
    private final int processingTime;
    private final int energyPerTick;

    public AssemblyRecipe(ResourceLocation id, List<AssemblyInput> inputs,
                          RecipeOutput result, int processingTime, int energyPerTick) {
        this.id = id;
        this.inputs = new ArrayList<>(inputs);
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        if (inventory.getContainerSize() < MAX_INPUTS) {
            return false;
        }
        for (int index = 0; index < inputs.size(); index++) {
            AssemblyInput input = inputs.get(index);
            ItemStack stack = inventory.getItem(index);
            if (stack.getCount() < input.count || !input.ingredient.test(stack)) {
                return false;
            }
        }
        for (int index = inputs.size(); index < MAX_INPUTS; index++) {
            if (!inventory.getItem(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return getResultForPrimary(inventory.getItem(0));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= inputs.size();
    }

    @Override
    public ItemStack getResultItem() {
        return result.resolve();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (AssemblyInput input : inputs) {
            ingredients.add(input.ingredient);
        }
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.ASSEMBLING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.ASSEMBLING_TYPE;
    }

    public int getInputCount() {
        return inputs.size();
    }

    public Ingredient getInputIngredient(int index) {
        return index >= 0 && index < inputs.size() ? inputs.get(index).ingredient : Ingredient.EMPTY;
    }

    public int getRequiredCount(int index) {
        return index >= 0 && index < inputs.size() ? inputs.get(index).count : 0;
    }

    public List<ItemStack> getInputDisplayStacks(int index) {
        if (index < 0 || index >= inputs.size()) {
            return java.util.Collections.emptyList();
        }
        AssemblyInput input = inputs.get(index);
        return Arrays.stream(input.ingredient.getItems())
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(stack -> {
                    ResourceLocation name = stack.getItem().getRegistryName();
                    return name == null ? "" : name.toString();
                }))
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(input.count);
                    return copy;
                })
                .collect(Collectors.toList());
    }

    public ItemStack getResultForPrimary(ItemStack primary) {
        return result.resolve(primary);
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

    public static final class AssemblyInput {
        private final Ingredient ingredient;
        private final int count;

        public AssemblyInput(Ingredient ingredient, int count) {
            this.ingredient = ingredient;
            this.count = count;
        }
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<AssemblyRecipe> {
        @Override
        public AssemblyRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("inputs") || !json.get("inputs").isJsonArray()) {
                throw new JsonSyntaxException("Assembly recipe " + recipeId + " requires an inputs array");
            }

            JsonArray inputArray = json.getAsJsonArray("inputs");
            if (inputArray.size() < 1 || inputArray.size() > MAX_INPUTS) {
                throw new JsonSyntaxException("Assembly recipe " + recipeId
                        + " must define between 1 and " + MAX_INPUTS + " inputs");
            }

            List<AssemblyInput> inputs = new ArrayList<>();
            for (JsonElement element : inputArray) {
                if (!element.isJsonObject()) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId + " has a non-object input");
                }
                JsonObject inputJson = element.getAsJsonObject();
                if (!inputJson.has("ingredient")) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId + " input is missing ingredient");
                }
                Ingredient ingredient = Ingredient.fromJson(inputJson.get("ingredient"));
                int count = JSONUtils.getAsInt(inputJson, "count", 1);
                if (ingredient.isEmpty() || count <= 0) {
                    throw new JsonSyntaxException("Assembly recipe " + recipeId
                            + " inputs require a non-empty ingredient and positive count");
                }
                inputs.add(new AssemblyInput(ingredient, count));
            }

            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 60);
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Assembly recipe time and FE/t must be greater than zero in " + recipeId);
            }

            return new AssemblyRecipe(recipeId, inputs, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public AssemblyRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            int inputCount = buffer.readVarInt();
            if (inputCount < 1 || inputCount > MAX_INPUTS) {
                return null;
            }
            List<AssemblyInput> inputs = new ArrayList<>();
            for (int index = 0; index < inputCount; index++) {
                inputs.add(new AssemblyInput(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new AssemblyRecipe(recipeId, inputs, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, AssemblyRecipe recipe) {
            buffer.writeVarInt(recipe.inputs.size());
            for (AssemblyInput input : recipe.inputs) {
                input.ingredient.toNetwork(buffer);
                buffer.writeVarInt(input.count);
            }
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
