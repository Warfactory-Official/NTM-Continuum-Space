package com.ntmspace.platform.neoforge;

//? if neoforge {
import com.ntmspace.NtmSpace;
import com.ntmspace.SpaceEntry;
import dev.golem.core.platform.ModEntries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NtmSpace.MOD_ID)
public final class NeoForgeInit {

    public NeoForgeInit(IEventBus bus) {
        ModEntries.start(new SpaceEntry());
        if (dev.golem.core.platform.Platform.isClient()) com.ntmspace.client.SpaceClient.init();
    }
}
//?}
