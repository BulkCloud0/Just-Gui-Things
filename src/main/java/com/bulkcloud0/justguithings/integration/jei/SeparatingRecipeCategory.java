package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.SeparatingRecipe;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SeparatingRecipeCategory implements IRecipeCategory<SeparatingRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "separating");

    private final IDrawable background;
    private final IDrawable icon;

    public SeparatingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(130, 52);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.INDUSTRIAL_SEPARATOR.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends SeparatingRecipe> getRecipeClass() {
        return SeparatingRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.separating");
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
    public void setIngredients(SeparatingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(Collections.singletonList(recipe.getInput()));
        List<ItemStack> outputs = new ArrayList<>();
        for (int index = 0; index < recipe.getOutputCount(); index++) {
            outputs.addAll(recipe.getOutputDisplayStacks(index));
        }
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, SeparatingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 8, 17);
        itemStacks.set(0, recipe.getCountedInputDisplayStacks());

        int[][] outputPositions = {
                {88, 0},
                {106, 17},
                {88, 34}
        };
        for (int index = 0; index < recipe.getOutputCount(); index++) {
            itemStacks.init(index + 1, false, outputPositions[index][0], outputPositions[index][1]);
            itemStacks.set(index + 1, recipe.getOutputDisplayStacks(index));
        }
    }

    @Override
    public void draw(SeparatingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 43.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 37.0F, 31.0F, 0xFF808080);
    }
}
