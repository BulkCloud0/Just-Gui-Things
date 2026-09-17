package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class QuenchingRecipe implements IRecipe<IInventory> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final Fluid fluid;
    private final int fluidAmount;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public QuenchingRecipe(ResourceLocation id, Ingredient input, Fluid fluid, int fluidAmount,
                           ItemStack result, int processingTime, int energyPerTick) {
        this.id = id;
        this.input = input;
        this.fluid = fluid;
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
                && !fluidStack.isEmpty()
                && fluidStack.getFluid() == fluid
                && fluidStack.getAmount() >= fluidAmount;
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

    public Fluid getFluid() {
        return fluid;
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

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            ResourceLocation fluidId = new ResourceLocation(JSONUtils.getAsString(json, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(json, "result"));
            int fluidAmount = JSONUtils.getAsInt(json, "fluid_amount", 250);
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 45);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " has an empty ingredient");
            }
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException("Unknown fluid " + fluidId + " in " + recipeId);
            }
            if (fluidAmount <= 0 || processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Fluid amount, processing time and FE/t must be greater than zero in " + recipeId);
            }
            if (result.isEmpty()) {
                throw new JsonSyntaxException("Quenching recipe " + recipeId + " has an empty result");
            }

            return new QuenchingRecipe(recipeId, input, fluid, fluidAmount, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public QuenchingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ResourceLocation fluidId = buffer.readResourceLocation();
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null) {
                fluid = Fluids.EMPTY;
            }
            int fluidAmount = buffer.readVarInt();
            ItemStack result = buffer.readItem();
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new QuenchingRecipe(recipeId, input, fluid, fluidAmount, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, QuenchingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(recipe.fluid);
            buffer.writeResourceLocation(fluidId);
            buffer.writeVarInt(recipe.fluidAmount);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
