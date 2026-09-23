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
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SolidFuelRecipe implements IRecipe<IInventory> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int energy;

    public SolidFuelRecipe(ResourceLocation id, Ingredient ingredient, int energy) {
        this.id = id;
        this.ingredient = ingredient;
        this.energy = energy;
    }

    public boolean matchesStack(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    public int getEnergy() {
        return energy;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public List<ItemStack> getDisplayStacks() {
        return Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(stack -> {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    return id == null ? "" : id.toString();
                }))
                .map(ItemStack::copy)
                .collect(Collectors.toList());
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return inventory.getContainerSize() > 0 && matchesStack(inventory.getItem(0));
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
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.SOLID_FUEL_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.SOLID_FUEL_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<SolidFuelRecipe> {
        @Override
        public SolidFuelRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Solid fuel recipe " + recipeId + " is missing ingredient");
            }
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            int energy = JSONUtils.getAsInt(json, "energy");

            if (ingredient.isEmpty()) {
                throw new JsonSyntaxException("Solid fuel recipe " + recipeId + " has an empty ingredient");
            }
            if (energy <= 0) {
                throw new JsonSyntaxException("Solid fuel energy must be greater than zero in " + recipeId);
            }

            return new SolidFuelRecipe(recipeId, ingredient, energy);
        }

        @Nullable
        @Override
        public SolidFuelRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int energy = buffer.readVarInt();
            return new SolidFuelRecipe(recipeId, ingredient, energy);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, SolidFuelRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.energy);
        }
    }
}
