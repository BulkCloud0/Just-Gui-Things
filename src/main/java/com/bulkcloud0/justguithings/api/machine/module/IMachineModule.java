package com.bulkcloud0.justguithings.api.machine.module;

import net.minecraft.util.ResourceLocation;

/**
 * Public extension point for items that can be installed in Just Gui Things machine module slots.
 *
 * Implementations identify their behavior through a namespaced module type. Addons may implement
 * this interface without subclassing JGT item classes.
 */
public interface IMachineModule {
    ResourceLocation getMachineModuleType();
}
