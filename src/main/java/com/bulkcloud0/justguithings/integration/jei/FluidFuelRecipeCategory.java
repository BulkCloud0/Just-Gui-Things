package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.FluidFuelRecipe;
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

public final class FluidFuelRecipeCategory implements IRecipeCategory<FluidFuelRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "fluid_fuel");

    private final IDrawable background;
    private final IDrawable icon;

    public FluidFuelRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(120, 52);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.FLUID_GENERATOR.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends FluidFuelRecipe> getRecipeClass() {
        return FluidFuelRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.fluid_fuel");
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
    public void setIngredients(FluidFuelRecipe recipe, IIngredients ingredients) {
        List<FluidStack> fluidInputs = recipe.getFluidDisplayStacks();
        if (!fluidInputs.isEmpty()) {
            ingredients.setInputLists(
                    VanillaTypes.FLUID,
                    Collections.singletonList(fluidInputs));
        }
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, FluidFuelRecipe recipe, IIngredients ingredients) {
        List<FluidStack> fluidInputs = recipe.getFluidDisplayStacks();
        if (fluidInputs.isEmpty()) {
            return;
        }

        IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();
        fluidStacks.setOverrideDisplayFocus(recipeLayout.getFocus(VanillaTypes.FLUID));
        fluidStacks.init(
                0,
                true,
                12,
                3,
                16,
                46,
                recipe.getFluidAmount(),
                true,
                null);
        fluidStacks.set(0, fluidInputs);
    }

    @Override
    public void draw(FluidFuelRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        ITextComponent energy = new TranslationTextComponent(
                "jei.justguithings.fluid_fuel.energy",
                recipe.getEnergy());
        minecraft.font.draw(matrixStack, energy, 38.0F, 22.0F, 0xFF808080);
    }
}
