package com.bulkcloud0.justguithings.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FluidIngredient {
    @Nullable
    private final Fluid fluid;
    @Nullable
    private final ResourceLocation tag;

    private FluidIngredient(@Nullable Fluid fluid, @Nullable ResourceLocation tag) {
        this.fluid = fluid;
        this.tag = tag;
    }

    public static FluidIngredient fromJson(ResourceLocation recipeId, JsonObject json) {
        boolean hasFluid = json.has("fluid");
        boolean hasTag = json.has("tag");
        if (hasFluid == hasTag) {
            throw new JsonSyntaxException("Recipe " + recipeId
                    + " fluid ingredient must define exactly one of 'fluid' or 'tag'");
        }

        if (hasFluid) {
            ResourceLocation fluidId = new ResourceLocation(JSONUtils.getAsString(json, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException("Recipe " + recipeId
                        + " references unknown fluid " + fluidId);
            }
            return new FluidIngredient(fluid, null);
        }

        return new FluidIngredient(null,
                new ResourceLocation(JSONUtils.getAsString(json, "tag")));
    }

    public static FluidIngredient fromNetwork(PacketBuffer buffer) {
        boolean tagBased = buffer.readBoolean();
        ResourceLocation id = buffer.readResourceLocation();
        if (tagBased) {
            return new FluidIngredient(null, id);
        }

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return new FluidIngredient(fluid == null ? Fluids.EMPTY : fluid, null);
    }

    public void toNetwork(PacketBuffer buffer) {
        buffer.writeBoolean(tag != null);
        if (tag != null) {
            buffer.writeResourceLocation(tag);
            return;
        }

        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
        buffer.writeResourceLocation(fluidId == null
                ? new ResourceLocation("minecraft", "empty")
                : fluidId);
    }

    public boolean test(FluidStack stack) {
        return !stack.isEmpty() && test(stack.getFluid());
    }

    public boolean test(Fluid candidate) {
        if (candidate == null || candidate == Fluids.EMPTY) {
            return false;
        }
        if (fluid != null) {
            return candidate == fluid;
        }
        return tag != null && candidate.getTags().contains(tag);
    }

    public boolean isTagBased() {
        return tag != null;
    }

    public List<FluidStack> getDisplayStacks(int amount) {
        List<Fluid> fluids = new ArrayList<>();
        if (fluid != null && fluid != Fluids.EMPTY) {
            fluids.add(fluid);
        } else if (tag != null) {
            for (Fluid candidate : ForgeRegistries.FLUIDS.getValues()) {
                if (candidate != Fluids.EMPTY && candidate.getTags().contains(tag)) {
                    fluids.add(candidate);
                }
            }
        }

        fluids.sort(Comparator.comparing(FluidIngredient::getRegistryName));
        List<FluidStack> stacks = new ArrayList<>();
        for (Fluid candidate : fluids) {
            stacks.add(new FluidStack(candidate, amount));
        }
        return stacks;
    }

    private static String getRegistryName(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id == null ? "" : id.toString();
    }
}
