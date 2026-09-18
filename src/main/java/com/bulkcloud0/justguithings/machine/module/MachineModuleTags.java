package com.bulkcloud0.justguithings.machine.module;

import com.bulkcloud0.justguithings.JustGuiThings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;

public final class MachineModuleTags {
    public static final Tags.IOptionalNamedTag<Item> SPEED = tag("speed");
    public static final Tags.IOptionalNamedTag<Item> EFFICIENCY = tag("efficiency");
    public static final Tags.IOptionalNamedTag<Item> BUFFER = tag("buffer");
    public static final Tags.IOptionalNamedTag<Item> BATCH = tag("batch");

    private MachineModuleTags() {}

    @Nullable
    public static ResourceLocation findType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        ResourceLocation match = null;

        match = mergeMatch(match, item, SPEED, MachineModuleTypes.SPEED);
        match = mergeMatch(match, item, EFFICIENCY, MachineModuleTypes.EFFICIENCY);
        match = mergeMatch(match, item, BUFFER, MachineModuleTypes.BUFFER);
        match = mergeMatch(match, item, BATCH, MachineModuleTypes.BATCH);

        return match;
    }

    @Nullable
    private static ResourceLocation mergeMatch(@Nullable ResourceLocation current,
                                               Item item,
                                               Tags.IOptionalNamedTag<Item> tag,
                                               ResourceLocation candidate) {
        if (!tag.contains(item)) {
            return current;
        }
        if (current != null && !current.equals(candidate)) {
            return null;
        }
        return candidate;
    }

    private static Tags.IOptionalNamedTag<Item> tag(String path) {
        return ItemTags.createOptional(new ResourceLocation(JustGuiThings.MOD_ID, "machine_modules/" + path));
    }
}
