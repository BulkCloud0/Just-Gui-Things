package com.bulkcloud0.justguithings.integration.jei;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.recipe.AssemblyRecipe;
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
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AssemblyRecipeCategory implements IRecipeCategory<AssemblyRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(JustGuiThings.MOD_ID, "assembly");

    private final IDrawable background;
    private final IDrawable icon;

    public AssemblyRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(136, 58);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.INDUSTRIAL_ASSEMBLER.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends AssemblyRecipe> getRecipeClass() {
        return AssemblyRecipe.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getTitle() {
        return getTitleAsTextComponent().getString();
    }

    @Override
    public ITextComponent getTitleAsTextComponent() {
        return new TranslationTextComponent("jei.justguithings.assembly");
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
    public void setIngredients(AssemblyRecipe recipe, IIngredients ingredients) {
        ingredients.setInputIngredients(recipe.getIngredients());
        ingredients.setOutputs(VanillaTypes.ITEM, getOutputDisplayStacks(recipe));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, AssemblyRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        DisplayVariants variants = getDisplayVariants(recipe, recipeLayout.getFocus(VanillaTypes.ITEM));

        int[] inputX = {1, 20, 39, 58};
        for (int slot = 0; slot < AssemblyRecipe.MAX_INPUTS; slot++) {
            itemStacks.init(slot, true, inputX[slot], 20);
            itemStacks.set(slot, variants.inputs.get(slot));
        }

        itemStacks.init(AssemblyRecipe.MAX_INPUTS, false, 110, 20);
        itemStacks.set(AssemblyRecipe.MAX_INPUTS, variants.outputs);

        if (hasMultipleOutputVariants(recipe)) {
            itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if (slotIndex == AssemblyRecipe.MAX_INPUTS && !input) {
                    tooltip.add(new TranslationTextComponent(
                            "jei.justguithings.output_matches_input_provider")
                            .withStyle(TextFormatting.GRAY));
                }
            });
        }
    }

    private static List<ItemStack> getInputDisplayStacks(AssemblyRecipe recipe, int inputIndex) {
        return withCount(recipe.getIngredient(inputIndex), recipe.getRequiredCount(inputIndex));
    }

    private static DisplayVariants getDisplayVariants(AssemblyRecipe recipe, IFocus<ItemStack> focus) {
        DisplayVariants variants = buildAllVariants(recipe);
        if (focus == null) {
            return variants;
        }

        ItemStack focused = focus.getValue();
        if (focus.getMode() == IFocus.Mode.INPUT) {
            for (int input = 0; input < recipe.getInputCount(); input++) {
                if (!recipe.getIngredient(input).test(focused)) {
                    continue;
                }

                List<List<ItemStack>> inputs = copyInputs(variants.inputs);
                inputs.set(input, Collections.singletonList(
                        withCount(focused, recipe.getRequiredCount(input))));

                List<ItemStack> outputs = input == 0
                        ? Collections.singletonList(getOutputForProvider(recipe, focused))
                        : variants.outputs;
                if (input != 0 || !outputs.get(0).isEmpty()) {
                    return new DisplayVariants(inputs, outputs);
                }
            }
        }

        if (focus.getMode() == IFocus.Mode.OUTPUT) {
            List<ItemStack> providers = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            for (ItemStack provider : getInputDisplayStacks(recipe, 0)) {
                ItemStack output = getOutputForProvider(recipe, provider);
                if (!output.isEmpty() && sameStackIdentity(output, focused)) {
                    providers.add(provider);
                    outputs.add(output);
                }
            }
            if (!providers.isEmpty()) {
                List<List<ItemStack>> inputs = copyInputs(variants.inputs);
                inputs.set(0, providers);
                return new DisplayVariants(inputs, outputs);
            }
        }

        return variants;
    }

    private static DisplayVariants buildAllVariants(AssemblyRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (int slot = 0; slot < AssemblyRecipe.MAX_INPUTS; slot++) {
            inputs.add(slot < recipe.getInputCount()
                    ? getInputDisplayStacks(recipe, slot)
                    : Collections.emptyList());
        }
        return new DisplayVariants(inputs, getOutputDisplayStacks(recipe));
    }

    private static List<List<ItemStack>> copyInputs(List<List<ItemStack>> inputs) {
        List<List<ItemStack>> copy = new ArrayList<>();
        for (List<ItemStack> input : inputs) {
            copy.add(new ArrayList<>(input));
        }
        return copy;
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static List<ItemStack> withCount(Ingredient ingredient, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            copy.setCount(count);
            stacks.add(copy);
        }
        return stacks;
    }

    private static List<ItemStack> getOutputDisplayStacks(AssemblyRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        List<ItemStack> providers = getInputDisplayStacks(recipe, 0);
        if (providers.isEmpty()) {
            return outputs;
        }

        for (ItemStack provider : providers) {
            ItemStack output = getOutputForProvider(recipe, provider);
            if (!output.isEmpty() && !containsSameStack(outputs, output)) {
                outputs.add(output);
            }
        }
        return outputs;
    }

    private static ItemStack getOutputForProvider(AssemblyRecipe recipe, ItemStack provider) {
        return recipe.getResultForInventory(buildRepresentativeInventory(recipe, provider));
    }

    private static Inventory buildRepresentativeInventory(AssemblyRecipe recipe, ItemStack provider) {
        ItemStack[] stacks = new ItemStack[AssemblyRecipe.MAX_INPUTS];
        Arrays.fill(stacks, ItemStack.EMPTY);
        stacks[0] = provider.copy();

        for (int input = 1; input < recipe.getInputCount(); input++) {
            List<ItemStack> candidates = getInputDisplayStacks(recipe, input);
            if (!candidates.isEmpty()) {
                stacks[input] = candidates.get(0).copy();
            }
        }
        return new Inventory(stacks);
    }

    private static boolean sameStackIdentity(ItemStack first, ItemStack second) {
        return ItemStack.isSame(first, second) && ItemStack.tagMatches(first, second);
    }

    private static boolean containsSameStack(List<ItemStack> stacks, ItemStack candidate) {
        for (ItemStack stack : stacks) {
            if (sameStackIdentity(stack, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMultipleOutputVariants(AssemblyRecipe recipe) {
        return getOutputDisplayStacks(recipe).size() > 1;
    }

    @Override
    public void draw(AssemblyRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.font.draw(matrixStack, recipe.getProcessingTime() + " t", 80.0F, 12.0F, 0xFF808080);
        minecraft.font.draw(matrixStack, recipe.getEnergyPerTick() + " FE/t", 76.0F, 39.0F, 0xFF808080);
    }

    private static final class DisplayVariants {
        private final List<List<ItemStack>> inputs;
        private final List<ItemStack> outputs;

        private DisplayVariants(List<List<ItemStack>> inputs, List<ItemStack> outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }
}
