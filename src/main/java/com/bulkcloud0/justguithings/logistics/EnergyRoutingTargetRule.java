package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;

public final class EnergyRoutingTargetRule {
    private RoutingPriority priority = RoutingPriority.NORMAL;
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public EnergyRoutingTargetRule() {
    }

    public EnergyRoutingTargetRule(EnergyRoutingTargetRule other) {
        priority = other.priority;
        redstoneMode = other.redstoneMode;
    }

    public RoutingPriority getPriority() {
        return priority;
    }

    public RoutingPriority cyclePriority() {
        priority = priority.next();
        return priority;
    }

    public RoutingRedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public RoutingRedstoneMode cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        return redstoneMode;
    }

    public boolean allowsRedstone(boolean powered) {
        return redstoneMode.allows(powered);
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("Priority", priority.ordinal());
        nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        return nbt;
    }

    public static EnergyRoutingTargetRule load(CompoundNBT nbt) {
        EnergyRoutingTargetRule rule = new EnergyRoutingTargetRule();
        if (nbt.contains("Priority")) {
            rule.priority = RoutingPriority.fromOrdinal(nbt.getInt("Priority"));
        }
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }
        return rule;
    }
}
