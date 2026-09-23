package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
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

public class SawingRecipe extends SingleInputProcessingRecipe {
    @Nullable
    private final RecipeOutput secondaryResult;

    public SawingRecipe(ResourceLocation id,
                        Ingredient input,
                        RecipeOutput result,
                        @Nullable RecipeOutput secondaryResult,
                        int processingTime,
                        int energyPerTick) {
        super(id, input, result, processingTime, energyPerTick);
        this.secondaryResult = secondaryResult;
    }

    public boolean hasSecondaryResult() {
        return secondaryResult != null;
    }

    public ItemStack getSecondaryResultForInput(ItemStack inputStack) {
        return secondaryResult == null ? ItemStack.EMPTY : secondaryResult.resolve(inputStack);
    }

    public List<ItemStack> getSecondaryResultDisplayStacks() {
        if (secondaryResult == null) {
            return Collections.emptyList();
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack inputStack : getInputDisplayStacks()) {
            ItemStack output = getSecondaryResultForInput(inputStack);
            if (!output.isEmpty()) {
                outputs.add(output);
            }
        }
        return outputs;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.SAWING_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<SawingRecipe> {
        @Override
        public SawingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient") || !json.has("result")) {
                throw new JsonSyntaxException("Sawing recipe " + recipeId + " requires ingredient and result");
            }
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            RecipeOutput secondary = json.has("secondary_result")
                    ? RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "secondary_result"))
                    : null;
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 80);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 25);
            if (input.isEmpty()) {
                throw new JsonSyntaxException("Sawing recipe " + recipeId + " has an empty ingredient");
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Sawing recipe time and FE/t must be greater than zero in " + recipeId);
            }
            return new SawingRecipe(recipeId, input, result, secondary, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public SawingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            RecipeOutput secondary = buffer.readBoolean() ? RecipeOutput.fromNetwork(buffer) : null;
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new SawingRecipe(recipeId, input, result, secondary, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, SawingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.result.toNetwork(buffer);
            buffer.writeBoolean(recipe.secondaryResult != null);
            if (recipe.secondaryResult != null) {
                recipe.secondaryResult.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
