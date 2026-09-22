package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.recipe.SingleInputProcessingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SingleInputRecipeJeiHelper {
    private SingleInputRecipeJeiHelper() {
    }

    static void setIngredients(SingleInputProcessingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(Collections.singletonList(recipe.getInput()));
        ingredients.setOutputs(VanillaTypes.ITEM, recipe.getResultDisplayStacks());
    }

    static void setRecipe(IRecipeLayout recipeLayout, SingleInputProcessingRecipe recipe) {
        setRecipe(recipeLayout, recipe, 1);
    }

    static void setRecipe(IRecipeLayout recipeLayout,
                          SingleInputProcessingRecipe recipe,
                          int inputCount) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 8, 17);
        itemStacks.init(1, false, 94, 17);

        DisplayPairs pairs = getDisplayPairs(
                recipe,
                recipeLayout.getFocus(VanillaTypes.ITEM),
                Math.max(1, inputCount));
        itemStacks.set(0, pairs.inputs);
        itemStacks.set(1, pairs.outputs);

        if (hasMultipleOutputVariants(recipe)) {
            itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if (slotIndex == 1 && !input) {
                    tooltip.add(new TranslationTextComponent(
                            "jei.justguithings.output_matches_input_provider")
                            .withStyle(TextFormatting.GRAY));
                }
            });
        }
    }

    private static DisplayPairs getDisplayPairs(SingleInputProcessingRecipe recipe,
                                                IFocus<ItemStack> focus,
                                                int inputCount) {
        DisplayPairs allPairs = buildAllPairs(recipe, inputCount);
        if (focus == null) {
            return allPairs;
        }

        ItemStack focused = focus.getValue();
        if (focus.getMode() == IFocus.Mode.INPUT && recipe.getInput().test(focused)) {
            ItemStack output = recipe.getResultForInput(focused);
            if (!output.isEmpty()) {
                ItemStack countedInput = focused.copy();
                countedInput.setCount(inputCount);
                return new DisplayPairs(
                        Collections.singletonList(countedInput),
                        Collections.singletonList(output));
            }
        }

        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (int i = 0; i < allPairs.outputs.size(); i++) {
                ItemStack output = allPairs.outputs.get(i);
                if (sameStackIdentity(output, focused)) {
                    inputs.add(allPairs.inputs.get(i));
                    outputs.add(output);
                }
            }
            if (!inputs.isEmpty()) {
                return new DisplayPairs(inputs, outputs);
            }
        }

        return allPairs;
    }

    private static DisplayPairs buildAllPairs(SingleInputProcessingRecipe recipe, int inputCount) {
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack input : recipe.getInputDisplayStacks()) {
            ItemStack output = recipe.getResultForInput(input);
            if (output.isEmpty()) {
                continue;
            }
            ItemStack countedInput = input.copy();
            countedInput.setCount(inputCount);
            inputs.add(countedInput);
            outputs.add(output);
        }
        return new DisplayPairs(inputs, outputs);
    }

    private static boolean sameStackIdentity(ItemStack first, ItemStack second) {
        return ItemStack.isSame(first, second) && ItemStack.tagMatches(first, second);
    }

    private static boolean hasMultipleOutputVariants(SingleInputProcessingRecipe recipe) {
        List<ItemStack> outputs = recipe.getResultDisplayStacks();
        for (int i = 0; i < outputs.size(); i++) {
            for (int j = i + 1; j < outputs.size(); j++) {
                if (!sameStackIdentity(outputs.get(i), outputs.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class DisplayPairs {
        private final List<ItemStack> inputs;
        private final List<ItemStack> outputs;

        private DisplayPairs(List<ItemStack> inputs, List<ItemStack> outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }
}
