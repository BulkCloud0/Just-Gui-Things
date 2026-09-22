package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.tile.AutoCrafterTileEntity;
import com.bulkcloud0.justguithings.world.tile.VacuumCollectorTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.ChargingStationTileEntity;
import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;
import com.bulkcloud0.justguithings.world.tile.CrusherTileEntity;
import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import com.bulkcloud0.justguithings.world.tile.IndustrialMixerTileEntity;
import com.bulkcloud0.justguithings.world.tile.IndustrialAssemblerTileEntity;
import com.bulkcloud0.justguithings.world.tile.ItemBufferTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidPumpTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidContainerStationTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidGeneratorTileEntity;
import com.bulkcloud0.justguithings.world.tile.ResistiveFurnaceTileEntity;
import com.bulkcloud0.justguithings.world.tile.RodMillTileEntity;
import com.bulkcloud0.justguithings.world.tile.StampingPressTileEntity;
import com.bulkcloud0.justguithings.world.tile.SteamGeneratorTileEntity;
import com.bulkcloud0.justguithings.world.tile.WireMillTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, JustGuiThings.MOD_ID);

    public static final RegistryObject<TileEntityType<CoalGeneratorTileEntity>> COAL_GENERATOR = TILE_ENTITIES.register(
            "coal_generator", () -> TileEntityType.Builder.of(CoalGeneratorTileEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null));
    public static final RegistryObject<TileEntityType<SteamGeneratorTileEntity>> STEAM_GENERATOR = TILE_ENTITIES.register(
            "steam_generator", () -> TileEntityType.Builder.of(SteamGeneratorTileEntity::new, ModBlocks.STEAM_GENERATOR.get()).build(null));
    public static final RegistryObject<TileEntityType<ChargingStationTileEntity>> CHARGING_STATION = TILE_ENTITIES.register(
            "charging_station", () -> TileEntityType.Builder.of(ChargingStationTileEntity::new, ModBlocks.CHARGING_STATION.get()).build(null));
    public static final RegistryObject<TileEntityType<CrusherTileEntity>> CRUSHER = TILE_ENTITIES.register(
            "crusher", () -> TileEntityType.Builder.of(CrusherTileEntity::new, ModBlocks.CRUSHER.get()).build(null));
    public static final RegistryObject<TileEntityType<StampingPressTileEntity>> STAMPING_PRESS = TILE_ENTITIES.register(
            "stamping_press", () -> TileEntityType.Builder.of(StampingPressTileEntity::new, ModBlocks.STAMPING_PRESS.get()).build(null));
    public static final RegistryObject<TileEntityType<IndustrialMixerTileEntity>> INDUSTRIAL_MIXER = TILE_ENTITIES.register(
            "industrial_mixer", () -> TileEntityType.Builder.of(IndustrialMixerTileEntity::new, ModBlocks.INDUSTRIAL_MIXER.get()).build(null));
    public static final RegistryObject<TileEntityType<IndustrialAssemblerTileEntity>> INDUSTRIAL_ASSEMBLER = TILE_ENTITIES.register(
            "industrial_assembler", () -> TileEntityType.Builder.of(IndustrialAssemblerTileEntity::new, ModBlocks.INDUSTRIAL_ASSEMBLER.get()).build(null));
    public static final RegistryObject<TileEntityType<AutoCrafterTileEntity>> AUTO_CRAFTER = TILE_ENTITIES.register(
            "auto_crafter", () -> TileEntityType.Builder.of(AutoCrafterTileEntity::new, ModBlocks.AUTO_CRAFTER.get()).build(null));
    public static final RegistryObject<TileEntityType<VacuumCollectorTileEntity>> VACUUM_COLLECTOR = TILE_ENTITIES.register(
            "vacuum_collector", () -> TileEntityType.Builder.of(VacuumCollectorTileEntity::new, ModBlocks.VACUUM_COLLECTOR.get()).build(null));
    public static final RegistryObject<TileEntityType<EnergyCellTileEntity>> ENERGY_CELL = TILE_ENTITIES.register(
            "energy_cell", () -> TileEntityType.Builder.of(EnergyCellTileEntity::new, ModBlocks.ENERGY_CELL.get()).build(null));
    public static final RegistryObject<TileEntityType<ItemBufferTileEntity>> ITEM_BUFFER = TILE_ENTITIES.register(
            "item_buffer", () -> TileEntityType.Builder.of(ItemBufferTileEntity::new, ModBlocks.ITEM_BUFFER.get()).build(null));
    public static final RegistryObject<TileEntityType<BasicEnergyCableTileEntity>> BASIC_ENERGY_CABLE = TILE_ENTITIES.register(
            "basic_energy_cable", () -> TileEntityType.Builder.of(BasicEnergyCableTileEntity::new, ModBlocks.BASIC_ENERGY_CABLE.get()).build(null));
    public static final RegistryObject<TileEntityType<BasicItemPipeTileEntity>> BASIC_ITEM_PIPE = TILE_ENTITIES.register(
            "basic_item_pipe", () -> TileEntityType.Builder.of(BasicItemPipeTileEntity::new, ModBlocks.BASIC_ITEM_PIPE.get()).build(null));
    public static final RegistryObject<TileEntityType<BasicFluidPipeTileEntity>> BASIC_FLUID_PIPE = TILE_ENTITIES.register(
            "basic_fluid_pipe", () -> TileEntityType.Builder.of(BasicFluidPipeTileEntity::new, ModBlocks.BASIC_FLUID_PIPE.get()).build(null));
    public static final RegistryObject<TileEntityType<FluidReservoirTileEntity>> FLUID_RESERVOIR = TILE_ENTITIES.register(
            "fluid_reservoir", () -> TileEntityType.Builder.of(FluidReservoirTileEntity::new, ModBlocks.FLUID_RESERVOIR.get()).build(null));
    public static final RegistryObject<TileEntityType<FluidPumpTileEntity>> FLUID_PUMP = TILE_ENTITIES.register(
            "fluid_pump", () -> TileEntityType.Builder.of(FluidPumpTileEntity::new, ModBlocks.FLUID_PUMP.get()).build(null));
    public static final RegistryObject<TileEntityType<FluidContainerStationTileEntity>> FLUID_CONTAINER_STATION = TILE_ENTITIES.register(
            "fluid_container_station", () -> TileEntityType.Builder.of(FluidContainerStationTileEntity::new, ModBlocks.FLUID_CONTAINER_STATION.get()).build(null));
    public static final RegistryObject<TileEntityType<FluidGeneratorTileEntity>> FLUID_GENERATOR = TILE_ENTITIES.register(
            "fluid_generator", () -> TileEntityType.Builder.of(FluidGeneratorTileEntity::new, ModBlocks.FLUID_GENERATOR.get()).build(null));
    public static final RegistryObject<TileEntityType<ResistiveFurnaceTileEntity>> RESISTIVE_FURNACE = TILE_ENTITIES.register(
            "resistive_furnace", () -> TileEntityType.Builder.of(ResistiveFurnaceTileEntity::new, ModBlocks.RESISTIVE_FURNACE.get()).build(null));
    public static final RegistryObject<TileEntityType<RodMillTileEntity>> ROD_MILL = TILE_ENTITIES.register(
            "rod_mill", () -> TileEntityType.Builder.of(RodMillTileEntity::new, ModBlocks.ROD_MILL.get()).build(null));
    public static final RegistryObject<TileEntityType<WireMillTileEntity>> WIRE_MILL = TILE_ENTITIES.register(
            "wire_mill", () -> TileEntityType.Builder.of(WireMillTileEntity::new, ModBlocks.WIRE_MILL.get()).build(null));

    private ModTileEntities() {}
}
