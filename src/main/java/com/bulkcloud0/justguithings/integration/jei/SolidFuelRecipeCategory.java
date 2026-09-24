package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.SolidFuelRecipe;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collections;

public final class SolidFuelRecipeCategory implements IRecipeCategory<SolidFuelRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "solid_fuel");

    private final IDrawable background;
    private final IDrawable icon;

    public SolidFuelRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 52);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.COAL_GENERATOR.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends SolidFuelRecipe> getRecipeClass() {
        return SolidFuelRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.solid_fuel");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(SolidFuelRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(Collections.singletonList(recipe.getIngredient()));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, SolidFuelRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 12, 17);
        itemStacks.set(0, recipe.getDisplayStacks());
    }

    @Override
    public void draw(SolidFuelRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        ITextComponent energy = new TranslationTextComponent(
                "jei.justguithings.solid_fuel.energy",
                recipe.getEnergy());
        minecraft.font.draw(matrixStack, energy, 38.0F, 22.0F, 0xFF808080);
    }
}
