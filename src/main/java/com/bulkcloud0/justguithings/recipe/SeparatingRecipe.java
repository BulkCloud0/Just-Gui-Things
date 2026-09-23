package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SeparatingRecipe extends SingleInputProcessingRecipe {
    private final int inputCount;
    private final List<RecipeOutput> outputs;

    public SeparatingRecipe(ResourceLocation id,
                            Ingredient input,
                            int inputCount,
                            List<RecipeOutput> outputs,
                            int processingTime,
                            int energyPerTick) {
        super(id, input, outputs.get(0), processingTime, energyPerTick);
        this.inputCount = inputCount;
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getOutputCount() {
        return outputs.size();
    }

    public ItemStack getOutputForInput(int index, ItemStack inputStack) {
        if (index < 0 || index >= outputs.size()) {
            return ItemStack.EMPTY;
        }
        return outputs.get(index).resolve(inputStack);
    }

    public List<ItemStack> getOutputDisplayStacks(int index) {
        if (index < 0 || index >= outputs.size()) {
            return Collections.emptyList();
        }
        return outputs.get(index).getDisplayStacks();
    }

    public List<ItemStack> getCountedInputDisplayStacks() {
        return getInputDisplayStacks().stream()
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(inputCount);
                    return copy;
                })
                .collect(Collectors.toList());
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.SEPARATING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.SEPARATING_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<SeparatingRecipe> {
        @Override
        public SeparatingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient") || !json.has("results")) {
                throw new JsonSyntaxException("Separating recipe " + recipeId + " requires ingredient and results");
            }

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            int inputCount = JSONUtils.getAsInt(json, "input_count", 1);
            if (input.isEmpty()) {
                throw new JsonSyntaxException("Separating recipe " + recipeId + " has an empty ingredient");
            }
            if (inputCount <= 0 || inputCount > 64) {
                throw new JsonSyntaxException("Separating input_count must be between 1 and 64 in " + recipeId);
            }

            JsonArray resultsJson = json.getAsJsonArray("results");
            if (resultsJson == null || resultsJson.size() < 2 || resultsJson.size() > 3) {
                throw new JsonSyntaxException("Separating recipe " + recipeId + " requires exactly 2 or 3 results");
            }

            List<RecipeOutput> outputs = new ArrayList<>();
            for (JsonElement element : resultsJson) {
                if (!element.isJsonObject()) {
                    throw new JsonSyntaxException("Separating result entries must be objects in " + recipeId);
                }
                outputs.add(RecipeOutput.fromJson(element.getAsJsonObject()));
            }

            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 45);
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Separating recipe time and FE/t must be greater than zero in " + recipeId);
            }

            return new SeparatingRecipe(recipeId, input, inputCount, outputs, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public SeparatingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int inputCount = buffer.readVarInt();
            int outputCount = buffer.readVarInt();
            List<RecipeOutput> outputs = new ArrayList<>();
            for (int index = 0; index < outputCount; index++) {
                outputs.add(RecipeOutput.fromNetwork(buffer));
            }
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new SeparatingRecipe(recipeId, input, inputCount, outputs, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, SeparatingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.outputs.size());
            for (RecipeOutput output : recipe.outputs) {
                output.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
