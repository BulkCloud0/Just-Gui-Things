package com.bulkcloud0.justguithings.logistics;

import net.minecraft.nbt.CompoundNBT;

public final class EnergyRoutingSourceRule {
    private RoutingRedstoneMode redstoneMode = RoutingRedstoneMode.ALWAYS;

    public EnergyRoutingSourceRule() {
    }

    public EnergyRoutingSourceRule(EnergyRoutingSourceRule other) {
        redstoneMode = other.redstoneMode;
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
        nbt.putInt("RedstoneMode", redstoneMode.ordinal());
        return nbt;
    }

    public static EnergyRoutingSourceRule load(CompoundNBT nbt) {
        EnergyRoutingSourceRule rule = new EnergyRoutingSourceRule();
        if (nbt.contains("RedstoneMode")) {
            rule.redstoneMode = RoutingRedstoneMode.fromOrdinal(nbt.getInt("RedstoneMode"));
        }
        return rule;
    }
}
