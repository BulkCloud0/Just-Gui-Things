package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemRouteFilter {
    public static final int MAX_SAMPLES = 9;

    private final List<ItemStack> samples = new ArrayList<>();
    private RoutingFilterMode mode = RoutingFilterMode.WHITELIST;
    private boolean matchNbt = true;

    public ItemRouteFilter() {
    }

    public ItemRouteFilter(ItemRouteFilter other) {
        for (ItemStack sample : other.samples) {
            samples.add(sample.copy());
        }
        mode = other.mode;
        matchNbt = other.matchNbt;
    }

    public List<ItemStack> getSamples() {
        List<ItemStack> copy = new ArrayList<>(samples.size());
        for (ItemStack sample : samples) {
            copy.add(sample.copy());
        }
        return Collections.unmodifiableList(copy);
    }

    public int getSampleCount() {
        return samples.size();
    }

    public RoutingFilterSampleChange toggleSample(ItemStack stack) {
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

    public boolean addSample(ItemStack stack) {
        if (stack == null || stack.isEmpty() || samples.size() >= MAX_SAMPLES) {
            return false;
        }
        if (findExactSample(stack) >= 0) {
            return true;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        samples.add(copy);
        return true;
    }

    public void clearSamples() {
        samples.clear();
    }

    public RoutingFilterMode getMode() {
        return mode;
    }

    public RoutingFilterMode cycleMode() {
        mode = mode.next();
        return mode;
    }

    public boolean isMatchNbt() {
        return matchNbt;
    }

    public boolean toggleMatchNbt() {
        matchNbt = !matchNbt;
        return matchNbt;
    }

    public boolean accepts(ItemStack stack) {
        if (samples.isEmpty()) {
            return true;
        }

        boolean matches = false;
        for (ItemStack sample : samples) {
            if (matchesSample(sample, stack, matchNbt)) {
                matches = true;
                break;
            }
        }

        return mode == RoutingFilterMode.WHITELIST ? matches : !matches;
    }

    public boolean matchesIdentity(ItemStack first, ItemStack second) {
        return matchesSample(first, second, matchNbt);
    }

    private int findExactSample(ItemStack stack) {
        for (int index = 0; index < samples.size(); index++) {
            if (matchesSample(samples.get(index), stack, true)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matchesSample(ItemStack sample, ItemStack stack, boolean compareNbt) {
        return ItemStack.isSame(sample, stack)
                && (!compareNbt || ItemStack.tagMatches(sample, stack));
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("Mode", mode.ordinal());
        nbt.putBoolean("MatchNbt", matchNbt);

        ListNBT sampleList = new ListNBT();
        for (ItemStack sample : samples) {
            sampleList.add(sample.save(new CompoundNBT()));
        }
        if (!sampleList.isEmpty()) {
            nbt.put("Samples", sampleList);
        }

        return nbt;
    }

    public static ItemRouteFilter load(CompoundNBT nbt) {
        ItemRouteFilter filter = new ItemRouteFilter();

        if (nbt.contains("Mode")) {
            filter.mode = RoutingFilterMode.fromOrdinal(nbt.getInt("Mode"));
        }
        if (nbt.contains("MatchNbt")) {
            filter.matchNbt = nbt.getBoolean("MatchNbt");
        }

        if (nbt.contains("Samples")) {
            ListNBT list = nbt.getList("Samples", 10);
            for (int index = 0; index < list.size() && filter.samples.size() < MAX_SAMPLES; index++) {
                filter.addSample(ItemStack.of(list.getCompound(index)));
            }
        } else if (nbt.contains("Sample")) {
            filter.addSample(ItemStack.of(nbt.getCompound("Sample")));
        }

        return filter;
    }
}
