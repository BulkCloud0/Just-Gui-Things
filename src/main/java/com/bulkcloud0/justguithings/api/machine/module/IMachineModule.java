package com.bulkcloud0.justguithings.api.machine.module;

import net.minecraft.util.ResourceLocation;

/**
 * Stable public extension point for items that can be installed in JGT machine module slots.
 *
 * Addons may implement this interface without subclassing a JGT item. Returning a module type
 * only identifies the module family; each machine still decides which module types it accepts.
 */
public interface IMachineModule {
    ResourceLocation getMachineModuleType();
}
