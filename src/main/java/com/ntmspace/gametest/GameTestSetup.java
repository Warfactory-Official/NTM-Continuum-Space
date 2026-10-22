package com.ntmspace.gametest;

//? if neoforge {
import com.ntmspace.NtmSpace;
import dev.golem.core.gametest.GameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Declares the GameTest suite while mods construct and hands it to NeoForge. Found by annotation, so release jars hold no call to it. */
@EventBusSubscriber(modid = NtmSpace.MOD_ID)
public final class GameTestSetup {

    private GameTestSetup() {}

    @SubscribeEvent
    public static void construct(FMLConstructModEvent event) {
        SpaceGameTests.initAll();
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, SpaceGameTests.TESTS);
    }
}
//?} elif forge {
/*import com.ntmspace.NtmSpace;
import dev.golem.core.gametest.GameTests;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

/^* Declares the GameTest suite while mods construct and hands it to Forge. Found by annotation, so release jars hold no call to it. ^/
@Mod.EventBusSubscriber(modid = NtmSpace.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GameTestSetup {

    private GameTestSetup() {}

    @SubscribeEvent
    public static void construct(FMLConstructModEvent event) {
        SpaceGameTests.initAll();
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, SpaceGameTests.TESTS);
    }
}
*///?}
