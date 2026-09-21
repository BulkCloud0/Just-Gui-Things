package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.AssemblyRecipe;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
import com.bulkcloud0.justguithings.recipe.HeatingRecipe;
import com.bulkcloud0.justguithings.recipe.RodFormingRecipe;
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

    public static final IRecipeType<AssemblyRecipe> ASSEMBLING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":assembling");
    public static final IRecipeType<CrusherRecipe> CRUSHING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":crushing");
    public static final IRecipeType<PressingRecipe> PRESSING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":pressing");
    public static final IRecipeType<MixingRecipe> MIXING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":mixing");
    public static final IRecipeType<HeatingRecipe> HEATING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":heating");
    public static final IRecipeType<RodFormingRecipe> ROD_FORMING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":rod_forming");
    public static final IRecipeType<WireDrawingRecipe> WIRE_DRAWING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":wire_drawing");

    public static final RegistryObject<IRecipeSerializer<AssemblyRecipe>> ASSEMBLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("assembling", AssemblyRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrusherRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pressing", PressingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<MixingRecipe>> MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixing", MixingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<HeatingRecipe>> HEATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("heating", HeatingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<RodFormingRecipe>> ROD_FORMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("rod_forming", RodFormingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<WireDrawingRecipe>> WIRE_DRAWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("wire_drawing", WireDrawingRecipe.Serializer::new);

    private ModRecipes() {}
}
