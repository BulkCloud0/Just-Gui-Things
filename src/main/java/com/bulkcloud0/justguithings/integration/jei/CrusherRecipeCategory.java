package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
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


public final class CrusherRecipeCategory implements IRecipeCategory<CrusherRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "crushing");

    private final IDrawable background;
    private final IDrawable icon;

    public CrusherRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(120, 52);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.CRUSHER.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends CrusherRecipe> getRecipeClass() {
        return CrusherRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.crushing");
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
    public void setIngredients(CrusherRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setIngredients(recipe, ingredients);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CrusherRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setRecipe(recipeLayout, recipe);
    }

    @Override
    public void draw(CrusherRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        String time = recipe.getProcessingTime() + " t";
        String energy = recipe.getEnergyPerTick() + " FE/t";
        minecraft.font.draw(matrixStack, time, 45.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, energy, 39.0F, 31.0F, 0xFF808080);
    }
}
