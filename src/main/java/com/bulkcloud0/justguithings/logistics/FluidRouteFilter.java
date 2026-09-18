package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FluidRouteFilter {
    public static final int MAX_SAMPLES = 9;

    private final List<FluidStack> samples = new ArrayList<>();
    private RoutingFilterMode mode = RoutingFilterMode.WHITELIST;
    private boolean matchNbt = true;

    public FluidRouteFilter() {
    }

    public FluidRouteFilter(FluidRouteFilter other) {
        for (FluidStack sample : other.samples) {
            samples.add(sample.copy());
        }
        mode = other.mode;
        matchNbt = other.matchNbt;
    }

    public List<FluidStack> getSamples() {
        List<FluidStack> copy = new ArrayList<>(samples.size());
        for (FluidStack sample : samples) {
            copy.add(sample.copy());
        }
        return Collections.unmodifiableList(copy);
    }

    public int getSampleCount() {
        return samples.size();
    }

    public RoutingFilterSampleChange toggleSample(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RoutingFilterSampleChange.FULL;
        }

        int existing = findExactSample(stack);
        if (existing >= 0) {
            samples.remove(existing);
            return RoutingFilterSampleChange.REMOVED;
        }

        if (samples.size() >= MAX_SAMPLES) {
            return RoutingFilterSampleChange.FULL;
        }

        addSample(stack);
        return RoutingFilterSampleChange.ADDED;
    }

    public boolean addSample(FluidStack stack) {
        if (stack == null || stack.isEmpty() || samples.size() >= MAX_SAMPLES) {
            return false;
        }
        if (findExactSample(stack) >= 0) {
            return true;
        }

        FluidStack copy = stack.copy();
        copy.setAmount(1);
        samples.add(copy);
        return true;
    }

    public void clearSamples() {
        samples.clear();
    }

    public RoutingFilterMode cycleMode() {
        mode = mode.next();
        return mode;
    }

    public boolean toggleMatchNbt() {
        matchNbt = !matchNbt;
        return matchNbt;
    }

    public boolean accepts(FluidStack stack) {
        if (samples.isEmpty()) {
            return true;
        }

        boolean matches = false;
        for (FluidStack sample : samples) {
            if (matchesSample(sample, stack, matchNbt)) {
                matches = true;
                break;
            }
        }

        return mode == RoutingFilterMode.WHITELIST ? matches : !matches;
    }

    public boolean matchesIdentity(FluidStack first, FluidStack second) {
        return matchesSample(first, second, matchNbt);
    }

    private int findExactSample(FluidStack stack) {
        for (int index = 0; index < samples.size(); index++) {
            if (matchesSample(samples.get(index), stack, true)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matchesSample(FluidStack first, FluidStack second, boolean compareNbt) {
        return first != null
                && second != null
                && !first.isEmpty()
                && !second.isEmpty()
                && first.getFluid() == second.getFluid()
                && (!compareNbt || Objects.equals(first.getTag(), second.getTag()));
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("Mode", mode.ordinal());
        nbt.putBoolean("MatchNbt", matchNbt);

        ListNBT sampleList = new ListNBT();
        for (FluidStack sample : samples) {
            sampleList.add(sample.writeToNBT(new CompoundNBT()));
        }
        if (!sampleList.isEmpty()) {
            nbt.put("Samples", sampleList);
        }

        return nbt;
    }

    public static FluidRouteFilter load(CompoundNBT nbt) {
        FluidRouteFilter filter = new FluidRouteFilter();

        if (nbt.contains("Mode")) {
            filter.mode = RoutingFilterMode.fromOrdinal(nbt.getInt("Mode"));
        }
        if (nbt.contains("MatchNbt")) {
            filter.matchNbt = nbt.getBoolean("MatchNbt");
        }
        if (nbt.contains("Samples")) {
            ListNBT list = nbt.getList("Samples", 10);
            for (int index = 0; index < list.size() && filter.samples.size() < MAX_SAMPLES; index++) {
                filter.addSample(FluidStack.loadFluidStackFromNBT(list.getCompound(index)));
            }
        }

        return filter;
    }
}
