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

public class MixingRecipe implements IRecipe<IInventory>, MachineProcessingRecipe {
    private final ResourceLocation id;
    private final Ingredient primary;
    private final Ingredient secondary;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public MixingRecipe(ResourceLocation id, Ingredient primary, Ingredient secondary, ItemStack result,
                        int processingTime, int energyPerTick) {
        this.id = id;
        this.primary = primary;
        this.secondary = secondary;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return primary.test(inventory.getItem(0)) && secondary.test(inventory.getItem(1));
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(primary);
        ingredients.add(secondary);
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.MIXING_SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return ModRecipes.MIXING_TYPE;
    }

    public Ingredient getPrimary() {
        return primary;
    }

    public Ingredient getSecondary() {
        return secondary;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<MixingRecipe> {
        @Override
        public MixingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("primary") || !json.has("secondary")) {
                throw new JsonSyntaxException("Mixing recipe " + recipeId + " requires primary and secondary ingredients");
            }

            Ingredient primary = Ingredient.fromJson(json.get("primary"));
            Ingredient secondary = Ingredient.fromJson(json.get("secondary"));
            ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(json, "result"));
            int processingTime = JSONUtils.getAsInt(json, "processing_time", 160);
            int energyPerTick = JSONUtils.getAsInt(json, "energy_per_tick", 45);

            if (primary.isEmpty() || secondary.isEmpty()) {
                throw new JsonSyntaxException("Mixing recipe " + recipeId + " has an empty ingredient");
            }
            if (result.isEmpty()) {
                throw new JsonSyntaxException("Mixing recipe " + recipeId + " has an empty result");
            }
            if (processingTime <= 0 || energyPerTick <= 0) {
                throw new JsonSyntaxException("Mixing recipe time and FE/t must be greater than zero in " + recipeId);
            }

            return new MixingRecipe(recipeId, primary, secondary, result, processingTime, energyPerTick);
        }

        @Nullable
        @Override
        public MixingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            Ingredient primary = Ingredient.fromNetwork(buffer);
            Ingredient secondary = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int processingTime = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            return new MixingRecipe(recipeId, primary, secondary, result, processingTime, energyPerTick);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, MixingRecipe recipe) {
            recipe.primary.toNetwork(buffer);
            recipe.secondary.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
