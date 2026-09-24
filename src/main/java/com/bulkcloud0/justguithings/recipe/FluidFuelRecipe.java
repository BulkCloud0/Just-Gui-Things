package com.bulkcloud0.justguithings.recipe;

import com.bulkcloud0.justguithings.registry.ModRecipes;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;

public class FluidFuelRecipe implements IRecipe<IInventory> {
    public static final int MAX_FLUID_AMOUNT = 16_000;
    private final ResourceLocation id;
    private final FluidIngredient fluidIngredient;
    private final int fluidAmount;
    private final int energy;

    public FluidFuelRecipe(ResourceLocation id, FluidIngredient fluidIngredient, int fluidAmount, int energy) {
        this.id = id;
        this.fluidIngredient = fluidIngredient;
        this.fluidAmount = fluidAmount;
        this.energy = energy;
    }

    public boolean matchesFluid(FluidStack stack) {
        return stack.getAmount() >= fluidAmount && fluidIngredient.test(stack);
    }

    public boolean matchesFluidType(Fluid fluid) {
        return fluidIngredient.test(fluid);
    }

    public boolean isTagBased() {
        return fluidIngredient.isTagBased();
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getEnergy() {
        return energy;
    }

    public List<FluidStack> getFluidDisplayStacks() {
        return fluidIngredient.getDisplayStacks(fluidAmount);
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return false;
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<net.minecraft.item.crafting.Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.FLUID_FUEL_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.FLUID_FUEL_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<FluidFuelRecipe> {
        @Override
        public FluidFuelRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("fluid")) {
                throw new JsonSyntaxException("Fluid fuel recipe " + recipeId + " is missing fluid");
            }

            JsonObject fluidJson = JSONUtils.getAsJsonObject(json, "fluid");
            FluidIngredient fluidIngredient = FluidIngredient.fromJson(recipeId, fluidJson);
            int fluidAmount = JSONUtils.getAsInt(fluidJson, "amount", 1000);
            int energy = JSONUtils.getAsInt(json, "energy");

            if (fluidAmount <= 0 || fluidAmount > MAX_FLUID_AMOUNT) {
                throw new JsonSyntaxException("Fluid fuel amount must be between 1 and "
                        + MAX_FLUID_AMOUNT + " mB in " + recipeId);
            }
            if (energy <= 0) {
                throw new JsonSyntaxException("Fluid fuel energy must be greater than zero in " + recipeId);
            }

            return new FluidFuelRecipe(recipeId, fluidIngredient, fluidAmount, energy);
        }

        @Nullable
        @Override
        public FluidFuelRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            FluidIngredient fluidIngredient = FluidIngredient.fromNetwork(buffer);
            int fluidAmount = buffer.readVarInt();
            int energy = buffer.readVarInt();
            if (fluidAmount <= 0 || fluidAmount > MAX_FLUID_AMOUNT || energy <= 0) {
                return null;
            }
            return new FluidFuelRecipe(recipeId, fluidIngredient, fluidAmount, energy);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, FluidFuelRecipe recipe) {
            recipe.fluidIngredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.fluidAmount);
            buffer.writeVarInt(recipe.energy);
        }
    }
}
