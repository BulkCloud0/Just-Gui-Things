package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicFluidPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.BasicItemPipeTileEntity;
import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;
import com.bulkcloud0.justguithings.world.tile.CrusherTileEntity;
import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import com.bulkcloud0.justguithings.world.tile.IndustrialMixerTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidReservoirTileEntity;
import com.bulkcloud0.justguithings.world.tile.FluidPumpTileEntity;
import com.bulkcloud0.justguithings.world.tile.ResistiveFurnaceTileEntity;
import com.bulkcloud0.justguithings.world.tile.PrecisionExtruderTileEntity;
import com.bulkcloud0.justguithings.world.tile.StampingPressTileEntity;
import com.bulkcloud0.justguithings.world.tile.WireMillTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, JustGuiThings.MOD_ID);

    public static final RegistryObject<TileEntityType<CoalGeneratorTileEntity>> COAL_GENERATOR = TILE_ENTITIES.register(
            "coal_generator", () -> TileEntityType.Builder.of(CoalGeneratorTileEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null));
    public static final RegistryObject<TileEntityType<CrusherTileEntity>> CRUSHER = TILE_ENTITIES.register(
            "crusher", () -> TileEntityType.Builder.of(CrusherTileEntity::new, ModBlocks.CRUSHER.get()).build(null));
    public static final RegistryObject<TileEntityType<StampingPressTileEntity>> STAMPING_PRESS = TILE_ENTITIES.register(
            "stamping_press", () -> TileEntityType.Builder.of(StampingPressTileEntity::new, ModBlocks.STAMPING_PRESS.get()).build(null));
    public static final RegistryObject<TileEntityType<IndustrialMixerTileEntity>> INDUSTRIAL_MIXER = TILE_ENTITIES.register(
            "industrial_mixer", () -> TileEntityType.Builder.of(IndustrialMixerTileEntity::new, ModBlocks.INDUSTRIAL_MIXER.get()).build(null));
    public static final RegistryObject<TileEntityType<EnergyCellTileEntity>> ENERGY_CELL = TILE_ENTITIES.register(
            "energy_cell", () -> TileEntityType.Builder.of(EnergyCellTileEntity::new, ModBlocks.ENERGY_CELL.get()).build(null));
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

    public static final RegistryObject<TileEntityType<ResistiveFurnaceTileEntity>> RESISTIVE_FURNACE = TILE_ENTITIES.register(
            "resistive_furnace", () -> TileEntityType.Builder.of(ResistiveFurnaceTileEntity::new, ModBlocks.RESISTIVE_FURNACE.get()).build(null));

    public static final RegistryObject<TileEntityType<PrecisionExtruderTileEntity>> PRECISION_EXTRUDER = TILE_ENTITIES.register(
            "precision_extruder", () -> TileEntityType.Builder.of(PrecisionExtruderTileEntity::new, ModBlocks.PRECISION_EXTRUDER.get()).build(null));

    public static final RegistryObject<TileEntityType<WireMillTileEntity>> WIRE_MILL = TILE_ENTITIES.register(
            "wire_mill", () -> TileEntityType.Builder.of(WireMillTileEntity::new, ModBlocks.WIRE_MILL.get()).build(null));

    private ModTileEntities() {}
}
