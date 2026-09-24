package com.bulkcloud0.justguithings.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecipeSelectionHelper {
    private RecipeSelectionHelper() {}

    public static int compareSingleInput(SingleInputProcessingRecipe left,
                                         SingleInputProcessingRecipe right) {
        int specificity = compareIngredients(left.getInput(), right.getInput());
        if (specificity != 0) {
            return specificity;
        }
        return compareIds(left.getId(), right.getId());
    }

    public static int compareIngredients(Ingredient left, Ingredient right) {
        return Integer.compare(candidateCount(left), candidateCount(right));
    }

    public static int compareIngredientSets(Iterable<Ingredient> left,
                                            Iterable<Ingredient> right) {
        List<Integer> leftCounts = candidateCounts(left);
        List<Integer> rightCounts = candidateCounts(right);

        int slotSpecificity = Integer.compare(rightCounts.size(), leftCounts.size());
        if (slotSpecificity != 0) {
            return slotSpecificity;
        }

        int combinationSpecificity = Long.compare(
                combinationCount(leftCounts),
                combinationCount(rightCounts));
        if (combinationSpecificity != 0) {
            return combinationSpecificity;
        }

        Collections.sort(leftCounts);
        Collections.sort(rightCounts);
        for (int index = 0; index < leftCounts.size(); index++) {
            int widthSpecificity = Integer.compare(leftCounts.get(index), rightCounts.get(index));
            if (widthSpecificity != 0) {
                return widthSpecificity;
            }
        }
        return 0;
    }

    public static int compareIds(ResourceLocation left, ResourceLocation right) {
        return left.toString().compareTo(right.toString());
    }

    private static List<Integer> candidateCounts(Iterable<Ingredient> ingredients) {
        List<Integer> result = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            result.add(candidateCount(ingredient));
        }
        return result;
    }

    private static int candidateCount(Ingredient ingredient) {
        int count = 0;
        for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count == 0 ? Integer.MAX_VALUE : count;
    }

    private static long combinationCount(List<Integer> candidateCounts) {
        long combinations = 1L;
        for (int count : candidateCounts) {
            if (count == Integer.MAX_VALUE || combinations > Long.MAX_VALUE / count) {
                return Long.MAX_VALUE;
            }
            combinations *= count;
        }
        return combinations;
    }
}
