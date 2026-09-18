package com.bulkcloud0.justguithings.client;

import com.bulkcloud0.justguithings.JustGuiThings;
import com.bulkcloud0.justguithings.client.screen.CoalGeneratorScreen;
import com.bulkcloud0.justguithings.client.screen.CrusherScreen;
import com.bulkcloud0.justguithings.client.screen.EnergyCellScreen;
import com.bulkcloud0.justguithings.client.screen.IndustrialMixerScreen;
import com.bulkcloud0.justguithings.client.screen.FluidReservoirScreen;
import com.bulkcloud0.justguithings.client.screen.FluidPumpScreen;
import com.bulkcloud0.justguithings.client.screen.ResistiveFurnaceScreen;
import com.bulkcloud0.justguithings.client.screen.PrecisionExtruderScreen;
import com.bulkcloud0.justguithings.client.screen.StampingPressScreen;
import com.bulkcloud0.justguithings.client.screen.WireMillScreen;
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
            ScreenManager.register(ModContainers.ENERGY_CELL.get(), EnergyCellScreen::new);
            ScreenManager.register(ModContainers.FLUID_RESERVOIR.get(), FluidReservoirScreen::new);
            ScreenManager.register(ModContainers.FLUID_PUMP.get(), FluidPumpScreen::new);
            ScreenManager.register(ModContainers.RESISTIVE_FURNACE.get(), ResistiveFurnaceScreen::new);
            ScreenManager.register(ModContainers.PRECISION_EXTRUDER.get(), PrecisionExtruderScreen::new);
            ScreenManager.register(ModContainers.WIRE_MILL.get(), WireMillScreen::new);
        });
    }
}
