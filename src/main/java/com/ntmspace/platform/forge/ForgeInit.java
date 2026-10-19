package com.ntmspace.platform.forge;

//? if forge {
/*import com.ntmspace.NtmSpace;
import com.ntmspace.SpaceEntry;
import com.ntmspace.gametest.SpaceGameTests;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.ModEntries;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NtmSpace.MOD_ID)
public final class ForgeInit {

    public ForgeInit() {
        ModEntries.start(new SpaceEntry());
        if (dev.golem.core.platform.Platform.isClient()) com.ntmspace.client.SpaceClient.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterGameTestsEvent event) ->
                GameTests.register(event, SpaceGameTests.TESTS));
    }
}
*///?}
