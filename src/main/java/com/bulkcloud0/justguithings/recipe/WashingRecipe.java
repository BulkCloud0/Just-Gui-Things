package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;

public class WashingRecipe extends SingleInputProcessingRecipe {
    private final FluidIngredient fluidIngredient;
    private final int fluidAmount;

    public WashingRecipe(ResourceLocation id,
                         Ingredient input,
                         FluidIngredient fluidIngredient,
                         int fluidAmount,
                         RecipeOutput result,
                         int processingTime,
                         int energyPerTick) {
        super(id, input, result, processingTime, energyPerTick);
        this.fluidIngredient = fluidIngredient;
        this.fluidAmount = fluidAmount;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.WASHING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.WASHING_TYPE;
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public boolean matchesFluid(FluidStack stack) {
        return stack.getAmount() >= fluidAmount && fluidIngredient.test(stack);
    }

    public boolean matchesFluidType(Fluid fluid) {
        return fluidIngredient.test(fluid);
    }

    public boolean isFluidTagBased() {
        return fluidIngredient.isTagBased();
    }

    public List<FluidStack> getFluidDisplayStacks() {
        return fluidIngredient.getDisplayStacks(fluidAmount);
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<WashingRecipe> {
        @Override
        public WashingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("input") || !json.has("fluid") || !json.has("result")) {
                throw new JsonSyntaxException("Washing recipe " + recipeId
                        + " requires input, fluid and result");
            }

            Ingredient input = Ingredient.fromJson(json.get("input"));
            JsonObject fluidJson = JSONUtils.getAsJsonObject(json, "fluid");
            FluidIngredient fluidIngredient = FluidIngredient.fromJson(recipeId, fluidJson);
            int fluidAmount = JSONUtils.getAsInt(fluidJson, "amount", 1000);
            RecipeOutput result = RecipeOutput.fromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 100);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 30);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Washing recipe " + recipeId + " has an empty input");
            }
            if (fluidAmount <= 0 || fluidAmount > MAX_FLUID_AMOUNT) {
                throw new JsonSyntaxException("Washing recipe fluid amount must be between 1 and "
                        + MAX_FLUID_AMOUNT + " mB in " + recipeId);
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Washing recipe time and FE/t must be greater than zero in "
                        + recipeId);
            }

            return new WashingRecipe(recipeId, input, fluidIngredient, fluidAmount,
                    result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public WashingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            FluidIngredient fluidIngredient = FluidIngredient.fromNetwork(buffer);
            int fluidAmount = buffer.readVarInt();
            RecipeOutput result = RecipeOutput.fromNetwork(buffer);
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            if (input.isEmpty() || fluidAmount <= 0 || fluidAmount > MAX_FLUID_AMOUNT) {
                return null;
            }
            return new WashingRecipe(recipeId, input, fluidIngredient, fluidAmount,
                    result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, WashingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.fluidIngredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.fluidAmount);
            recipe.result.toNetwork(buffer);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
