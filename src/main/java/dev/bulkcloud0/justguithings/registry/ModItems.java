package dev.bulkcloud0.justguithings.registry;

import dev.bulkcloud0.justguithings.JustGuiThings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JustGuiThings.MOD_ID);

    public static final RegistryObject<Item> MACHINE_FRAME = ITEMS.register("machine_frame", () ->
            new BlockItem(ModBlocks.MACHINE_FRAME.get(), new Item.Properties().tab(JustGuiThings.TAB)));
    public static final RegistryObject<Item> CRUSHER = ITEMS.register("crusher", () ->
            new BlockItem(ModBlocks.CRUSHER.get(), new Item.Properties().tab(JustGuiThings.TAB)));
    public static final RegistryObject<Item> COAL_GENERATOR = ITEMS.register("coal_generator", () ->
            new BlockItem(ModBlocks.COAL_GENERATOR.get(), new Item.Properties().tab(JustGuiThings.TAB)));

    private ModItems() {}
}
