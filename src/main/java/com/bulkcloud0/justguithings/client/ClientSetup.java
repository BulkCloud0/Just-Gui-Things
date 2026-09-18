package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.client.screen.CoalGeneratorScreen;
import com.bulkcloud0.justguithings.client.screen.CrusherScreen;
import com.bulkcloud0.justguithings.client.screen.EnergyCellScreen;
import com.bulkcloud0.justguithings.client.screen.IndustrialMixerScreen;
import com.bulkcloud0.justguithings.client.screen.FluidReservoirScreen;
import com.bulkcloud0.justguithings.client.screen.QuenchChamberScreen;
import com.bulkcloud0.justguithings.client.screen.StampingPressScreen;
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
        event.enqueueWork(() -> {
            ScreenManager.register(ModContainers.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
            ScreenManager.register(ModContainers.CRUSHER.get(), CrusherScreen::new);
            ScreenManager.register(ModContainers.STAMPING_PRESS.get(), StampingPressScreen::new);
            ScreenManager.register(ModContainers.INDUSTRIAL_MIXER.get(), IndustrialMixerScreen::new);
            ScreenManager.register(ModContainers.QUENCH_CHAMBER.get(), QuenchChamberScreen::new);
            ScreenManager.register(ModContainers.ENERGY_CELL.get(), EnergyCellScreen::new);
            ScreenManager.register(ModContainers.FLUID_RESERVOIR.get(), FluidReservoirScreen::new);
        });
    }
}
