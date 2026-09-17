package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import com.bulkcloud0.justguithings.world.container.CrusherContainer;
import com.bulkcloud0.justguithings.world.container.EnergyCellContainer;
import com.bulkcloud0.justguithings.world.container.IndustrialMixerContainer;
import com.bulkcloud0.justguithings.world.container.QuenchChamberContainer;
import com.bulkcloud0.justguithings.world.container.StampingPressContainer;
import com.bulkcloud0.justguithings.world.container.ThermalKilnContainer;
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
    public static final RegistryObject<ContainerType<CrusherContainer>> CRUSHER =
            CONTAINERS.register("crusher", () -> IForgeContainerType.create(CrusherContainer::new));
    public static final RegistryObject<ContainerType<StampingPressContainer>> STAMPING_PRESS =
            CONTAINERS.register("stamping_press", () -> IForgeContainerType.create(StampingPressContainer::new));
    public static final RegistryObject<ContainerType<IndustrialMixerContainer>> INDUSTRIAL_MIXER =
            CONTAINERS.register("industrial_mixer", () -> IForgeContainerType.create(IndustrialMixerContainer::new));
    public static final RegistryObject<ContainerType<QuenchChamberContainer>> QUENCH_CHAMBER =
            CONTAINERS.register("quench_chamber", () -> IForgeContainerType.create(QuenchChamberContainer::new));
    public static final RegistryObject<ContainerType<ThermalKilnContainer>> THERMAL_KILN =
            CONTAINERS.register("thermal_kiln", () -> IForgeContainerType.create(ThermalKilnContainer::new));
    public static final RegistryObject<ContainerType<EnergyCellContainer>> ENERGY_CELL =
            CONTAINERS.register("energy_cell", () -> IForgeContainerType.create(EnergyCellContainer::new));

    private ModContainers() {}
}
