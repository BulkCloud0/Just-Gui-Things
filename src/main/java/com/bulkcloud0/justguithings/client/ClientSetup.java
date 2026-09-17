package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.client.screen.CoalGeneratorScreen;
import com.bulkcloud0.justguithings.registry.ModContainers;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = JustGuiThings.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ScreenManager.registerFactory(ModContainers.COAL_GENERATOR.get(), CoalGeneratorScreen::new));
    }
}
