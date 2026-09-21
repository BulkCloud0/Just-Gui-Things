package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.client.screen.CrusherScreen;
import com.bulkcloud0.justguithings.client.screen.IndustrialMixerScreen;
import com.bulkcloud0.justguithings.client.screen.IndustrialAssemblerScreen;
import com.bulkcloud0.justguithings.client.screen.StampingPressScreen;
import com.bulkcloud0.justguithings.client.screen.ResistiveFurnaceScreen;
import com.bulkcloud0.justguithings.client.screen.RodMillScreen;
import com.bulkcloud0.justguithings.client.screen.WireMillScreen;
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
                new MixingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AssemblyRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new HeatingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RodFormingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new WireDrawingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientWorld world = Minecraft.getInstance().level;
        if (world != null) {
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHING_TYPE), CrusherRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSING_TYPE), PressingRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.MIXING_TYPE), MixingRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE), AssemblyRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.HEATING_TYPE), HeatingRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.ROD_FORMING_TYPE), RodFormingRecipeCategory.UID);
            registration.addRecipes(world.getRecipeManager().getAllRecipesFor(ModRecipes.WIRE_DRAWING_TYPE), WireDrawingRecipeCategory.UID);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CRUSHER.get()), CrusherRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.STAMPING_PRESS.get()), PressingRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.INDUSTRIAL_MIXER.get()), MixingRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.INDUSTRIAL_ASSEMBLER.get()), AssemblyRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.RESISTIVE_FURNACE.get()), HeatingRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ROD_MILL.get()), RodFormingRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(ModItems.WIRE_MILL.get()), WireDrawingRecipeCategory.UID);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CrusherScreen.class, 69, 38, 36, 12, CrusherRecipeCategory.UID);
        registration.addRecipeClickArea(StampingPressScreen.class, 69, 38, 36, 12, PressingRecipeCategory.UID);
        registration.addRecipeClickArea(IndustrialMixerScreen.class, 75, 38, 30, 12, MixingRecipeCategory.UID);
        registration.addRecipeClickArea(IndustrialAssemblerScreen.class, 91, 38, 19, 12, AssemblyRecipeCategory.UID);
        registration.addRecipeClickArea(ResistiveFurnaceScreen.class, 69, 38, 36, 12, HeatingRecipeCategory.UID);
        registration.addRecipeClickArea(RodMillScreen.class, 69, 38, 36, 12, RodFormingRecipeCategory.UID);
        registration.addRecipeClickArea(WireMillScreen.class, 69, 38, 36, 12, WireDrawingRecipeCategory.UID);
    }
}
