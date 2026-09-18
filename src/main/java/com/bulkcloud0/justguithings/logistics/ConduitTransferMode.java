package com.bulkcloud0.justguithings.logistics;

public enum ConduitTransferMode {
    BOTH(true, true),
    PULL(true, false),
    PUSH(false, true),
    DISABLED(false, false);

    private final boolean pull;
    private final boolean push;

    ConduitTransferMode(boolean pull, boolean push) {
        this.pull = pull;
        this.push = push;
    }

    public boolean canPull() {
        return pull;
    }

    public boolean canPush() {
        return push;
    }

    public ConduitTransferMode next() {
        ConduitTransferMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static ConduitTransferMode fromOrdinal(int ordinal) {
        ConduitTransferMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BOTH;
        }
        return values[ordinal];
    }
}
