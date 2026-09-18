package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.WireDrawingRecipe;
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

import java.util.Arrays;
import java.util.Collections;

public final class WireDrawingRecipeCategory implements IRecipeCategory<WireDrawingRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "wire_drawing");

    private final IDrawable background;
    private final IDrawable icon;

    public WireDrawingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 52);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.WIRE_MILL.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends WireDrawingRecipe> getRecipeClass() {
        return WireDrawingRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.wire_drawing");
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
    public void setIngredients(WireDrawingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(Collections.singletonList(recipe.getInput()));
        ingredients.setOutputs(VanillaTypes.ITEM, recipe.getResultDisplayStacks());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, WireDrawingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        stacks.init(0, true, 8, 17);
        stacks.init(1, false, 94, 17);
        stacks.set(0, Arrays.asList(recipe.getInput().getItems()));
        stacks.set(1, recipe.getResultDisplayStacks());
    }

    @Override
    public void draw(WireDrawingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 45.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 39.0F, 31.0F, 0xFF808080);
    }
}
