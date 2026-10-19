package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.DresChunkGenerator;
import com.ntmspace.world.DunaChunkGenerator;
import com.ntmspace.world.EveChunkGenerator;
import com.ntmspace.world.LayeredBiomeSource;
import com.ntmspace.world.LayeredBodies;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.LaytheChunkGenerator;
import com.ntmspace.world.MinmusChunkGenerator;
import com.ntmspace.world.MohoChunkGenerator;
import com.ntmspace.world.TektoChunkGenerator;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Biomes;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * The biome source of each body with a 1.7 biome map ({@link LayeredBiomeSource}, {@code "type": "ntmspace:layered"}):
 * decoded from the body's authored dimension JSON through the registered type (so the codec, the registration and the
 * name-to-biome map are what a level loads; each name maps to {@code ntmspace:<body>_<name>}), given its seed by its generator (Golem Core's
 * {@code CompatChunkGenerator.useSeed} passes it on), for a fixed seed every biome cell of six chunks, at four heights,
 * is the biome of the 1.7 detail-chain id of the column at the cell's centre - the same id the body's terrain reads
 * ({@link LayeredBodyTerrain17#biomeIdAt}), whose maps the generator tests pin to the 1.7.10 jar. When the world has the
 * body's dimension (a normal world; a GameTest world may not), its generator's biome source is checked to be this
 * source type, seeded with the level seed.
 */
public final class LayeredBiomeGameTests {

    static final long SEED = 4815162342L;
    static final int[][] CHUNKS = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}, {250, -310}};
    static final int[] QUART_YS = {0, 4, 15, 63};

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function DUNA = test(LayeredBodies.DUNA, DunaChunkGenerator::new);
    public static final GameTests.Function DRES = test(LayeredBodies.DRES, DresChunkGenerator::new);
    public static final GameTests.Function EVE = test(LayeredBodies.EVE, EveChunkGenerator::new);
    public static final GameTests.Function MOHO = test(LayeredBodies.MOHO, MohoChunkGenerator::new);
    public static final GameTests.Function MINMUS = test(LayeredBodies.MINMUS, MinmusChunkGenerator::new);
    public static final GameTests.Function LAYTHE = test(LayeredBodies.LAYTHE, LaytheChunkGenerator::new);
    public static final GameTests.Function TEKTO = test(LayeredBodies.TEKTO, TektoChunkGenerator::new);

    private LayeredBiomeGameTests() {}

    public static void init() {}

    private static GameTests.Function test(LayeredBodies.Body body, Function<BiomeSource, LayeredChunkGenerator> factory) {
        return TESTS.function(body.name() + "_biome_source", 400, () -> helper -> check(helper, body, factory));
    }

    private static void check(GameTestHelper helper, LayeredBodies.Body body,
                              Function<BiomeSource, LayeredChunkGenerator> factory) {
        ServerLevel level = helper.getLevel();
        Map<String, Holder<Biome>> biomes = new LinkedHashMap<>();
        for (String name : body.biomes().keySet()) {
            biomes.put(name, Biomes.holder(level.registryAccess(), Biomes.key(NtmSpace.MOD_ID, body.name() + "_" + name)));
        }

        // the authored dimension's biome source, decoded the way the level loads it
        BiomeSource decoded;
        try (java.io.Reader reader = level.getServer().getResourceManager()
                .getResource(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "dimension/" + body.name() + ".json")).orElseThrow().openAsReader()) {
            com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(reader)
                    .getAsJsonObject().getAsJsonObject("generator").get("biome_source");
            var result = BiomeSource.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
                    level.registryAccess()), json);
            if (result.result().isEmpty()) {
                fail(helper, body.name() + ": the dimension's biome_source does not decode: "
                        + result.error().map(e -> e.message()).orElse("?"));
                return;
            }
            decoded = result.result().get();
        } catch (java.io.IOException | RuntimeException e) {
            fail(helper, body.name() + ": cannot read data/ntmspace/dimension/" + body.name() + ".json: " + e);
            return;
        }
        if (!(decoded instanceof LayeredBiomeSource source) || source.body() != body) {
            fail(helper, body.name() + ": the dimension's biome_source is " + decoded.getClass().getName() + ", not the layered " + body.name());
            return;
        }
        for (Map.Entry<String, Holder<Biome>> entry : biomes.entrySet()) {
            Holder<Biome> named = source.biome(body.biomes().get(entry.getKey()));
            if (named == null || !named.is(entry.getValue().unwrapKey().orElseThrow())) {
                fail(helper, body.name() + ": the dimension maps " + entry.getKey() + " to " + key(named) + ", not " + key(entry.getValue()));
                return;
            }
        }
        LayeredChunkGenerator generator = factory.apply(source);
        generator.useSeed(SEED); // seeds the biome source too
        generator.useSessionSeed(1L);

        Set<String> seen = new HashSet<>();
        int cells = 0;
        LayeredBodyTerrain17 terrain = generator.terrain();
        for (int[] c : CHUNKS) {
            for (int qx = c[0] * 4; qx < c[0] * 4 + 4; qx++) {
                for (int qz = c[1] * 4; qz < c[1] * 4 + 4; qz++) {
                    int id;
                    synchronized (generator) {
                        id = terrain.biomeIdAt(qx * 4 + 2, qz * 4 + 2);
                    }
                    String name = nameOf(body, id);
                    if (name == null) {
                        fail(helper, body.name() + ": the 1.7 detail map has id " + id + ", which no biome of the body has");
                        return;
                    }
                    Holder<Biome> expected = biomes.get(name);
                    for (int qy : QUART_YS) {
                        Holder<Biome> actual = source.getNoiseBiome(qx, qy, qz, null);
                        if (actual != expected) {
                            fail(helper, body.name() + ": biome cell " + qx + "," + qy + "," + qz + " is " + key(actual)
                                    + ", the 1.7 detail map gives " + key(expected) + " (id " + id + ")");
                            return;
                        }
                        cells++;
                    }
                    seen.add(name);
                }
            }
        }

        ServerLevel dimension = level.getServer().getLevel(dev.golem.core.world.Dimensions.level(NtmSpace.MOD_ID, body.name()));
        if (dimension != null) {
            BiomeSource live = dimension.getChunkSource().getGenerator().getBiomeSource();
            if (!(live instanceof LayeredBiomeSource layered) || layered.body() != body) {
                fail(helper, body.name() + ": the dimension's biome source is " + live.getClass().getName());
                return;
            }
        }
        NtmSpace.LOG.info("{} biome source: {} cells match the 1.7 detail map ({} of {} biomes seen){}", body.name(), cells,
                seen.size(), body.biomes().size(), dimension == null ? "" : "; the dimension uses it");
        helper.succeed();
    }

    private static String nameOf(LayeredBodies.Body body, int id) {
        for (Map.Entry<String, Integer> entry : body.biomes().entrySet()) {
            if (entry.getValue() == id) return entry.getKey();
        }
        return null;
    }

    private static String key(Holder<Biome> biome) {
        return biome == null ? "null" : biome.unwrapKey().map(k -> dev.golem.core.Ids.keyId(k).toString()).orElse("?");
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
