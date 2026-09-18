package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class ItemRouteFilter {
    private ItemStack sample = ItemStack.EMPTY;
    private ItemFilterMode mode = ItemFilterMode.WHITELIST;
    private boolean matchNbt = true;

    public ItemRouteFilter() {
    }

    public ItemRouteFilter(ItemRouteFilter other) {
        this.sample = other.sample.isEmpty() ? ItemStack.EMPTY : other.sample.copy();
        this.mode = other.mode;
        this.matchNbt = other.matchNbt;
    }

    public ItemStack getSample() {
        return sample.isEmpty() ? ItemStack.EMPTY : sample.copy();
    }

    public void setSample(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            sample = ItemStack.EMPTY;
            return;
        }

        sample = stack.copy();
        sample.setCount(1);
    }

    public void clearSample() {
        sample = ItemStack.EMPTY;
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
        if (sample.isEmpty()) {
            return true;
        }

        boolean matches = ItemStack.isSame(sample, stack)
                && (!matchNbt || ItemStack.tagMatches(sample, stack));
        return mode == ItemFilterMode.WHITELIST ? matches : !matches;
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("Mode", mode.ordinal());
        nbt.putBoolean("MatchNbt", matchNbt);
        if (!sample.isEmpty()) {
            nbt.put("Sample", sample.save(new CompoundNBT()));
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
        if (nbt.contains("Sample")) {
            ItemStack loaded = ItemStack.of(nbt.getCompound("Sample"));
            filter.setSample(loaded);
        }
        return filter;
    }
}
