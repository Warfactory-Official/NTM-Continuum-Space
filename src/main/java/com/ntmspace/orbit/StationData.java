package com.ntmspace.orbit;

import com.ntmspace.data.SpaceData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.world.CompatSavedData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * The orbital stations' part of hbm-space's {@code SolarSystemWorldSavedData} ({@code "stations"}), kept on the
 * overworld: one entry per grid square ever used, with its orbited and target bodies <b>by name</b>, its travel state
 * <b>by name</b> (1.7 saved the ordinal), the timers, whether a core was launched there, its name and its gravity switch.
 * The body traits of the same 1.7 file belong to the atmosphere port.
 */
public final class StationData extends CompatSavedData<StationData> {

    public static final String ID = NtmSpace.MOD_ID + ":stations";

    /** One saved station: 1.7's tag {@code x, z, orbiting, target, state, stateTimer, maxStateTimer, hasStation, name, gravity}. */
    record Entry(int x, int z, String orbiting, String target, String state, int stateTimer, int maxStateTimer,
                 boolean hasStation, String name, float gravity) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(Entry::x),
                Codec.INT.fieldOf("z").forGetter(Entry::z),
                Codec.STRING.fieldOf("orbiting").forGetter(Entry::orbiting),
                Codec.STRING.fieldOf("target").forGetter(Entry::target),
                Codec.STRING.fieldOf("state").forGetter(Entry::state),
                Codec.INT.fieldOf("stateTimer").forGetter(Entry::stateTimer),
                Codec.INT.fieldOf("maxStateTimer").forGetter(Entry::maxStateTimer),
                Codec.BOOL.fieldOf("hasStation").forGetter(Entry::hasStation),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name),
                // 1.7: stationTag.hasKey("gravity") ? getFloat("gravity") : 1
                Codec.FLOAT.optionalFieldOf("gravity", 1F).forGetter(Entry::gravity)
        ).apply(instance, Entry::new));

        static Entry of(OrbitalStation station) {
            return new Entry(station.dX, station.dZ, station.orbiting.name, station.target.name, station.state.name(),
                    station.stateTimer, station.maxStateTimer, station.hasStation, station.name, station.gravityMultiplier);
        }

        OrbitalStation toStation() {
            OrbitalStation station = new OrbitalStation(OrbitalStation.bodyOrKerbin(orbiting), x, z);
            station.target = OrbitalStation.bodyOrKerbin(target);
            station.state = OrbitalStation.stateOf(state);
            station.stateTimer = stateTimer;
            station.maxStateTimer = maxStateTimer;
            station.hasStation = hasStation;
            station.name = name;
            station.gravityMultiplier = gravity;
            return station;
        }
    }

    public static final Codec<StationData> CODEC = Entry.CODEC.listOf().optionalFieldOf("stations", List.of()).codec()
            .xmap(StationData::new, StationData::entries);

    private final Random rand = new Random();
    private final Map<Long, OrbitalStation> stations = new LinkedHashMap<>();

    public StationData() {}

    private StationData(List<Entry> entries) {
        for (Entry entry : entries) stations.put(key(entry.x(), entry.z()), entry.toStation());
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (OrbitalStation station : stations.values()) entries.add(Entry.of(station));
        return entries;
    }

    /** A grid square as one key (1.7 used a {@code ChunkCoordIntPair}). */
    static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    @Override
    protected Codec<StationData> codec() {
        return CODEC;
    }

    /** The stations of this server (on the overworld's data storage, as 1.7 kept them on dimension 0). */
    public static StationData get(MinecraftServer server) {
        return get(server.overworld(), ID, StationData::new, CODEC);
    }

    public Collection<OrbitalStation> getStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    /** Grabs an existing station for block {@code x, z} of the orbit level, or null. */
    public @Nullable OrbitalStation getStationFromPosition(int x, int z) {
        return stations.get(key(OrbitalStation.gridOf(x), OrbitalStation.gridOf(z)));
    }

    /**
     * 1.7 {@code findFreeSpace}: a random unused square within {@link Orbit#MAX_STATION_DISTANCE}; after 256 tries a
     * reserved but never launched square is accepted too.
     */
    public int[] findFreeSpace() {
        int size = SpaceData.MAX_STATION_DISTANCE.get() / OrbitalStation.STATION_SIZE;

        int[] pos = null;
        for (int i = 0; i < 512; i++) {
            pos = new int[] {rand.nextInt(size * 2) - size, rand.nextInt(size * 2) - size};
            OrbitalStation existing = stations.get(key(pos[0], pos[1]));
            if (existing == null) return pos;
            if (i > 256 && !existing.hasStation) return pos;
        }

        return pos;
    }

    /** Finds an unoccupied square and adds a new station there. */
    public OrbitalStation addStation(SolarSystem.Body orbiting) {
        int[] pos = findFreeSpace();
        return addStation(pos[0], pos[1], orbiting);
    }

    /** Adds a station at grid square {@code x, z}; never overwrites an existing one. */
    public OrbitalStation addStation(int x, int z, SolarSystem.Body orbiting) {
        long key = key(x, z);
        OrbitalStation station = stations.get(key);

        if (station == null) {
            station = new OrbitalStation(orbiting, x, z);
            stations.put(key, station);
        }

        setDirty();
        return station;
    }

    public void removeStation(OrbitalStation station) {
        removeStation(station.dX, station.dZ);
    }

    /** Removes a temporary station; one with a launched core stays. */
    public void removeStation(int x, int z) {
        long key = key(x, z);
        OrbitalStation station = stations.get(key);
        if (station == null || station.hasStation) return;
        stations.remove(key);
        setDirty();
    }
}
