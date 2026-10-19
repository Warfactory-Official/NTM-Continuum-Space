package com.ntmspace.probe.platform.forge;

//? if forge {
/*import com.ntmspace.probe.ProbeEntry;
import com.ntmspace.probe.ProbeGameTests;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.ModEntries;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ProbeEntry.MOD_ID)
public final class ProbeForge {

    public ProbeForge() {
        ModEntries.start(new ProbeEntry());
        if (dev.golem.core.platform.Platform.isClient()) com.ntmspace.probe.client.ProbeClient.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterGameTestsEvent event) ->
                GameTests.register(event, ProbeGameTests.TESTS));
    }
}
*///?}
