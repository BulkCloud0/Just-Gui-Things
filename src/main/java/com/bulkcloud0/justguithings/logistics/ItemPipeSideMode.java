package com.bulkcloud0.justguithings.logistics;

public enum ItemPipeSideMode {
    BOTH(true, true),
    PULL(true, false),
    PUSH(false, true),
    DISABLED(false, false);

    private final boolean pull;
    private final boolean push;

    ItemPipeSideMode(boolean pull, boolean push) {
        this.pull = pull;
        this.push = push;
    }

    public boolean canPull() {
        return pull;
    }

    public boolean canPush() {
        return push;
    }

    public ItemPipeSideMode next() {
        ItemPipeSideMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static ItemPipeSideMode fromOrdinal(int ordinal) {
        ItemPipeSideMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BOTH;
        }
        return values[ordinal];
    }
}
