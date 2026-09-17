package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.tile.BasicEnergyCableTileEntity;
import com.bulkcloud0.justguithings.world.tile.CoalGeneratorTileEntity;
import com.bulkcloud0.justguithings.world.tile.CrusherTileEntity;
import com.bulkcloud0.justguithings.world.tile.EnergyCellTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, JustGuiThings.MOD_ID);

    public static final RegistryObject<TileEntityType<CoalGeneratorTileEntity>> COAL_GENERATOR = TILE_ENTITIES.register(
            "coal_generator",
            () -> TileEntityType.Builder.of(CoalGeneratorTileEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null));

    public static final RegistryObject<TileEntityType<CrusherTileEntity>> CRUSHER = TILE_ENTITIES.register(
            "crusher",
            () -> TileEntityType.Builder.of(CrusherTileEntity::new, ModBlocks.CRUSHER.get()).build(null));

    public static final RegistryObject<TileEntityType<EnergyCellTileEntity>> ENERGY_CELL = TILE_ENTITIES.register(
            "energy_cell",
            () -> TileEntityType.Builder.of(EnergyCellTileEntity::new, ModBlocks.ENERGY_CELL.get()).build(null));

    public static final RegistryObject<TileEntityType<BasicEnergyCableTileEntity>> BASIC_ENERGY_CABLE = TILE_ENTITIES.register(
            "basic_energy_cable",
            () -> TileEntityType.Builder.of(BasicEnergyCableTileEntity::new, ModBlocks.BASIC_ENERGY_CABLE.get()).build(null));

    private ModTileEntities() {}
}
