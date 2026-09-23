package com.bulkcloud0.justguithings.machine.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MachineUpgradeScalingTest {
    @Test
    public void preservesNormalProcessingTimeScaling() {
        assertEquals(54, MachineUpgradeScaling.getEffectiveProcessingTime(160, 4, 0));
        assertEquals(224, MachineUpgradeScaling.getEffectiveProcessingTime(160, 0, 4));
        assertEquals(75, MachineUpgradeScaling.getEffectiveProcessingTime(160, 4, 4));
    }

    @Test
    public void preservesNormalEnergyScaling() {
        assertEquals(135, MachineUpgradeScaling.getEffectiveEnergyPerTick(45, 4, 0, 1));
        assertEquals(18, MachineUpgradeScaling.getEffectiveEnergyPerTick(45, 0, 4, 1));
        assertEquals(54, MachineUpgradeScaling.getEffectiveEnergyPerTick(45, 4, 4, 1));
    }

    @Test
    public void saturatesExtremeProcessingTimeInsteadOfWrapping() {
        assertEquals(
                Integer.MAX_VALUE,
                MachineUpgradeScaling.getEffectiveProcessingTime(Integer.MAX_VALUE, 0, 4));
    }

    @Test
    public void saturatesExtremeEnergyInsteadOfWrapping() {
        assertEquals(
                Integer.MAX_VALUE,
                MachineUpgradeScaling.getEffectiveEnergyPerTick(Integer.MAX_VALUE, 0, 0, 2));
    }

    @Test
    public void preservesAndSaturatesPowerCoilScaling() {
        assertEquals(70, MachineUpgradeScaling.getPowerCoilProcessingTime(140));
        assertEquals(100, MachineUpgradeScaling.getPowerCoilEnergyPerTick(40));
        assertEquals(103, MachineUpgradeScaling.getPowerCoilEnergyPerTick(41));
        assertEquals(
                1_073_741_824,
                MachineUpgradeScaling.getPowerCoilProcessingTime(Integer.MAX_VALUE));
        assertEquals(
                Integer.MAX_VALUE,
                MachineUpgradeScaling.getPowerCoilEnergyPerTick(Integer.MAX_VALUE));
    }
}
