package com.ntmspace.platform.neoforge;

//? if neoforge {
import com.ntmspace.NtmSpace;
import com.ntmspace.SpaceEntry;
import com.ntmspace.gametest.SpaceGameTests;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.ModEntries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(NtmSpace.MOD_ID)
public final class NeoForgeInit {

    public NeoForgeInit(IEventBus bus) {
        ModEntries.start(new SpaceEntry());
        if (dev.golem.core.platform.Platform.isClient()) com.ntmspace.client.SpaceClient.init();
        bus.addListener((RegisterGameTestsEvent event) -> GameTests.register(event, SpaceGameTests.TESTS));
    }
}
//?}
