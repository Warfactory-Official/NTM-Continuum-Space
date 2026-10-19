package com.ntmspace.war;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.ntmspace.satellite.SatelliteSky;
import com.ntmspace.satellite.SatelliteWar;
import dev.golem.core.platform.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * The war satellites in orbit (hbm-space {@code SatelliteWar} / {@code SatelliteRailgun}): their firing animation and
 * the railguns the client draws in the sky.
 *
 * <ul>
 *   <li>Firing: {@link SatelliteWar#fire()} advances a railgun's flash ({@code interp}) while it may fire. 1.7 declared it
 *       but no code called it (nor {@code onClick}, which arms it: the war controller's "fire" packet is commented out),
 *       so in play a railgun never fires and its flash stays 0. The port ticks it every server tick for every railgun,
 *       which changes nothing unless something armed it (the test command {@code /ntmwar railgun <freq> fire}).</li>
 *   <li>Sky: 1.7 synced every satellite of the player's world ({@code SatelliteSavedData.getClientSats}, perma-sync,
 *       with {@code interp} in the railgun's bytes) and the sky drew each with its {@code render}. The general satellite
 *       sky cache ({@link SatelliteSky}) carries them, railguns with their {@code interp}; {@link #clientRailguns} is its
 *       railgun part.</li>
 * </ul>
 */
public final class WarSatellites {

    private WarSatellites() {}

    /** One railgun the client draws: its frequency (1.7 sky position seed) and its flash. */
    public record Railgun(int freq, float interp) {}

    /** Client: the railguns of the player's sky (1.7 {@code clientSats}, railguns only), from {@link SatelliteSky#client()}. */
    public static List<Railgun> clientRailguns() {
        List<Railgun> out = new ArrayList<>();
        for (SatelliteSky.Sat sat : SatelliteSky.client().satellites()) {
            if (sat.type().equals("ntmspace:railgun")) out.add(new Railgun(sat.freq(), sat.interp()));
        }
        return out;
    }

    public static void init() {
        Lifecycle.onServerTick(WarSatellites::tick);
    }

    /** The client half lives in {@link SatelliteSky#initClient()}; kept for the war family's client start-up. */
    public static void initClient() {}

    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            // interp and canFire are not saved (1.7), so nothing here dirties the save
            for (Satellite sat : SatelliteSavedData.get(level).sats.values()) {
                if (sat instanceof SatelliteWar war) war.fire();
            }
        }
    }
}
