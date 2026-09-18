package com.bulkcloud0.justguithings.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FluidIngredient {
    private final ResourceLocation id;
    private final boolean tag;
    private final Fluid fluid;
    private final ITag.INamedTag<Fluid> fluidTag;

    private FluidIngredient(ResourceLocation id, boolean tag, Fluid fluid, ITag.INamedTag<Fluid> fluidTag) {
        this.id = id;
        this.tag = tag;
        this.fluid = fluid;
        this.fluidTag = fluidTag;
    }

    public static FluidIngredient fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new JsonSyntaxException("Fluid ingredient is missing");
        }

        if (element.isJsonPrimitive()) {
            return exact(new ResourceLocation(element.getAsString()));
        }

        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("Fluid ingredient must be a fluid id or object");
        }

        JsonObject object = element.getAsJsonObject();
        boolean hasFluid = object.has("fluid");
        boolean hasTag = object.has("tag");
        if (hasFluid == hasTag) {
            throw new JsonSyntaxException("Fluid ingredient object must contain exactly one of 'fluid' or 'tag'");
        }

        if (hasTag) {
            return tag(new ResourceLocation(JSONUtils.getAsString(object, "tag")));
        }
        return exact(new ResourceLocation(JSONUtils.getAsString(object, "fluid")));
    }

    public static FluidIngredient fromNetwork(PacketBuffer buffer) {
        boolean tag = buffer.readBoolean();
        ResourceLocation id = buffer.readResourceLocation();
        return tag ? tag(id) : exact(id);
    }

    public void toNetwork(PacketBuffer buffer) {
        buffer.writeBoolean(tag);
        buffer.writeResourceLocation(id);
    }

    public boolean test(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return tag ? fluidTag.contains(stack.getFluid()) : stack.getFluid() == fluid;
    }

    public List<FluidStack> getMatchingStacks(int amount) {
        int safeAmount = Math.max(1, amount);
        if (!tag) {
            return fluid == null || fluid == Fluids.EMPTY
                    ? Collections.emptyList()
                    : Collections.singletonList(new FluidStack(fluid, safeAmount));
        }

        List<FluidStack> stacks = new ArrayList<>();
        for (Fluid candidate : fluidTag.getValues()) {
            if (candidate != null && candidate != Fluids.EMPTY) {
                stacks.add(new FluidStack(candidate, safeAmount));
            }
        }
        return stacks;
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isTag() {
        return tag;
    }

    private static FluidIngredient exact(ResourceLocation id) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null || fluid == Fluids.EMPTY) {
            throw new JsonSyntaxException("Unknown fluid " + id);
        }
        return new FluidIngredient(id, false, fluid, null);
    }

    private static FluidIngredient tag(ResourceLocation id) {
        return new FluidIngredient(id, true, null, FluidTags.createOptional(id));
    }
}
