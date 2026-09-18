package com.bulkcloud0.justguithings.item;

import com.bulkcloud0.justguithings.machine.module.IMachineModule;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class MachineModuleItem extends TooltipItem implements IMachineModule {
    private final ResourceLocation moduleType;

    public MachineModuleItem(Properties properties, ResourceLocation moduleType, String... tooltipKeys) {
        super(properties, tooltipKeys);
        this.moduleType = moduleType;
    }

    @Nonnull
    @Override
    public ResourceLocation getMachineModuleType() {
        return moduleType;
    }
}
