package dev.bulkcloud0.justguithings.client;

import dev.bulkcloud0.justguithings.JustGuiThings;
import dev.bulkcloud0.justguithings.client.screen.CoalGeneratorScreen;
import dev.bulkcloud0.justguithings.client.screen.CrusherScreen;
import dev.bulkcloud0.justguithings.registry.ModContainers;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = JustGuiThings.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ScreenManager.registerFactory(ModContainers.CRUSHER.get(), CrusherScreen::new);
            ScreenManager.registerFactory(ModContainers.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        });
    }
}
