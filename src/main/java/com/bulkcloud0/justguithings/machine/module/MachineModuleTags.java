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
    public static final Tags.IOptionalNamedTag<Item> INDUCTION_COIL = tag("induction_coil");

    private MachineModuleTags() {}

    @Nullable
    public static ResourceLocation findType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        ResourceLocation match = null;
        int matchCount = 0;

        if (SPEED.contains(item)) {
            match = MachineModuleTypes.SPEED;
            matchCount++;
        }
        if (EFFICIENCY.contains(item)) {
            match = MachineModuleTypes.EFFICIENCY;
            matchCount++;
        }
        if (BUFFER.contains(item)) {
            match = MachineModuleTypes.BUFFER;
            matchCount++;
        }
        if (BATCH.contains(item)) {
            match = MachineModuleTypes.BATCH;
            matchCount++;
        }
        if (INDUCTION_COIL.contains(item)) {
            match = MachineModuleTypes.INDUCTION_COIL;
            matchCount++;
        }

        return matchCount == 1 ? match : null;
    }

    private static Tags.IOptionalNamedTag<Item> tag(String path) {
        return ItemTags.createOptional(new ResourceLocation(JustGuiThings.MOD_ID, "machine_modules/" + path));
    }
}
