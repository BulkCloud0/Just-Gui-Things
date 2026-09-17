package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
import com.bulkcloud0.justguithings.recipe.PressingRecipe;
import com.bulkcloud0.justguithings.recipe.QuenchingRecipe;
import com.bulkcloud0.justguithings.recipe.ThermalRecipe;
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
    public static final IRecipeType<QuenchingRecipe> QUENCHING_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":quenching");
    public static final IRecipeType<ThermalRecipe> THERMAL_TYPE =
            IRecipeType.register(JustGuiThings.MOD_ID + ":thermal_processing");

    public static final RegistryObject<IRecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrusherRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<PressingRecipe>> PRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pressing", PressingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<MixingRecipe>> MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixing", MixingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<QuenchingRecipe>> QUENCHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("quenching", QuenchingRecipe.Serializer::new);
    public static final RegistryObject<IRecipeSerializer<ThermalRecipe>> THERMAL_SERIALIZER =
            RECIPE_SERIALIZERS.register("thermal_processing", ThermalRecipe.Serializer::new);

    private ModRecipes() {}
}
