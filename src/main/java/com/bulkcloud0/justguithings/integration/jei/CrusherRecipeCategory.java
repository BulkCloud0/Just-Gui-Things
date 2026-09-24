package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.CrusherRecipe;
import com.bulkcloud0.justguithings.registry.ModItems;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (!recipe.hasSecondaryResult()) {
            SingleInputRecipeJeiHelper.setIngredients(recipe, ingredients);
            return;
        }

        ingredients.setInputIngredients(Collections.singletonList(recipe.getInput()));
        List<ItemStack> outputs = new ArrayList<>(recipe.getResultDisplayStacks());
        outputs.addAll(recipe.getSecondaryResultDisplayStacks());
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CrusherRecipe recipe, IIngredients ingredients) {
        if (!recipe.hasSecondaryResult()) {
            SingleInputRecipeJeiHelper.setRecipe(recipeLayout, recipe);
            return;
        }

        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 8, 17);
        itemStacks.init(1, false, 80, 8);
        itemStacks.init(2, false, 98, 26);

        DisplayVariants variants = getDisplayVariants(recipe, recipeLayout.getFocus(VanillaTypes.ITEM));
        itemStacks.set(0, variants.inputs);
        itemStacks.set(1, variants.primaryOutputs);
        itemStacks.set(2, variants.secondaryOutputs);

        if (hasProviderVariants(recipe)) {
            itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if ((slotIndex == 1 || slotIndex == 2) && !input) {
                    tooltip.add(new TranslationTextComponent(
                            "jei.justguithings.output_matches_input_provider")
                            .withStyle(TextFormatting.GRAY));
                }
            });
        }
    }

    private static DisplayVariants getDisplayVariants(CrusherRecipe recipe, IFocus<ItemStack> focus) {
        DisplayVariants all = buildAllVariants(recipe);
        if (focus == null) {
            return all;
        }

        ItemStack focused = focus.getValue();
        if (focus.getMode() == IFocus.Mode.INPUT && recipe.getInput().test(focused)) {
            ItemStack primary = recipe.getResultForInput(focused);
            ItemStack secondary = recipe.getSecondaryResultForInput(focused);
            if (!primary.isEmpty()) {
                return new DisplayVariants(
                        Collections.singletonList(focused.copy()),
                        Collections.singletonList(primary),
                        secondary.isEmpty()
                                ? Collections.emptyList()
                                : Collections.singletonList(secondary));
            }
        }

        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> primaryOutputs = new ArrayList<>();
            List<ItemStack> secondaryOutputs = new ArrayList<>();
            for (ItemStack input : recipe.getInputDisplayStacks()) {
                ItemStack primary = recipe.getResultForInput(input);
                ItemStack secondary = recipe.getSecondaryResultForInput(input);
                if (sameStackIdentity(primary, focused) || sameStackIdentity(secondary, focused)) {
                    inputs.add(input);
                    if (!primary.isEmpty()) {
                        primaryOutputs.add(primary);
                    }
                    if (!secondary.isEmpty()) {
                        secondaryOutputs.add(secondary);
                    }
                }
            }
            if (!inputs.isEmpty()) {
                return new DisplayVariants(inputs, primaryOutputs, secondaryOutputs);
            }
        }

        return all;
    }

    private static DisplayVariants buildAllVariants(CrusherRecipe recipe) {
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> primaryOutputs = new ArrayList<>();
        List<ItemStack> secondaryOutputs = new ArrayList<>();

        for (ItemStack input : recipe.getInputDisplayStacks()) {
            ItemStack primary = recipe.getResultForInput(input);
            if (primary.isEmpty()) {
                continue;
            }
            inputs.add(input);
            primaryOutputs.add(primary);

            ItemStack secondary = recipe.getSecondaryResultForInput(input);
            if (!secondary.isEmpty()) {
                secondaryOutputs.add(secondary);
            }
        }

        return new DisplayVariants(inputs, primaryOutputs, secondaryOutputs);
    }

    private static boolean hasProviderVariants(CrusherRecipe recipe) {
        return hasMultipleIdentities(recipe.getResultDisplayStacks())
                || hasMultipleIdentities(recipe.getSecondaryResultDisplayStacks());
    }

    private static boolean hasMultipleIdentities(List<ItemStack> stacks) {
        for (int first = 0; first < stacks.size(); first++) {
            for (int second = first + 1; second < stacks.size(); second++) {
                if (!sameStackIdentity(stacks.get(first), stacks.get(second))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean sameStackIdentity(ItemStack first, ItemStack second) {
        return !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.isSame(first, second)
                && ItemStack.tagMatches(first, second);
    }

    @Override
    public void draw(CrusherRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        String time = recipe.getProcessingTime() + " t";
        String energy = recipe.getEnergyPerTick() + " FE/t";
        minecraft.font.draw(matrixStack, time, 45.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, energy, 39.0F, 31.0F, 0xFF808080);
    }

    private static final class DisplayVariants {
        private final List<ItemStack> inputs;
        private final List<ItemStack> primaryOutputs;
        private final List<ItemStack> secondaryOutputs;

        private DisplayVariants(List<ItemStack> inputs,
                                List<ItemStack> primaryOutputs,
                                List<ItemStack> secondaryOutputs) {
            this.inputs = inputs;
            this.primaryOutputs = primaryOutputs;
            this.secondaryOutputs = secondaryOutputs;
        }
    }
}
