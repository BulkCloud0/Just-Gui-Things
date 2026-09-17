package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.client.screen.CrusherScreen;
import com.bulkcloud0.justguithings.client.screen.IndustrialMixerScreen;
import com.bulkcloud0.justguithings.client.screen.StampingPressScreen;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.bulkcloud0.justguithings.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

@JeiPlugin
public final class JustGuiThingsJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = new ResourceLocation(JustGuiThings.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrusherRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MixingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientWorld world = Minecraft.getInstance().level;
        if (world != null) {
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHING_TYPE), CrusherRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSING_TYPE), PressingRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE), MixingRecipeCategory.UID);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRUSHER.get()), CrusherRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.STAMPING_PRESS.get()), PressingRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.INDUSTRIAL_MIXER.get()), MixingRecipeCategory.UID);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CrusherScreen.class, 69, 38, 36, 12, CrusherRecipeCategory.UID);
        registration.addRecipeClickArea(StampingPressScreen.class, 69, 38, 36, 12, PressingRecipeCategory.UID);
        registration.addRecipeClickArea(IndustrialMixerScreen.class, 75, 38, 30, 12, MixingRecipeCategory.UID);
    }
}
