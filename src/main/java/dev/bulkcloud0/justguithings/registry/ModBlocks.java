package dev.bulkcloud0.justguithings.registry;

import dev.bulkcloud0.justguithings.JustGuiThings;
import dev.bulkcloud0.justguithings.block.CoalGeneratorBlock;
import dev.bulkcloud0.justguithings.block.CrusherBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, JustGuiThings.MOD_ID);

    public static final RegistryObject<Block> MACHINE_FRAME = BLOCKS.register("machine_frame", () ->
            new Block(AbstractBlock.Properties.of(Material.METAL).strength(3.5F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher", () ->
            new CrusherBlock(AbstractBlock.Properties.of(Material.METAL).strength(4.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> COAL_GENERATOR = BLOCKS.register("coal_generator", () ->
            new CoalGeneratorBlock(AbstractBlock.Properties.of(Material.METAL).strength(4.0F).sound(SoundType.METAL)));

    private ModBlocks() {}
}
