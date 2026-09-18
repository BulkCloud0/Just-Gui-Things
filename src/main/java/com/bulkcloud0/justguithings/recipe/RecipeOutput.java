package com.bulkcloud0.justguithings.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class RecipeOutput {
    private final Ingredient ingredient;
    private final int count;

    private RecipeOutput(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public static RecipeOutput fromJson(JsonObject json) {
        boolean hasItem = json.has("item");
        boolean hasTag = json.has("tag");
        if (hasItem == hasTag) {
            throw new JsonSyntaxException("Recipe result must define exactly one of 'item' or 'tag'");
        }

        JsonObject ingredientJson = new JsonObject();
        if (hasItem) {
            ingredientJson.add("item", json.get("item"));
        } else {
            ingredientJson.add("tag", json.get("tag"));
        }

        int count = JSONUtils.getAsInt(json, "count", 1);
        if (count <= 0) {
            throw new JsonSyntaxException("Recipe result count must be greater than zero");
        }

        return new RecipeOutput(Ingredient.fromJson(ingredientJson), count);
    }

    public static RecipeOutput fromNetwork(PacketBuffer buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        int count = buffer.readVarInt();
        return new RecipeOutput(ingredient, Math.max(1, count));
    }

    public void toNetwork(PacketBuffer buffer) {
        ingredient.toNetwork(buffer);
        buffer.writeVarInt(count);
    }

    public ItemStack resolve() {
        return resolve(null);
    }

    public ItemStack resolve(@Nullable ItemStack context) {
        List<ItemStack> candidates = getDisplayStacks();
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        String preferredNamespace = getNamespace(context);
        if (preferredNamespace != null) {
            for (ItemStack candidate : candidates) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(candidate.getItem());
                if (id != null && preferredNamespace.equals(id.getNamespace())) {
                    return candidate.copy();
                }
            }
        }

        return candidates.get(0).copy();
    }

    public List<ItemStack> getDisplayStacks() {
        return Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparing(RecipeOutput::getRegistryName))
                .map(this::withCount)
                .collect(Collectors.toList());
    }

    private ItemStack withCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();
        result.setCount(count);
        return result;
    }

    @Nullable
    private static String getNamespace(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : id.getNamespace();
    }

    private static String getRegistryName(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }
}
