package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.block.AutoCrafterBlock;
import com.bulkcloud0.justguithings.world.block.VacuumCollectorBlock;
import com.bulkcloud0.justguithings.world.block.BasicEnergyCableBlock;
import com.bulkcloud0.justguithings.world.block.BasicFluidPipeBlock;
import com.bulkcloud0.justguithings.world.block.BasicItemPipeBlock;
import com.bulkcloud0.justguithings.world.block.ChargingStationBlock;
import com.bulkcloud0.justguithings.world.block.CoalGeneratorBlock;
import com.bulkcloud0.justguithings.world.block.CrusherBlock;
import com.bulkcloud0.justguithings.world.block.EnergyCellBlock;
import com.bulkcloud0.justguithings.world.block.IndustrialMixerBlock;
import com.bulkcloud0.justguithings.world.block.IndustrialAssemblerBlock;
import com.bulkcloud0.justguithings.world.block.ItemBufferBlock;
import com.bulkcloud0.justguithings.world.block.FluidReservoirBlock;
import com.bulkcloud0.justguithings.world.block.FluidPumpBlock;
import com.bulkcloud0.justguithings.world.block.FluidContainerStationBlock;
import com.bulkcloud0.justguithings.world.block.FluidGeneratorBlock;
import com.bulkcloud0.justguithings.world.block.ResistiveFurnaceBlock;
import com.bulkcloud0.justguithings.world.block.RodMillBlock;
import com.bulkcloud0.justguithings.world.block.StampingPressBlock;
import com.bulkcloud0.justguithings.world.block.SteamGeneratorBlock;
import com.bulkcloud0.justguithings.world.block.WireMillBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, JustGuiThings.MOD_ID);

    public static final RegistryObject<Block> COAL_GENERATOR = BLOCKS.register("coal_generator",
            () -> new CoalGeneratorBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(3.5F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> STEAM_GENERATOR = BLOCKS.register("steam_generator",
            () -> new SteamGeneratorBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.5F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> CHARGING_STATION = BLOCKS.register("charging_station",
            () -> new ChargingStationBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
            () -> new CrusherBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> STAMPING_PRESS = BLOCKS.register("stamping_press",
            () -> new StampingPressBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> INDUSTRIAL_MIXER = BLOCKS.register("industrial_mixer",
            () -> new IndustrialMixerBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> INDUSTRIAL_ASSEMBLER = BLOCKS.register("industrial_assembler",
            () -> new IndustrialAssemblerBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.5F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> AUTO_CRAFTER = BLOCKS.register("auto_crafter",
            () -> new AutoCrafterBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> VACUUM_COLLECTOR = BLOCKS.register("vacuum_collector",
            () -> new VacuumCollectorBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> ENERGY_CELL = BLOCKS.register("energy_cell",
            () -> new EnergyCellBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> ITEM_BUFFER = BLOCKS.register("item_buffer",
            () -> new ItemBufferBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> BASIC_ENERGY_CABLE = BLOCKS.register("basic_energy_cable",
            () -> new BasicEnergyCableBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(1.5F).harvestTool(ToolType.PICKAXE).harvestLevel(0)));
    public static final RegistryObject<Block> BASIC_ITEM_PIPE = BLOCKS.register("basic_item_pipe",
            () -> new BasicItemPipeBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(1.5F).harvestTool(ToolType.PICKAXE).harvestLevel(0)));
    public static final RegistryObject<Block> BASIC_FLUID_PIPE = BLOCKS.register("basic_fluid_pipe",
            () -> new BasicFluidPipeBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(1.5F).harvestTool(ToolType.PICKAXE).harvestLevel(0)));
    public static final RegistryObject<Block> FLUID_RESERVOIR = BLOCKS.register("fluid_reservoir",
            () -> new FluidReservoirBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> FLUID_PUMP = BLOCKS.register("fluid_pump",
            () -> new FluidPumpBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> FLUID_CONTAINER_STATION = BLOCKS.register("fluid_container_station",
            () -> new FluidContainerStationBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> FLUID_GENERATOR = BLOCKS.register("fluid_generator",
            () -> new FluidGeneratorBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> RESISTIVE_FURNACE = BLOCKS.register("resistive_furnace",
            () -> new ResistiveFurnaceBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> ROD_MILL = BLOCKS.register("rod_mill",
            () -> new RodMillBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.5F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));
    public static final RegistryObject<Block> WIRE_MILL = BLOCKS.register("wire_mill",
            () -> new WireMillBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.5F).harvestTool(ToolType.PICKAXE).harvestLevel(1)));

    private ModBlocks() {}
}
