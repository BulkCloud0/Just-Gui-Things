package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class PressingRecipe extends SingleInputProcessingRecipe {
    public static final int MAX_ENERGY_PER_TICK = 100_000;
    public static final int MAX_INPUT_COUNT = 64;
    private final int inputCount;

    public PressingRecipe(ResourceLocation id,
                          Ingredient input,
                          RecipeOutput result,
                          int inputCount,
                          int processingTime,
                          int energyPerTick) {
        super(id, input, result, processingTime, energyPerTick);
        this.inputCount = inputCount;
    }

    public int getInputCount() {
        return inputCount;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.PRESSING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.PRESSING_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<PressingRecipe> {
        @Override
        public PressingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Pressing recipe " + recipeId + " is missing ingredient");
            }

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int inputCount = JSONUtils.getAsInt(json, "input_count", 1);
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 120);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 30);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Pressing recipe " + recipeId + " has an empty ingredient");
            }
            if (inputCount <= 0 || inputCount > MAX_INPUT_COUNT) {
                throw new JsonSyntaxException("input_count must be between 1 and 64 in " + recipeId);
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Pressing recipe time and FE/t must be greater than zero in " + recipeId);
            }

            if (energyPerTick > MAX_ENERGY_PER_TICK) {
                throw new JsonSyntaxException("Pressing recipe energy_per_tick must not exceed "
                        + MAX_ENERGY_PER_TICK + " FE/t in " + recipeId);
            }

            return new PressingRecipe(recipeId, input, result, inputCount, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public PressingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int inputCount = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            if (input.isEmpty() || inputCount <= 0 || inputCount > MAX_INPUT_COUNT
                    || processingTime <= 0 || energyPerTick <= 0
                    || energyPerTick > MAX_ENERGY_PER_TICK) {
                return null;
            }
            return new PressingRecipe(recipeId, input, result, inputCount, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, PressingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
