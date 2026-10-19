package com.ntmspace.world;

import com.ntmspace.world.gen.legacy.DresLayers17;
import com.ntmspace.world.gen.legacy.DunaLayers17;
import com.ntmspace.world.gen.legacy.EveLayers17;
import com.ntmspace.world.gen.legacy.LaytheLayers17;
import com.ntmspace.world.gen.legacy.MinmusLayers17;
import com.ntmspace.world.gen.legacy.MohoLayers17;
import com.ntmspace.world.gen.legacy.TektoLayers17;
import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongFunction;

/**
 * The celestial bodies whose biomes come from a 1.7 {@code GenLayer} stack, for {@link LayeredBiomeSource}: each body's
 * stack and the biomes its detail chain produces, by name. The names are the keys of the {@code biomes} map in the
 * body's dimension JSON; the ids are hbm-space's {@code SpaceConfig} defaults, which the stacks compare against.
 */
public final class LayeredBodies {

    /** A body: its 1.7 biome stack ({@code createBiomeGenerators}) and its biomes, name to 1.7 id. */
    public record Body(String name, LongFunction<BiomeLayers17> layers, Map<String, Integer> biomes) {}

    private static final Map<String, Body> BODIES = new LinkedHashMap<>();

    public static final Body DUNA = add("duna", DunaLayers17::create,
            "plains", DunaLayers17.PLAINS, "lowlands", DunaLayers17.LOWLANDS, "polar", DunaLayers17.POLAR,
            "hills", DunaLayers17.HILLS, "polar_hills", DunaLayers17.POLAR_HILLS);
    public static final Body DRES = add("dres", DresLayers17::create,
            "plains", DresLayers17.PLAINS, "canyon", DresLayers17.CANYON);
    public static final Body EVE = add("eve", EveLayers17::create,
            "plains", EveLayers17.PLAINS, "mountains", EveLayers17.MOUNTAINS, "ocean", EveLayers17.OCEAN,
            "seismic_plains", EveLayers17.SEISMIC_PLAINS, "river", EveLayers17.RIVER);
    public static final Body MOHO = add("moho", MohoLayers17::create,
            "crag", MohoLayers17.CRAG, "basalt", MohoLayers17.BASALT, "lava_sea", MohoLayers17.LAVA_SEA,
            "plateau", MohoLayers17.PLATEAU);
    public static final Body MINMUS = add("minmus", MinmusLayers17::create,
            "hills", MinmusLayers17.HILLS, "basins", MinmusLayers17.BASINS);
    public static final Body LAYTHE = add("laythe", LaytheLayers17::create,
            "islands", LaytheLayers17.ISLANDS, "ocean", LaytheLayers17.OCEAN, "polar", LaytheLayers17.POLAR,
            "coast", LaytheLayers17.COAST);
    public static final Body TEKTO = add("tekto", TektoLayers17::create,
            "polyvinyl_plains", TektoLayers17.POLYVINYL_PLAINS, "halogen_hills", TektoLayers17.HALOGEN_HILLS,
            "tetrachloric_river", TektoLayers17.TETRACHLORIC_RIVER, "forest", TektoLayers17.FOREST,
            "vinyl_sands", TektoLayers17.VINYL_SANDS);

    private LayeredBodies() {}

    private static Body add(String name, LongFunction<BiomeLayers17> layers, Object... biomes) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < biomes.length; i += 2) map.put((String) biomes[i], (Integer) biomes[i + 1]);
        Body body = new Body(name, layers, java.util.Collections.unmodifiableMap(map));
        BODIES.put(name, body);
        return body;
    }

    /** The body called {@code name}, or null. */
    public static Body byName(String name) {
        return BODIES.get(name);
    }

    /** Every layered body, in declaration order. */
    public static java.util.Collection<Body> all() {
        return java.util.Collections.unmodifiableCollection(BODIES.values());
    }
}
