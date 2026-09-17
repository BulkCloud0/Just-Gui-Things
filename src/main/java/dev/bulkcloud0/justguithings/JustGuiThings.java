package dev.bulkcloud0.justguithings;

import dev.bulkcloud0.justguithings.registry.ModBlocks;
import dev.bulkcloud0.justguithings.registry.ModContainers;
import dev.bulkcloud0.justguithings.registry.ModItems;
import dev.bulkcloud0.justguithings.registry.ModRecipes;
import dev.bulkcloud0.justguithings.registry.ModTileEntities;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JustGuiThings.MOD_ID)
public class JustGuiThings {
    public static final String MOD_ID = "justguithings";

    public static final ItemGroup TAB = new ItemGroup(MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.CRUSHER.get());
        }
    };

    public JustGuiThings() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModTileEntities.TILE_ENTITIES.register(bus);
        ModContainers.CONTAINERS.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
    }
}
