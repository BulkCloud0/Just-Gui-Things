package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, JustGuiThings.MOD_ID);

    public static final RegistryObject<ContainerType<CoalGeneratorContainer>> COAL_GENERATOR =
            CONTAINERS.register("coal_generator", () -> IForgeContainerType.create(CoalGeneratorContainer::new));

    private ModContainers() {}
}
