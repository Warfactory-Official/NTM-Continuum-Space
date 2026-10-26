package com.ntmspace.satellite;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.platform.Lifecycle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a client draws in its sky that only the server knows: the satellites in orbit around the player's level and the
 * orbital stations above it (hbm-space's perma-sync: {@code SatelliteSavedData.setClientSats} and
 * {@code OrbitalStation.orbitingStations}, read by {@code SkyProviderCelestial}).
 *
 * <ul>
 *   <li>Satellites: every satellite of the player's level ({@code clientSats} = the satellites of the player's
 *       dimension), with the orbit settings the sky draws it by (inclination, altitude, phase, blinking, colour;
 *       {@link SpaceSatellites#orbit(Satellite)}) and a railgun's firing flash ({@code interp}). None in orbit, where 1.7's
 *       sky drew none.</li>
 *   <li>Stations: every built station whose orbited body's surface is the player's level (1.7
 *       {@code hasStation && orbiting.dimensionId == player.dimension}; a body without a level of its own counted as
 *       Kerbin's, as {@link Orbit#levelOf} does), by its grid position, which seeds its path in the sky.</li>
 * </ul>
 * Sent to each player whenever their view changes (1.7 resent it every tick); the client keeps the last one
 * ({@link #client()}).
 */
public final class SatelliteSky {

    private SatelliteSky() {}

    /** One satellite as the sky draws it: its frequency (the sky seed), type name, orbit settings and railgun flash. */
    public record Sat(int freq, String type, SatelliteOrbit orbit, float interp) {}

    /** One station by its grid position ({@code dX}, {@code dZ}). */
    public record Station(int x, int z) {}

    public record View(List<Sat> satellites, List<Station> stations) {
        public static final View EMPTY = new View(List.of(), List.of());
    }

    public record Payload(View view) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.ofRegistry(Payload.class, Ids.of(NtmSpace.MOD_ID, "sky_objects"),
            SatelliteSky::write, SatelliteSky::read);

    private static void write(Payload payload, RegistryFriendlyByteBuf buf) {
        View view = payload.view();
        buf.writeVarInt(view.satellites().size());
        for (Sat sat : view.satellites()) {
            buf.writeInt(sat.freq());
            buf.writeUtf(sat.type());
            SatelliteOrbit o = sat.orbit();
            buf.writeFloat(o.inclination());
            buf.writeFloat(o.altitude());
            buf.writeFloat(o.phaseOffset());
            buf.writeBoolean(o.blinking());
            buf.writeFloat(o.blinkPeriod());
            buf.writeUtf(o.owner());
            buf.writeFloat(o.colorR());
            buf.writeFloat(o.colorG());
            buf.writeFloat(o.colorB());
            buf.writeFloat(sat.interp());
        }
        buf.writeVarInt(view.stations().size());
        for (Station station : view.stations()) {
            buf.writeInt(station.x());
            buf.writeInt(station.z());
        }
    }

    private static Payload read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Sat> sats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int freq = buf.readInt();
            String type = buf.readUtf();
            SatelliteOrbit orbit = new SatelliteOrbit(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                    buf.readFloat(), buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readFloat());
            sats.add(new Sat(freq, type, orbit, buf.readFloat()));
        }
        int stationCount = buf.readVarInt();
        List<Station> stations = new ArrayList<>(stationCount);
        for (int i = 0; i < stationCount; i++) stations.add(new Station(buf.readInt(), buf.readInt()));
        return new Payload(new View(List.copyOf(sats), List.copyOf(stations)));
    }

    /** Client: the last view the server sent. */
    private static volatile View client = View.EMPTY;

    public static View client() {
        return client;
    }

    private static final Map<UUID, View> SENT = new HashMap<>();

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC);
        Lifecycle.onServerTick(SatelliteSky::tick);
        Lifecycle.onServerStopping(server -> SENT.clear());
    }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> client = payload.view());
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GolemNetwork.canSendToPlayer(player, CODEC)) continue;
            View view = view((ServerLevel) player.level());
            if (view.equals(SENT.get(player.getUUID()))) continue;
            SENT.put(player.getUUID(), view);
            GolemNetwork.sendToPlayer(player, CODEC, new Payload(view));
        }
        SENT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
    }

    /** What a player in {@code level} sees: see the class comment. */
    public static View view(ServerLevel level) {
        if (Orbit.inOrbit(level)) return View.EMPTY;
        List<Sat> sats = new ArrayList<>();
        for (Map.Entry<Integer, Satellite> entry : SatelliteSavedData.get(level).sats.entrySet()) {
            Satellite sat = entry.getValue();
            if (!SpaceSatellites.isSpace(sat.type())) continue;
            float interp = sat instanceof SatelliteRailgun railgun ? railgun.interp : 0.0F;
            sats.add(new Sat(entry.getKey(), sat.type().name(), SpaceSatellites.orbit(sat), interp));
        }
        sats.sort(Comparator.comparingInt(Sat::freq));
        List<Station> stations = new ArrayList<>();
        for (OrbitalStation station : StationData.get(level.getServer()).getStations()) {
            if (!station.hasStation || station.orbiting == null) continue;
            if (!Orbit.levelOf(station.orbiting).equals(level.dimension())) continue;
            stations.add(new Station(station.dX, station.dZ));
        }
        stations.sort(Comparator.comparingInt(Station::x).thenComparingInt(Station::z));
        return new View(List.copyOf(sats), List.copyOf(stations));
    }
}
