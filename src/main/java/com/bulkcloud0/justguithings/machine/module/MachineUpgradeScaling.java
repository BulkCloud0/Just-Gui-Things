package com.bulkcloud0.justguithings.machine.module;

public final class MachineUpgradeScaling {
    private static final int BASE_PERCENT = 100;
    private static final int SPEED_PERCENT_PER_MODULE = 50;
    private static final int EFFICIENCY_TIME_PERCENT_PER_MODULE = 10;
    private static final int EFFICIENCY_ENERGY_PERCENT_PER_MODULE = 15;
    private static final int MIN_EFFICIENCY_ENERGY_PERCENT = 40;
    private static final int MIN_PROCESSING_TICKS = 20;

    private MachineUpgradeScaling() {
    }

    public static int getEffectiveProcessingTime(int baseProcessingTime,
                                                 int speedModuleCount,
                                                 int efficiencyModuleCount) {
        int speedMultiplier = BASE_PERCENT + SPEED_PERCENT_PER_MODULE * speedModuleCount;
        int efficiencyTimeMultiplier = BASE_PERCENT
                + EFFICIENCY_TIME_PERCENT_PER_MODULE * efficiencyModuleCount;
        long scaled = (long) baseProcessingTime * efficiencyTimeMultiplier;
        return Math.max(MIN_PROCESSING_TICKS,
                (int) ((scaled + speedMultiplier - 1L) / speedMultiplier));
    }

    public static int getEffectiveEnergyPerTick(int baseEnergyPerTick,
                                                int speedModuleCount,
                                                int efficiencyModuleCount,
                                                int workUnits) {
        int speedMultiplier = BASE_PERCENT + SPEED_PERCENT_PER_MODULE * speedModuleCount;
        int efficiencyMultiplier = Math.max(
                MIN_EFFICIENCY_ENERGY_PERCENT,
                BASE_PERCENT - EFFICIENCY_ENERGY_PERCENT_PER_MODULE * efficiencyModuleCount);
        long scaled = (long) baseEnergyPerTick
                * speedMultiplier
                * efficiencyMultiplier
                * Math.max(1, workUnits);
        return Math.max(1, (int) ((scaled + 9_999L) / 10_000L));
    }
}
