package com.ntmspace.probe.platform.neoforge;

//? if neoforge {
import com.ntmspace.probe.ProbeEntry;
import com.ntmspace.probe.ProbeGameTests;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.ModEntries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(ProbeEntry.MOD_ID)
public final class ProbeNeoForge {

    public ProbeNeoForge(IEventBus bus) {
        ModEntries.start(new ProbeEntry());
        if (dev.golem.core.platform.Platform.isClient()) com.ntmspace.probe.client.ProbeClient.init();
        bus.addListener((RegisterGameTestsEvent event) -> GameTests.register(event, ProbeGameTests.TESTS));
    }
}
//?}
