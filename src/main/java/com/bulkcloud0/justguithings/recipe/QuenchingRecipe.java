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
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class QuenchingRecipe implements IRecipe<IInventory> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final FluidIngredient fluidIngredient;
    private final int fluidAmount;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public QuenchingRecipe(ResourceLocation id, Ingredient input, FluidIngredient fluidIngredient, int fluidAmount,
                           ItemStack result, int processingTime, int energyPerTick) {
        this.id = id;
        this.input = input;
        this.fluidIngredient = fluidIngredient;
        this.fluidAmount = fluidAmount;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return input.test(inventory.getItem(0));
    }

    public boolean matches(ItemStack item, FluidStack fluidStack) {
        return input.test(item)
                && fluidIngredient.test(fluidStack)
                && fluidStack.getAmount() >= fluidAmount;
    }

    public boolean matchesFluid(FluidStack fluidStack) {
        return fluidIngredient.test(fluidStack);
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
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

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.QUENCHING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.QUENCHING_TYPE;
    }

    public Ingredient getInput() {
        return input;
    }

    public FluidIngredient getFluidIngredient() {
        return fluidIngredient;
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<QuenchingRecipe> {
        @Override
        public QuenchingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " is missing ingredient");
            }
            if (!json.has("fluid")) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " is missing fluid");
            }

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            FluidIngredient fluidIngredient = FluidIngredient.fromJson(json.get("fluid"));
            ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(json, "result"));
            int fluidAmount = JSONUtils.getAsInt(json, "fluid_amount", 250);
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 45);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " has an empty ingredient");
            }
            if (fluidAmount <= 0 || processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Fluid amount, processing time and FE/t must be greater than zero in " + recipeId);
            }
            if (result.isEmpty()) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " has an empty result");
            }

            return new QuenchingRecipe(recipeId, input, fluidIngredient, fluidAmount, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public QuenchingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            FluidIngredient fluidIngredient = FluidIngredient.fromNetwork(buffer);
            int fluidAmount = buffer.readVarInt();
            ItemStack result = buffer.readItem();
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new QuenchingRecipe(recipeId, input, fluidIngredient, fluidAmount, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, QuenchingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            recipe.fluidIngredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.fluidAmount);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
