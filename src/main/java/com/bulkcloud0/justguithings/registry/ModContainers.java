package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.container.CoalGeneratorContainer;
import com.bulkcloud0.justguithings.world.container.CrusherContainer;
import com.bulkcloud0.justguithings.world.container.EnergyCellContainer;
import com.bulkcloud0.justguithings.world.container.IndustrialMixerContainer;
import com.bulkcloud0.justguithings.world.container.FluidReservoirContainer;
import com.bulkcloud0.justguithings.world.container.FluidPumpContainer;
import com.bulkcloud0.justguithings.world.container.QuenchChamberContainer;
import com.bulkcloud0.justguithings.world.container.ResistiveFurnaceContainer;
import com.bulkcloud0.justguithings.world.container.PrecisionExtruderContainer;
import com.bulkcloud0.justguithings.world.container.StampingPressContainer;
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
    public static final RegistryObject<ContainerType<EnergyCellContainer>> ENERGY_CELL =
            CONTAINERS.register("energy_cell", () -> IForgeContainerType.create(EnergyCellContainer::new));

    public static final RegistryObject<ContainerType<FluidReservoirContainer>> FLUID_RESERVOIR =
            CONTAINERS.register("fluid_reservoir", () -> IForgeContainerType.create(FluidReservoirContainer::new));

    public static final RegistryObject<ContainerType<FluidPumpContainer>> FLUID_PUMP =
            CONTAINERS.register("fluid_pump", () -> IForgeContainerType.create(FluidPumpContainer::new));

    public static final RegistryObject<ContainerType<ResistiveFurnaceContainer>> RESISTIVE_FURNACE =
            CONTAINERS.register("resistive_furnace", () -> IForgeContainerType.create(ResistiveFurnaceContainer::new));

    public static final RegistryObject<ContainerType<PrecisionExtruderContainer>> PRECISION_EXTRUDER =
            CONTAINERS.register("precision_extruder", () -> IForgeContainerType.create(PrecisionExtruderContainer::new));

    private ModContainers() {}
}
