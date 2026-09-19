package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.RodFormingRecipe;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;


public final class RodFormingRecipeCategory implements IRecipeCategory<RodFormingRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "rod_forming");
    private final IDrawable background;
    private final IDrawable icon;

    public RodFormingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 52);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.ROD_MILL.get()));
    }

    @Override public ResourceLocation getUid() { return UID; }
    @Override public Class<? extends RodFormingRecipe> getRecipeClass() { return RodFormingRecipe.class; }
    @Override @SuppressWarnings("deprecation") public String getTitle() { return getTitleAsTextComponent().getString(); }
    @Override public ITextComponent getTitleAsTextComponent() { return new TranslationTextComponent("jei.justguithings.rod_forming"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setIngredients(RodFormingRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setIngredients(recipe, ingredients);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, RodFormingRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setRecipe(recipeLayout, recipe);
    }

    @Override
    public void draw(RodFormingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 45.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 39.0F, 31.0F, 0xFF808080);
    }
}
