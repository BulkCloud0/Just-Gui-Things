package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.recipe.PressingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModRecipes {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, JustGuiThings.MOD_ID);

    public static final IRecipeType<CrusherRecipe> CRUSHING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":crushing");
    public static final IRecipeType<PressingRecipe> PRESSING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":pressing");

    public static final RegistryObject<IRecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrusherRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pressing", PressingRecipe.Serializer::new);

    private ModRecipes() {}
}
