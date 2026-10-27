package com.ntmspace.client;

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
*///?}
//? if forge {
/*import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
*///?}
//? if neoforge {
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}
import com.ntmspace.trait.CelestialTimeSync;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Skies;

/** Receives the server's celestial rate and retires that client state when its connection closes. */
public final class CelestialTimeClient {

    private static boolean installed;

    private CelestialTimeClient() {}

    public static synchronized void init() {
        if (installed) return;
        installed = true;
        GolemNetwork.registerPlayToClient(CelestialTimeSync.CODEC, (payload, context) -> {
            if (context.level() == null) return;
            if (!CelestialTimeSync.applyClient(payload.multiplier())) return;
            Skies.invalidateEnvironment(context.level());
        });
        //? if fabric {
        /*ClientPlayConnectionEvents.DISCONNECT.register(
                (connection, client) -> CelestialTimeSync.applyClient(1));
        *///?} elif neoforge {
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> CelestialTimeSync.applyClient(1));
        //?} elif forge {
        /*MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> CelestialTimeSync.applyClient(1));
        *///?}
    }
}
