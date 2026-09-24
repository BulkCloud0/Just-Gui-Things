package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.WashingRecipe;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

public final class WashingRecipeCategory implements IRecipeCategory<WashingRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "washing");

    private final IDrawable background;
    private final IDrawable icon;

    public WashingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 58);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.INDUSTRIAL_WASHER.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends WashingRecipe> getRecipeClass() {
        return WashingRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.washing");
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
    public void setIngredients(WashingRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setIngredients(recipe, ingredients);
        ingredients.setInputLists(
                VanillaTypes.FLUID,
                Collections.singletonList(recipe.getFluidDisplayStacks()));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WashingRecipe recipe, IIngredients ingredients) {
        SingleInputRecipeJeiHelper.setRecipe(recipeLayout, recipe);

        List<FluidStack> fluidInputs = recipe.getFluidDisplayStacks();
        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();
        fluidStacks.setOverrideDisplayFocus(recipeLayout.getFocus(VanillaTypes.FLUID));
        fluidStacks.init(
                0,
                true,
                42,
                5,
                10,
                48,
                recipe.getFluidAmount(),
                true,
                null);
        fluidStacks.set(0, fluidInputs);
    }

    @Override
    public void draw(WashingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 58.0F, 14.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 55.0F, 35.0F, 0xFF808080);
    }
}
