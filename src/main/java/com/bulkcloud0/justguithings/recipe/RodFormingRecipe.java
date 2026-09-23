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

public class RodFormingRecipe extends SingleInputProcessingRecipe {
    public static final int MAX_ENERGY_PER_TICK = 140_000;
    public RodFormingRecipe(ResourceLocation id, Ingredient input, RecipeOutput result, int processingTime, int energyPerTick) {
        super(id, input, result, processingTime, energyPerTick);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.ROD_FORMING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.ROD_FORMING_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<RodFormingRecipe> {
        @Override
        public RodFormingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Rod forming recipe " + recipeId + " is missing ingredient");
            }
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 55);
            if (input.isEmpty()) {
                throw new JsonSyntaxException("Rod forming recipe " + recipeId + " has an empty ingredient");
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Rod forming recipe time and FE/t must be greater than zero in " + recipeId);
            }
            if (energyPerTick > MAX_ENERGY_PER_TICK) {
                throw new JsonSyntaxException("Rod forming recipe energy_per_tick must not exceed "
                        + MAX_ENERGY_PER_TICK + " FE/t in " + recipeId);
            }

            return new RodFormingRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public RodFormingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            if (processingTime <= 0 || energyPerTick <= 0 || energyPerTick > MAX_ENERGY_PER_TICK) {
                return null;
            }
            return new RodFormingRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, RodFormingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
