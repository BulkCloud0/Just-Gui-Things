package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.item.EnergyCellBlockItem;
import com.bulkcloud0.justguithings.item.MachineModuleItem;
import com.bulkcloud0.justguithings.item.TooltipItem;
import com.bulkcloud0.justguithings.machine.module.MachineModuleTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JustGuiThings.MOD_ID);

    public static final RegistryObject<Item> COAL_GENERATOR = ITEMS.register("coal_generator",
            () -> new BlockItem(ModBlocks.COAL_GENERATOR.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final RegistryObject<Item> CRUSHER = ITEMS.register("crusher",
            () -> new BlockItem(ModBlocks.CRUSHER.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final RegistryObject<Item> STAMPING_PRESS = ITEMS.register("stamping_press",
            () -> new BlockItem(ModBlocks.STAMPING_PRESS.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final RegistryObject<Item> INDUSTRIAL_MIXER = ITEMS.register("industrial_mixer",
            () -> new BlockItem(ModBlocks.INDUSTRIAL_MIXER.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final RegistryObject<Item> QUENCH_CHAMBER = ITEMS.register("quench_chamber",
            () -> new BlockItem(ModBlocks.QUENCH_CHAMBER.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    public static final RegistryObject<Item> ENERGY_CELL = ITEMS.register("energy_cell",
            () -> new EnergyCellBlockItem(ModBlocks.ENERGY_CELL.get(),
                    new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));
    public static final RegistryObject<Item> BASIC_ENERGY_CABLE = ITEMS.register("basic_energy_cable",
            () -> new BlockItem(ModBlocks.BASIC_ENERGY_CABLE.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> CONFIGURATOR = ITEMS.register("configurator",
            () -> new TooltipItem(new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1),
                    "tooltip.justguithings.configurator.use"));
    public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new MachineModuleItem(new Item.Properties().tab(ItemGroup.TAB_REDSTONE), MachineModuleTypes.SPEED,
                    "tooltip.justguithings.speed_module.effect",
                    "tooltip.justguithings.speed_module.compatibility"));
    public static final RegistryObject<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade",
            () -> new MachineModuleItem(new Item.Properties().tab(ItemGroup.TAB_REDSTONE), MachineModuleTypes.EFFICIENCY,
                    "tooltip.justguithings.efficiency_module.effect",
                    "tooltip.justguithings.efficiency_module.compatibility"));
    public static final RegistryObject<Item> BUFFER_UPGRADE = ITEMS.register("buffer_upgrade",
            () -> new MachineModuleItem(new Item.Properties().tab(ItemGroup.TAB_REDSTONE), MachineModuleTypes.BUFFER,
                    "tooltip.justguithings.buffer_module.effect",
                    "tooltip.justguithings.buffer_module.compatibility"));
    public static final RegistryObject<Item> BATCH_UPGRADE = ITEMS.register("batch_upgrade",
            () -> new MachineModuleItem(new Item.Properties().tab(ItemGroup.TAB_REDSTONE), MachineModuleTypes.BATCH,
                    "tooltip.justguithings.batch_module.effect",
                    "tooltip.justguithings.batch_module.compatibility"));

    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> COAL_DUST = ITEMS.register("coal_dust",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> GOLD_PLATE = ITEMS.register("gold_plate",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> STEEL_BLEND = ITEMS.register("steel_blend",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));
    public static final RegistryObject<Item> TEMPERED_STEEL_INGOT = ITEMS.register("tempered_steel_ingot",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));

    private ModItems() {}
}
