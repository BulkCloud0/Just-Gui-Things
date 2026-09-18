package com.bulkcloud0.justguithings.logistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;

public final class ItemRoutingSourceRule {
    private static final int[] MIN_STOCK_VALUES = {0, 1, 8, 16, 32, 64};

    private ItemRouteFilter filter = new ItemRouteFilter();
    private int minStock;
    private ItemRoutingRedstoneMode redstoneMode = ItemRoutingRedstoneMode.ALWAYS;

    public ItemRoutingSourceRule() {
    }

    public ItemRoutingSourceRule(ItemRoutingSourceRule other) {
        filter = new ItemRouteFilter(other.filter);
        minStock = other.minStock;
        redstoneMode = other.redstoneMode;
    }

    public ItemRouteFilter getFilter() {
        return new ItemRouteFilter(filter);
    }

    public ItemFilterSampleChange toggleFilterSample(ItemStack stack) {
        return filter.toggleSample(stack);
    }

    public int getFilterSampleCount() {
        return filter.getSampleCount();
    }

    public void clearFilterSamples() {
        filter.clearSamples();
    }

    public RoutingFilterMode cycleFilterMode() {
        return filter.cycleMode();
    }

    public boolean toggleFilterNbt() {
        return filter.toggleMatchNbt();
    }

    public int getMinStock() {
        return minStock;
    }

    public int cycleMinStock() {
        int currentIndex = 0;
        for (int index = 0; index < MIN_STOCK_VALUES.length; index++) {
            if (MIN_STOCK_VALUES[index] == minStock) {
                currentIndex = index;
                break;
            }
        }
        minStock = MIN_STOCK_VALUES[(currentIndex + 1) % MIN_STOCK_VALUES.length];
        return minStock;
    }

    public ItemRoutingRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public ItemRoutingRedstoneMode cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        return redstoneMode;
    }

    public boolean allowsRedstone(boolean powered) {
        return redstoneMode.allows(powered);
    }

    public int getExtractableAmount(IItemHandler handler, int slot, int requested) {
        if (requested <= 0 || slot < 0 || slot >= handler.getSlots()) {
            return 0;
        }

        ItemStack candidate = handler.getStackInSlot(slot);
        if (candidate.isEmpty() || !filter.accepts(candidate)) {
            return 0;
        }

        if (minStock <= 0) {
            return requested;
        }

        int matchingCount = 0;
        for (int index = 0; index < handler.getSlots(); index++) {
            ItemStack stack = handler.getStackInSlot(index);
            if (!stack.isEmpty() && filter.matchesIdentity(candidate, stack)) {
                matchingCount += stack.getCount();
            }
        }

        return Math.min(requested, Math.max(0, matchingCount - minStock));
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Filter", filter.save());
        nbt.putInt("MinStock", minStock);
        nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        return nbt;
    }

    public static ItemRoutingSourceRule load(CompoundNBT nbt) {
        ItemRoutingSourceRule rule = new ItemRoutingSourceRule();

        if (nbt.contains("Filter")) {
            rule.filter = ItemRouteFilter.load(nbt.getCompound("Filter"));
        }
        if (nbt.contains("MinStock")) {
            rule.minStock = normalizeMinStock(nbt.getInt("MinStock"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = ItemRoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }

        return rule;
    }

    private static int normalizeMinStock(int value) {
        for (int allowed : MIN_STOCK_VALUES) {
            if (value == allowed) {
                return allowed;
            }
        }
        return 0;
    }
}
