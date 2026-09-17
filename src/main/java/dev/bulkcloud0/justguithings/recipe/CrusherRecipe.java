package dev.bulkcloud0.justguithings.recipe;

import com.google.gson.JsonObject;
import dev.bulkcloud0.justguithings.registry.ModRecipes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class CrusherRecipe implements IRecipe<Inventory> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public CrusherRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override public boolean matches(Inventory inventory, World world) { return ingredient.test(inventory.getItem(0)); }
    @Override public ItemStack assemble(Inventory inventory) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem() { return result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public IRecipeSerializer<?> getSerializer() { return ModRecipes.CRUSHING_SERIALIZER.get(); }
    @Override public IRecipeType<?> getType() { return ModRecipes.CRUSHING; }

    public int getProcessingTime() { return processingTime; }
    public int getEnergyPerTick() { return energyPerTick; }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CrusherRecipe> {
        @Override
        public CrusherRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(json, "result"));
            int time = JSONUtils.getAsInt(json, "processing_time", 100);
            int energy = JSONUtils.getAsInt(json, "energy_per_tick", 20);
            return new CrusherRecipe(id, input, result, time, energy);
        }

        @Nullable
        @Override
        public CrusherRecipe fromNetwork(ResourceLocation id, PacketBuffer buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int time = buffer.readVarInt();
            int energy = buffer.readVarInt();
            return new CrusherRecipe(id, input, result, time, energy);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, CrusherRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energyPerTick);
        }
    }
}
