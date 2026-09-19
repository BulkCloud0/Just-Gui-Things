package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.MixingRecipe;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MixingRecipeCategory implements IRecipeCategory<MixingRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "mixing");

    private final IDrawable background;
    private final IDrawable icon;

    public MixingRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(120, 52);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.INDUSTRIAL_MIXER.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends MixingRecipe> getRecipeClass() {
        return MixingRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.mixing");
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
    public void setIngredients(MixingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(Arrays.asList(recipe.getPrimary(), recipe.getSecondary()));
        ingredients.setOutputs(VanillaTypes.ITEM, recipe.getResultDisplayStacks());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, MixingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 8, 7);
        itemStacks.init(1, true, 8, 28);
        itemStacks.init(2, false, 94, 17);

        DisplayVariants variants = getDisplayVariants(recipe, recipeLayout.getFocus(VanillaTypes.ITEM));
        itemStacks.set(0, variants.primaryInputs);
        itemStacks.set(1, variants.secondaryInputs);
        itemStacks.set(2, variants.outputs);

        if (hasMultipleOutputVariants(recipe)) {
            itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if (slotIndex == 2 && !input) {
                    tooltip.add(new TranslationTextComponent(
                            "jei.justguithings.output_matches_input_provider")
                            .withStyle(TextFormatting.GRAY));
                }
            });
        }
    }

    private static DisplayVariants getDisplayVariants(MixingRecipe recipe, IFocus<ItemStack> focus) {
        DisplayVariants allVariants = buildAllVariants(recipe);
        if (focus == null) {
            return allVariants;
        }

        ItemStack focused = focus.getValue();
        if (focus.getMode() == IFocus.Mode.INPUT) {
            if (recipe.getPrimary().test(focused)) {
                ItemStack primary = withCount(focused, recipe.getPrimaryCount());
                ItemStack output = recipe.getResultForPrimary(focused);
                if (!output.isEmpty()) {
                    return new DisplayVariants(
                            Collections.singletonList(primary),
                            recipe.getSecondaryDisplayStacks(),
                            Collections.singletonList(output));
                }
            }

            if (recipe.getSecondary().test(focused)) {
                return new DisplayVariants(
                        allVariants.primaryInputs,
                        Collections.singletonList(withCount(focused, recipe.getSecondaryCount())),
                        allVariants.outputs);
            }
        }

        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            List<ItemStack> primaryInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (int index = 0; index < allVariants.outputs.size(); index++) {
                ItemStack output = allVariants.outputs.get(index);
                if (sameStackIdentity(output, focused)) {
                    primaryInputs.add(allVariants.primaryInputs.get(index));
                    outputs.add(output);
                }
            }
            if (!primaryInputs.isEmpty()) {
                return new DisplayVariants(primaryInputs, allVariants.secondaryInputs, outputs);
            }
        }

        return allVariants;
    }

    private static DisplayVariants buildAllVariants(MixingRecipe recipe) {
        List<ItemStack> primaryInputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack primary : recipe.getPrimaryDisplayStacks()) {
            ItemStack output = recipe.getResultForPrimary(primary);
            if (output.isEmpty()) {
                continue;
            }
            primaryInputs.add(primary);
            outputs.add(output);
        }
        return new DisplayVariants(primaryInputs, recipe.getSecondaryDisplayStacks(), outputs);
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static boolean sameStackIdentity(ItemStack first, ItemStack second) {
        return ItemStack.isSame(first, second) && ItemStack.tagMatches(first, second);
    }

    private static boolean hasMultipleOutputVariants(MixingRecipe recipe) {
        List<ItemStack> outputs = recipe.getResultDisplayStacks();
        for (int first = 0; first < outputs.size(); first++) {
            for (int second = first + 1; second < outputs.size(); second++) {
                if (!sameStackIdentity(outputs.get(first), outputs.get(second))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void draw(MixingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 46.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 40.0F, 31.0F, 0xFF808080);
    }

    private static final class DisplayVariants {
        private final List<ItemStack> primaryInputs;
        private final List<ItemStack> secondaryInputs;
        private final List<ItemStack> outputs;

        private DisplayVariants(List<ItemStack> primaryInputs,
                                List<ItemStack> secondaryInputs,
                                List<ItemStack> outputs) {
            this.primaryInputs = primaryInputs;
            this.secondaryInputs = secondaryInputs;
            this.outputs = outputs;
        }
    }
}
