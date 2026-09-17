package dev.bulkcloud0.justguithings.registry;

import dev.bulkcloud0.justguithings.JustGuiThings;
import dev.bulkcloud0.justguithings.recipe.CrusherRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModRecipes {
    public static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, JustGuiThings.MOD_ID);

    public static final IRecipeType<CrusherRecipe> CRUSHING = IRecipeType.register(JustGuiThings.MOD_ID + ":crushing");
    public static final RegistryObject<IRecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER = SERIALIZERS.register("crushing", CrusherRecipe.Serializer::new);

    private ModRecipes() {}
}
