package com.ntmspace.world.structure;

import com.hbm.config.StructureConfig;
import com.ntmspace.world.LayeredBiomeSource;
import com.ntmspace.world.LayeredBodies;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.jspecify.annotations.Nullable;

/**
 * Where hbm-space's NBT structures generate on each celestial body: its per-dimension spawn lists
 * ({@code NBTStructure.registerStructure(dimension, spawn)} / {@code registerNullWeight(dimension, weight)}, in
 * registration order) and 1.7's region pick over them ({@code GenStructure.getSpawnAtCoords} + {@code findSpawn}).
 *
 * <p><b>The lists.</b> {@code PlanetGen} built {@code WorldGeneratorCelestial} first, whose constructor registered the
 * meteor dungeon (weight 1, {@code rootHeight >= 0}) on every space dimension, then each body's generator, which
 * registered its structures and null weight:
 * <pre>
 * Mun     meteor_dungeon 1, mun_base 6, null 18          Duna    meteor_dungeon 1, duna_comms 6, null 18
 * Minmus  meteor_dungeon 1, minmus_base 6, null 18       Ike     meteor_dungeon 1, ike_artifact 4, null 12
 * Moho    meteor_dungeon 1, moho_base 4, null 20         Dres    meteor_dungeon 1, dres_rbmk 8, null 16
 * Eve     meteor_dungeon 1, null 24                      Tekto   meteor_dungeon 1, null 24
 * Laythe  meteor_dungeon 1, laythe_nuke_sub 6, laythe_vertibird 6, laythe_crashed_vertibird 6
 * Thatmo  meteor_dungeon 1   (WorldGeneratorThatmo was never registered: no null weight, no Thatmo structures)
 * </pre>
 *
 * <p><b>The pick</b>, 1.7's, bit for bit: regions of {@code structureMaxChunks} chunks; a region's random is seeded
 * {@code rx * 341873128712 + rz * 132897987541 + seed + 996996996 - dimension} (the body's 1.7 dimension id, hbm-space's
 * {@code SpaceConfig} default), two {@code nextInt(max - min)} pick the region's chunk, and a third
 * {@code nextInt(totalWeight)} picks among the spawns valid for the biome at the chunk's centre {@code (x*16+8, z*16+8)}
 * (1.7 {@code WorldChunkManager.getBiomeGenAt}, the body's detail biome). The structure set placement
 * ({@code hbm:config_spread}, salt {@code 996996996 - dimension}) is the first part; {@link #pick} repeats it to reach the
 * third draw, and the start is then built from that same random (1.7's {@code MapGenStructure} did), so pieces and
 * rotations follow 1.7 as well. Each structure of a body's set computes the same pick; only the winner generates.
 *
 * <p><b>Biome rules.</b> 1.7's predicates read {@code BiomeGenBase} fields; each body's biomes carry their hbm-space
 * {@code BiomeGenBase.Height} here ({@link Biome}).
 */
public final class BodyStructures {

    private BodyStructures() {}

    /** A body's 1.7 biome as the predicates saw it: name (the body's biome key), {@code rootHeight}, {@code heightVariation}. */
    public record Biome(String name, float rootHeight, float heightVariation) {}

    /** One entry of a body's spawn list; {@code spawn} null for a null weight. */
    public record Member(@Nullable String spawn, IntSupplier weight, @Nullable Predicate<Biome> canSpawn) {
        boolean valid(Biome biome) {
            return canSpawn == null || canSpawn.test(biome);
        }
    }

    /** A region's winner and the region random, after the three draws (the start's random). */
    public record Pick(Member member, WorldgenRandom random) {}

    private static final Predicate<Biome> ROOT_ABOVE_ZERO = b -> b.rootHeight() >= 0;
    private static final Predicate<Biome> FLAT = b -> b.heightVariation() < 0.1F;

    /** The bodies with NBT structures, their 1.7 dimension ids, biomes and spawn lists. */
    public enum Body {
        MUN(413_015, null, List.of(new Biome("mun", 0.125F, 0.05F))),
        DUNA(413_016, LayeredBodies.DUNA, List.of(new Biome("plains", 0.125F, 0.05F), new Biome("lowlands", -0.6F, 0.01F),
                new Biome("polar", 0.425F, 0.05F), new Biome("hills", 0.525F, 0.51F), new Biome("polar_hills", 0.725F, 0.8F))),
        IKE(413_017, null, List.of(new Biome("ike", 0.325F, 0.05F))),
        EVE(413_018, LayeredBodies.EVE, List.of(new Biome("plains", 0.125F, 0.05F), new Biome("mountains", 0.525F, 0.4F),
                new Biome("ocean", -0.6F, 0.01F), new Biome("seismic_plains", 0.270F, 0.3F), new Biome("river", -0.7F, 0.0F))),
        DRES(413_019, LayeredBodies.DRES, List.of(new Biome("plains", 0.625F, 0.04F), new Biome("canyon", -1F, 0.34F))),
        MOHO(413_020, LayeredBodies.MOHO, List.of(new Biome("crag", 0.275F, 0.666F), new Biome("basalt", 0F, 0.224F),
                new Biome("lava_sea", -0.8F, 0.01F), new Biome("plateau", 0.255F, 0.432F))),
        MINMUS(413_021, LayeredBodies.MINMUS, List.of(new Biome("hills", 0.325F, 0.08F), new Biome("basins", -1F, 0.02F))),
        LAYTHE(413_022, LayeredBodies.LAYTHE, List.of(new Biome("islands", 0.256F, 0.05F), new Biome("ocean", -1.8F, 0.24F),
                new Biome("polar", -0.1F, 0.05F), new Biome("coast", -0.4F, 0.01F))),
        TEKTO(413_024, LayeredBodies.TEKTO, List.of(new Biome("polyvinyl_plains", 0.125F, 0.05F),
                new Biome("halogen_hills", 0.525F, 0.4F), new Biome("tetrachloric_river", -0.7F, 0.0F),
                new Biome("forest", 0.425F, 0.3F), new Biome("vinyl_sands", 0.425F, 0.3F))),
        THATMO(413_025, null, List.of(new Biome("thatmo", 0.125F, 0.05F)));

        /** hbm-space's {@code SpaceConfig} dimension id (the region salt is {@code 996996996 - dimension}). */
        public final int dimension;
        /** The body's 1.7 biome stack, or null for a single-biome body. */
        public final LayeredBodies.@Nullable Body layered;
        private final List<Biome> biomes;
        private final List<Member> members = new ArrayList<>();

        Body(int dimension, LayeredBodies.@Nullable Body layered, List<Biome> biomes) {
            this.dimension = dimension;
            this.layered = layered;
            this.biomes = biomes;
        }

        /** The structure set's salt: 1.7's {@code 996996996 - dimensionId}. */
        public int salt() {
            return 996996996 - dimension;
        }

        /** The body's id path ({@code ntmspace:<name>} is its level). */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public List<Member> members() {
            return members;
        }

        public List<Biome> biomes() {
            return biomes;
        }

        /** The biome called {@code name}, or null. */
        public @Nullable Biome biome(String name) {
            for (Biome biome : biomes) if (biome.name().equals(name)) return biome;
            return null;
        }

        /** The biome of a 1.7 biome id of the body's stack, or null. */
        public @Nullable Biome biomeOfLegacyId(int id) {
            if (layered == null) return biomes.get(0);
            for (Map.Entry<String, Integer> e : layered.biomes().entrySet()) {
                if (e.getValue() == id) return biome(e.getKey());
            }
            return null;
        }

        private Body add(@Nullable String spawn, int weight, @Nullable Predicate<Biome> canSpawn) {
            members.add(new Member(spawn, () -> weight, canSpawn));
            return this;
        }

        static {
            // WorldGeneratorCelestial (first): the meteor dungeon on every space dimension, spawnWeight left at 1
            for (Body body : values()) body.add(SpaceStructures.METEOR_DUNGEON, 1, ROOT_ABOVE_ZERO);
            // WorldGeneratorMoon .. WorldGeneratorTekto, PlanetGen's order (each list is the body's own)
            MUN.add(SpaceStructures.MUN_BASE, 6, null).add(null, 18, null);
            DUNA.add(SpaceStructures.DUNA_COMMS, 6, FLAT).add(null, 18, null);
            IKE.add(SpaceStructures.IKE_ARTIFACT, 4, null).add(null, 12, null);
            EVE.add(null, 24, null);
            DRES.add(SpaceStructures.DRES_RBMK, 8, b -> b.name().equals("plains")).add(null, 16, null);
            MOHO.add(SpaceStructures.MOHO_BASE, 4, null).add(null, 20, null);
            MINMUS.add(SpaceStructures.MINMUS_BASE, 6, null).add(null, 18, null);
            LAYTHE.add(SpaceStructures.LAYTHE_NUKE_SUB, 6, b -> b.name().equals("ocean"))
                    .add(SpaceStructures.LAYTHE_VERTIBIRD, 6, ROOT_ABOVE_ZERO)
                    .add(SpaceStructures.LAYTHE_CRASHED_VERTIBIRD, 6, ROOT_ABOVE_ZERO);
            TEKTO.add(null, 24, null);
            // Thatmo: only the meteor dungeon (see the class comment)
        }

        public static @Nullable Body byId(String id) {
            for (Body body : values()) if (body.id().equals(id)) return body;
            return null;
        }
    }

    /** {@code StructureConfig.structureMaxChunks}, as {@code hbm:config_spread} clamps it. */
    public static int spacing() {
        return Math.max(2, StructureConfig.structureMaxChunks);
    }

    /** {@code StructureConfig.structureMinChunks}, as {@code hbm:config_spread} clamps it. */
    public static int separation() {
        return Math.max(0, Math.min(StructureConfig.structureMinChunks, spacing() - 1));
    }

    /**
     * 1.7's pick for chunk {@code (chunkX, chunkZ)} of {@code body}: null unless the chunk is its region's chunk and a
     * spawn (or a null weight) wins for {@code biome}. The returned random has made the three draws.
     */
    public static @Nullable Pick pick(Body body, long seed, int chunkX, int chunkZ, Biome biome) {
        int spacing = spacing();
        int rx = Math.floorDiv(chunkX, spacing);
        int rz = Math.floorDiv(chunkZ, spacing);
        WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(0L));
        rand.setLargeFeatureWithSalt(seed, rx, rz, body.salt());
        int x = rx * spacing + rand.nextInt(spacing - separation());
        int z = rz * spacing + rand.nextInt(spacing - separation());
        if (x != chunkX || z != chunkZ) return null;

        int total = 0;
        for (Member member : body.members) if (member.valid(biome)) total += member.weight().getAsInt();
        if (total <= 0) return null;
        int weight = rand.nextInt(total);
        for (Member member : body.members) {
            if (!member.valid(biome)) continue;
            weight -= member.weight().getAsInt();
            if (weight < 0) return new Pick(member, rand);
        }
        return null;
    }

    /**
     * The 1.7 biome at the centre of chunk {@code (chunkX, chunkZ)} ({@code getBiomeGenAt(x*16+8, z*16+8)}): the body's
     * detail biome through its {@link LayeredBiomeSource}, or the body's single biome.
     */
    public static @Nullable Biome biomeAt(Body body, BiomeSource source, int chunkX, int chunkZ) {
        if (body.layered == null) return body.biomes.get(0);
        if (!(source instanceof LayeredBiomeSource layered)) return null;
        return body.biomeOfLegacyId(layered.legacyIdAt(chunkX * 16 + 8, chunkZ * 16 + 8));
    }
}
