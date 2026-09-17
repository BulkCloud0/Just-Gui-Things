package com.bulkcloud0.justguithings.machine;

public enum MachineTier {
    BASIC("basic", 1),
    REINFORCED("reinforced", 2),
    ADVANCED("advanced", 3),
    ELITE("elite", 4);

    private final String name;
    private final int maxUpgradeLevel;

    MachineTier(String name, int maxUpgradeLevel) {
        this.name = name;
        this.maxUpgradeLevel = maxUpgradeLevel;
    }

    public String getSerializedName() {
        return name;
    }

    public String getTranslationKey() {
        return "tier.justguithings." + name;
    }

    public int getMaxUpgradeLevel() {
        return maxUpgradeLevel;
    }

    public static MachineTier fromOrdinal(int ordinal) {
        MachineTier[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BASIC;
        }
        return values[ordinal];
    }
}
