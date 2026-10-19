package com.ntmspace.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import dev.golem.core.world.CompatBiomeSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * The biomes of a celestial body with a 1.7 biome map (hbm-space's {@code WorldChunkManagerCelestial.getBiomeGenAt}):
 * the body's {@code GenLayer} stack ({@link LayeredBodies}) seeded with the level seed, read through its detail chain
 * (the Voronoi zoom, one biome per block column, as 1.7's {@code BiomeCache} gave a column). A modern biome cell
 * (4 x 4 blocks) gets the 1.7 biome of the block column at its centre, {@code (4 qx + 2, 4 qz + 2)}, on every height.
 *
 * <p>Vanilla fills each chunk's biomes from this source at its BIOMES step, and asks it for chunks that do not exist
 * yet ({@code /locate biome}, spawn search), so the biome there is the one the generated chunk gets.
 *
 * <p>JSON ({@code "type": "ntmspace:layered"}): {@code "body"} names the body, {@code "biomes"} maps each of the body's
 * 1.7 biome names to a biome. The detail ids are read 16 x 16 columns at a time, the window 1.7's {@code BiomeCache} and
 * {@link com.ntmspace.world.gen.legacy.LayeredBodyTerrain17#chunkBiomeIds} use; 1.7 layers are not thread-safe, so the
 * stack is read under this source's lock.
 */
public final class LayeredBiomeSource extends CompatBiomeSource {

    private record Spec(String body, Map<String, Holder<Biome>> biomes) {}

    private static final MapCodec<Spec> SPEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("body").forGetter(Spec::body),
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("biomes").forGetter(Spec::biomes)
    ).apply(instance, Spec::new));

    public static final MapCodec<LayeredBiomeSource> CODEC = SPEC.flatXmap(LayeredBiomeSource::of,
            source -> DataResult.success(new Spec(source.body.name(), source.biomes)));

    private final LayeredBodies.Body body;
    private final Map<String, Holder<Biome>> biomes;
    private final Holder<Biome>[] byLegacyId;
    private final Holder<Biome> fallback;

    private BiomeLayers17 layers;
    private final Map<Long, int[]> detail = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > 1024;
        }
    };
    private boolean warned;

    @SuppressWarnings("unchecked")
    public LayeredBiomeSource(LayeredBodies.Body body, Map<String, Holder<Biome>> biomes) {
        this.body = body;
        this.biomes = Map.copyOf(biomes);
        this.byLegacyId = new Holder[256];
        for (Map.Entry<String, Integer> entry : body.biomes().entrySet()) {
            byLegacyId[entry.getValue()] = biomes.get(entry.getKey());
        }
        this.fallback = biomes.get(body.biomes().keySet().iterator().next());
    }

    private static DataResult<LayeredBiomeSource> of(Spec spec) {
        LayeredBodies.Body body = LayeredBodies.byName(spec.body());
        if (body == null) return DataResult.error(() -> "no layered celestial body is called " + spec.body());
        if (!spec.biomes().keySet().equals(body.biomes().keySet())) {
            return DataResult.error(() -> spec.body() + "'s biomes must name exactly " + body.biomes().keySet()
                    + ", not " + spec.biomes().keySet());
        }
        return DataResult.success(new LayeredBiomeSource(body, spec.biomes()));
    }

    /** The body this source maps. */
    public LayeredBodies.Body body() {
        return body;
    }

    @Override
    protected MapCodec<? extends BiomeSource> mapCodec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return body.biomes().keySet().stream().map(biomes::get).distinct();
    }

    @Override
    protected synchronized void seeded(long levelSeed) {
        layers = body.layers().apply(levelSeed);
        detail.clear();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int id = legacyIdAt((quartX << 2) + 2, (quartZ << 2) + 2);
        Holder<Biome> biome = id >= 0 && id < byLegacyId.length ? byLegacyId[id] : null;
        if (biome != null) return biome;
        synchronized (this) {
            if (!warned) {
                warned = true;
                NtmSpace.LOG.warn("{}'s 1.7 biome map produced id {}, which has no biome; using {}", body.name(), id,
                        fallback.unwrapKey().map(Object::toString).orElse("?"));
            }
        }
        return fallback;
    }

    /** The 1.7 detail-chain biome id of block column {@code (x, z)}. */
    public synchronized int legacyIdAt(int x, int z) {
        if (layers == null) layers = body.layers().apply(seed()); // throws when the generator never seeded this source
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long key = (long) chunkX << 32 ^ (chunkZ & 0xFFFFFFFFL);
        int[] ids = detail.get(key);
        if (ids == null) {
            ids = layers.detail.getInts(chunkX * 16, chunkZ * 16, 16, 16);
            detail.put(key, ids);
        }
        return ids[(x & 15) + (z & 15) * 16];
    }

    /** The biome of the 1.7 id {@code legacyId} on this body, or null. */
    public Holder<Biome> biome(int legacyId) {
        return legacyId >= 0 && legacyId < byLegacyId.length ? byLegacyId[legacyId] : null;
    }
}
