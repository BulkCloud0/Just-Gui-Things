package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.world.block.BasicEnergyCableBlock;
import com.bulkcloud0.justguithings.world.block.CoalGeneratorBlock;
import com.bulkcloud0.justguithings.world.block.CrusherBlock;
import com.bulkcloud0.justguithings.world.block.EnergyCellBlock;
import com.bulkcloud0.justguithings.world.block.StampingPressBlock;
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
                    .strength(3.5F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(1)));

    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
            () -> new CrusherBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(1)));

    public static final RegistryObject<Block> STAMPING_PRESS = BLOCKS.register("stamping_press",
            () -> new StampingPressBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(1)));

    public static final RegistryObject<Block> ENERGY_CELL = BLOCKS.register("energy_cell",
            () -> new EnergyCellBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(4.0F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(1)));

    public static final RegistryObject<Block> BASIC_ENERGY_CABLE = BLOCKS.register("basic_energy_cable",
            () -> new BasicEnergyCableBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(1.5F)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)));

    private ModBlocks() {}
}
