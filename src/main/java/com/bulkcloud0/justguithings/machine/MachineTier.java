package com.bulkcloud0.justguithings.machine;

public enum MachineTier {
    MK_I("MK-I", 1),
    MK_II("MK-II", 2),
    MK_III("MK-III", 3),
    MK_IV("MK-IV", 4);

    private final String displayName;
    private final int maxModulesPerType;

    MachineTier(String displayName, int maxModulesPerType) {
        this.displayName = displayName;
        this.maxModulesPerType = maxModulesPerType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxModulesPerType() {
        return maxModulesPerType;
    }
}
