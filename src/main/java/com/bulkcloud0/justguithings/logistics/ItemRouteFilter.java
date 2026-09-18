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
    private ItemFilterMode mode = ItemFilterMode.WHITELIST;
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

    public ItemFilterSampleChange toggleSample(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemFilterSampleChange.FULL;
        }

        int existing = findExactSample(stack);
        if (existing >= 0) {
            samples.remove(existing);
            return ItemFilterSampleChange.REMOVED;
        }

        if (samples.size() >= MAX_SAMPLES) {
            return ItemFilterSampleChange.FULL;
        }

        addSample(stack);
        return ItemFilterSampleChange.ADDED;
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

    public ItemFilterMode getMode() {
        return mode;
    }

    public ItemFilterMode cycleMode() {
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

        return mode == ItemFilterMode.WHITELIST ? matches : !matches;
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
            filter.mode = ItemFilterMode.fromOrdinal(nbt.getInt("Mode"));
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
