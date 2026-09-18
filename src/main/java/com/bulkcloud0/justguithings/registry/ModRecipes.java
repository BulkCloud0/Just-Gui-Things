package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
import com.bulkcloud0.justguithings.recipe.HeatingRecipe;
import com.bulkcloud0.justguithings.recipe.ExtrudingRecipe;
import com.bulkcloud0.justguithings.recipe.WireDrawingRecipe;
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
    public static final IRecipeType<MixingRecipe> MIXING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":mixing");
    public static final IRecipeType<HeatingRecipe> HEATING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":heating");
    public static final IRecipeType<ExtrudingRecipe> EXTRUDING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":extruding");
    public static final IRecipeType<WireDrawingRecipe> WIRE_DRAWING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":wire_drawing");

    public static final RegistryObject<IRecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrusherRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pressing", PressingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<MixingRecipe>> MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixing", MixingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<HeatingRecipe>> HEATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("heating", HeatingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<ExtrudingRecipe>> EXTRUDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("extruding", ExtrudingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<WireDrawingRecipe>> WIRE_DRAWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("wire_drawing", WireDrawingRecipe.Serializer::new);

    private ModRecipes() {}
}
