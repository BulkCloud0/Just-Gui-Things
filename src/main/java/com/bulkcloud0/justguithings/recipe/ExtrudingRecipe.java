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

public class ExtrudingRecipe extends SingleInputProcessingRecipe {
    public ExtrudingRecipe(ResourceLocation id, Ingredient input, RecipeOutput result, int processingTime, int energyPerTick) {
        super(id, input, result, processingTime, energyPerTick);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.EXTRUDING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.EXTRUDING_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ExtrudingRecipe> {
        @Override
        public ExtrudingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Extruding recipe " + recipeId + " is missing ingredient");
            }

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 55);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Extruding recipe " + recipeId + " has an empty ingredient");
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Extruding recipe time and FE/t must be greater than zero in " + recipeId);
            }

            return new ExtrudingRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public ExtrudingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new ExtrudingRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, ExtrudingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
