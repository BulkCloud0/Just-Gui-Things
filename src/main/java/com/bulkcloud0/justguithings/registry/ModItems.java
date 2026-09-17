package com.bulkcloud0.justguithings.registry;

import com.bulkcloud0.justguithings.JustGuiThings;
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

    public static final RegistryObject<Item> ENERGY_CELL = ITEMS.register("energy_cell",
            () -> new BlockItem(ModBlocks.ENERGY_CELL.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> BASIC_ENERGY_CABLE = ITEMS.register("basic_energy_cable",
            () -> new BlockItem(ModBlocks.BASIC_ENERGY_CABLE.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> CONFIGURATOR = ITEMS.register("configurator",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_REDSTONE).stacksTo(1)));

    public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> BUFFER_UPGRADE = ITEMS.register("buffer_upgrade",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> BATCH_UPGRADE = ITEMS.register("batch_upgrade",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));

    public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MATERIALS)));

    private ModItems() {}
}
