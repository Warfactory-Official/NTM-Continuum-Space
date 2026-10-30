package com.ntmspace.gametest.despaceimport;

import com.ntmspace.NtmSpace;
import dev.golem.core.gametest.GameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The {@code ntmspace:despace_*} GameTests, found by annotation; release jars drop this package. */
@EventBusSubscriber(modid = NtmSpace.MOD_ID)
public final class DeSpaceImportGameTestsNeoForge {
    private DeSpaceImportGameTestsNeoForge() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, DeSpaceImportGameTests.TESTS);
    }
}
