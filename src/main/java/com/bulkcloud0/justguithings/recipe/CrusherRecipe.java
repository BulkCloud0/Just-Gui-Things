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
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class CrusherRecipe implements IRecipe<IInventory> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public CrusherRecipe(ResourceLocation id, Ingredient input, ItemStack result, int processingTime, int energyPerTick) {
        this.id = id;
        this.input = input;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return input.test(inventory.getItem(0));
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
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.CRUSHING_TYPE;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CrusherRecipe> {
        @Override
        public CrusherRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonSyntaxException("Crusher recipe " + recipeId + " is missing ingredient");
            }

            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 100);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 20);

            if (input.isEmpty()) {
                throw new JsonSyntaxException("Crusher recipe " + recipeId + " has an empty ingredient");
            }
            if (result.isEmpty()) {
                throw new JsonSyntaxException("Crusher recipe " + recipeId + " has an empty result");
            }
            if (processingTime <= 0) {
                throw new JsonSyntaxException("processing_time must be greater than zero in " + recipeId);
            }
            if (energyPerTick <= 0) {
                throw new JsonSyntaxException("energy_per_tick must be greater than zero in " + recipeId);
            }

            return new CrusherRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public CrusherRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new CrusherRecipe(recipeId, input, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, CrusherRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
