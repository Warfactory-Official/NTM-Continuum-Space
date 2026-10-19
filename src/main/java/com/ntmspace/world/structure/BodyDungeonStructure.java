package com.ntmspace.world.structure;

import com.hbm.config.DungeonConfig;
import com.hbm.config.WorldConfig;
import com.hbm.world.dungeon.AncientTomb;
import com.hbm.world.dungeon.Spaceship;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.populate.BedrockDeposit;
import com.ntmspace.world.populate.BodyGenerators17;
import com.ntmspace.world.populate.Populate17;
import com.ntmspace.world.populate.PopulateWorld;
import dev.golem.core.data.Tags;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * {@code ntmspace:body_dungeon}: HBM's two hardcoded dungeons that hbm-space's populate built on the celestial bodies,
 * each from its generator's random:
 * <ul>
 *   <li>{@code spaceship}: {@code HbmWorldGen.generateSurface} on every celestial body (after the fallen-meteorite roll:
 *       {@code rand.nextInt(spaceshipStructure) == 0}, column {@code i + nextInt(16), j + nextInt(16)},
 *       {@code new Spaceship().generate(world, rand, x, getHeightValue, z)}), whose spawn-site test accepted the planet
 *       surfaces hbm-space added (grass, dirt, stone, sand, sandstone, moon turf and rock, Duna sands, rock and cobble,
 *       Ike regolith and stone, Dres rock, Laythe silt, Eve silt and rock, Moho regolith and stone);</li>
 *   <li>{@code ancient_tomb}: {@code WorldGeneratorIke}, last ({@code rand.nextInt(pyramidStructure) == 0}, column, then
 *       {@code new AncientTomb().build(world, rand, x, getHeightValue, z)}).</li>
 * </ul>
 *
 * <p><b>Where.</b> Every chunk is a candidate ({@code random_spread} spacing 1); {@link #findGenerationPoint} repeats
 * the generator's draws for the chunk on a {@link Random17} seeded as FML seeded it ({@link Populate17#generatorSeed}):
 * Ike's whole generator through {@link BodyGenerators17} on a world that places nothing (every ore vein draws the same
 * there), the surface pass's meteorite roll directly. The site and the random's state at the builder call are the 1.7
 * ones; the builder then draws from that state. A fallen meteorite in the same chunk drew an amount that depended on
 * the blocks it hit, which cannot be repeated before the chunk exists: such a chunk (one in {@code meteoriteSpawn}, where
 * the meteorite then also landed) gets no spaceship.
 *
 * <p><b>How.</b> As HBM's own legacy dungeons ({@code LegacyDungeonStructure}): the 1.7 builder runs once on a
 * {@link LegacyDungeonWorld} over the generator's terrain, and every chunk of the result copies its share. The terrain
 * is the body's real blocks ({@link #terrain}): the spaceship's site test needs to tell an accepted surface from any other.
 */
public class BodyDungeonStructure extends Structure {

    public enum Kind {
        SPACESHIP("spaceship"),
        ANCIENT_TOMB("ancient_tomb");

        public final String id;

        Kind(String id) {
            this.id = id;
        }

        public static @Nullable Kind byId(String id) {
            for (Kind kind : values()) if (kind.id.equals(id)) return kind;
            return null;
        }
    }

    public static final MapCodec<BodyDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("dungeon").forGetter(s -> s.kind.id),
            Codec.STRING.fieldOf("body").forGetter(s -> s.body.name().toLowerCase(Locale.ROOT))
    ).apply(i, (settings, dungeon, body) -> {
        Kind kind = Kind.byId(dungeon);
        if (kind == null) throw new IllegalArgumentException("unknown body dungeon '" + dungeon + "'");
        Populate17.Body b = populateBody(body);
        if (b == null) throw new IllegalArgumentException("unknown celestial body '" + body + "'");
        return new BodyDungeonStructure(settings, kind, b);
    }));

    private static Populate17.@Nullable Body populateBody(String id) {
        for (Populate17.Body body : Populate17.Body.values()) {
            if (body.name().toLowerCase(Locale.ROOT).equals(id)) return body;
        }
        return null;
    }

    private final Kind kind;
    private final Populate17.Body body;

    public BodyDungeonStructure(StructureSettings settings, Kind kind, Populate17.Body body) {
        super(settings);
        this.kind = kind;
        this.body = body;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public StructureType<?> type() {
        return SpaceStructureTypes.BODY_DUNGEON.get();
    }

    // ------------------------------------------------------------------------------------------------ where

    /** A builder call 1.7 made: its column and the generator random's state just before the builder drew. */
    public record Site(int x, int z, long state) {}

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int chunkX = context.chunkPos().getMinBlockX() >> 4;
        int chunkZ = context.chunkPos().getMinBlockZ() >> 4;
        LegacyDungeonWorld.Terrain terrain = terrain(kind, context.chunkGenerator(), context.randomState(), context.heightAccessor());
        Site site = com.hbm.world.gen.nbt.NBTStructure.GenStructure.checksBiome(context)
                ? site(kind, body, context.seed(), chunkX, chunkZ, terrain)
                // /place structure: in this chunk, on a random from the context
                : new Site(chunkX * 16 + 8, chunkZ * 16 + 8, new Random17(context.random().nextLong()).state());
        if (site == null) return Optional.empty();

        int y = terrain.height(site.x(), site.z());
        LegacyDungeonWorld.Result result = run(kind, body, site, y, terrain);
        if (result == null) return Optional.empty();
        BoundingBox box = result.bounds();
        return Optional.of(new GenerationStub(new BlockPos(site.x(), y, site.z()),
                builder -> builder.addPiece(new Piece(kind, body, site, y, box))));
    }

    /** The 1.7 builder call in chunk {@code (chunkX, chunkZ)} of {@code body}, or null (no roll hit, or not repeatable). */
    public static @Nullable Site site(Kind kind, Populate17.Body body, long seed, int chunkX, int chunkZ,
                                      LegacyDungeonWorld.@Nullable Terrain terrain) {
        long chunkSeed = Populate17.generatorSeed(seed, chunkX, chunkZ);
        Random17 rand = new Random17(chunkSeed);
        int i = chunkX * 16;
        int j = chunkZ * 16;
        return switch (kind) {
            case ANCIENT_TOMB -> {
                if (body != Populate17.Body.IKE) yield null;
                SiteWorld world = new SiteWorld(chunkX, chunkZ, kind.id);
                BodyGenerators17.generate(body, world, rand, i, j, null, world);
                yield world.site;
            }
            case SPACESHIP -> {
                // HbmWorldGen.generateSurface: the fallen meteorite's roll first
                if (WorldConfig.meteoriteSpawn > 0 && rand.nextInt(WorldConfig.meteoriteSpawn) == 0) {
                    int x = i + rand.nextInt(16) + 8;
                    int z = j + rand.nextInt(16) + 8;
                    if (terrain == null) yield null;
                    int y = terrain.height(x, z) - rand.nextInt(10);
                    LegacyBlock b = terrain.block(x, y - 2, z);
                    // the meteorite landed: its own draws depend on the blocks it struck (not repeatable here)
                    if (b != Blocks.air && b != Blocks.water && b != Blocks.lava && y > 1) yield null;
                }
                if (DungeonConfig.spaceshipStructure > 0 && rand.nextInt(DungeonConfig.spaceshipStructure) == 0) {
                    int x = i + rand.nextInt(16);
                    int z = j + rand.nextInt(16);
                    yield new Site(x, z, rand.state());
                }
                yield null;
            }
        };
    }

    /** A populate world that places nothing and records where a dungeon builder was called. */
    static final class SiteWorld extends PopulateWorld {

        private final String kind;
        @Nullable Site site;

        SiteWorld(int chunkX, int chunkZ, String kind) {
            super(chunkX, chunkZ);
            this.kind = kind;
        }

        @Override
        public boolean exists(int x, int y, int z) {
            return false;
        }

        @Override
        protected BlockState read(int x, int y, int z) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        @Override
        protected void write(int x, int y, int z, BlockState state) {}

        @Override
        public void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape) {}

        @Override
        public Random worldRandom() {
            return new Random(0L);
        }

        @Override
        public void dungeonSite(String kind, int x, int z, Random rand) {
            if (this.kind.equals(kind) && rand instanceof Random17 r) site = new Site(x, z, r.state());
        }
    }

    // ------------------------------------------------------------------------------------------------ how

    private static final Map<String, LegacyDungeonWorld.Result> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LegacyDungeonWorld.Result> eldest) {
            return size() > 32;
        }
    };

    /** The run at a site (cached); null when the builder wrote nothing (its spawn-site test failed). */
    public static LegacyDungeonWorld.@Nullable Result run(Kind kind, Populate17.Body body, Site site, int y,
                                                          LegacyDungeonWorld.Terrain terrain) {
        String key = kind.id + "@" + body + ":" + site.x() + "," + y + "," + site.z() + "#" + site.state();
        synchronized (CACHE) {
            LegacyDungeonWorld.Result cached = CACHE.get(key);
            if (cached != null) return cached.size() == 0 ? null : cached;
        }
        RandomSource rand = Random17.source(site.state());
        // 1.7 world.rand and Library.rand were their own unseeded streams; seeded from the site here, as HBM does
        long s = site.state();
        LegacyDungeonWorld world = new LegacyDungeonWorld(terrain, RandomSource.create(s ^ 0x5DEECE66DL), RandomSource.create(s * 31L + 17L));
        switch (kind) {
            case SPACESHIP -> new Spaceship().generate(world, rand, site.x(), y, site.z());
            case ANCIENT_TOMB -> new AncientTomb().build(world, rand, site.x(), y, site.z());
        }
        LegacyDungeonWorld.Result result = world.result();
        synchronized (CACHE) {
            CACHE.put(key, result);
        }
        return result.size() == 0 ? null : result;
    }

    // ------------------------------------------------------------------------------------------------ terrain

    /**
     * hbm-space {@code Spaceship.GetValidSpawnBlocks}, as port ids: vanilla grass, dirt, stone, sand and sandstone (with
     * their 1.7 metadata forms) and the planet surfaces hbm-space added.
     */
    static final Set<String> SPACESHIP_GROUND = Set.of(
            "minecraft:grass_block", "minecraft:dirt", "minecraft:stone", "minecraft:sand", "minecraft:red_sand",
            "minecraft:sandstone", "minecraft:chiseled_sandstone", "minecraft:cut_sandstone",
            "hbm:moon_turf", "ntmspace:moon_rock", "ntmspace:duna_sands", "ntmspace:duna_rock", "ntmspace:duna_cobble",
            "ntmspace:ike_regolith", "ntmspace:ike_stone", "ntmspace:dres_rock", "ntmspace:laythe_silt",
            "ntmspace:eve_silt", "ntmspace:eve_rock", "ntmspace:moho_regolith", "ntmspace:moho_stone");

    /**
     * The body's generated terrain (the generator's own columns: the planets' 1.7 primers, surface included) as the
     * 1.7 builders read it. For the spaceship an accepted surface answers as {@code stone} (in its list), any other solid
     * as {@code gravel} (in no list), a plant or snow cover as {@code snow_layer} (1.7 accepted either over an accepted
     * block); for the tomb, which tests no ground, every solid is {@code stone} as in HBM's own terrain.
     */
    public static LegacyDungeonWorld.Terrain terrain(Kind kind, ChunkGenerator generator, RandomState random, LevelHeightAccessor height) {
        boolean ground = kind == Kind.SPACESHIP;
        return new LegacyDungeonWorld.Terrain() {
            private final Long2ObjectOpenHashMap<NoiseColumn> columns = new Long2ObjectOpenHashMap<>();
            private final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();

            {
                heights.defaultReturnValue(Integer.MIN_VALUE);
            }

            @Override
            public LegacyBlock block(int x, int y, int z) {
                if (y < height.getMinY() || y >= height.getMaxY()) return Blocks.air;
                NoiseColumn column = columns.computeIfAbsent(key(x, z), k -> generator.getBaseColumn(x, z, height, random));
                BlockState state = column.getBlock(y);
                if (state.isAir()) return Blocks.air;
                if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) return Blocks.water;
                if (!state.getFluidState().isEmpty()) return Blocks.lava;
                if (!ground) return Blocks.stone;
                if (!state.blocksMotion()) return Blocks.snow_layer;
                String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                return SPACESHIP_GROUND.contains(id) ? Blocks.stone : Blocks.gravel;
            }

            @Override
            public int height(int x, int z) {
                long k = key(x, z);
                int h = heights.get(k);
                if (h == Integer.MIN_VALUE) {
                    h = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
                    heights.put(k, h);
                }
                return h;
            }

            @Override
            public int minY() {
                return height.getMinY();
            }

            private static long key(int x, int z) {
                return ((long) x << 32) | (z & 0xFFFFFFFFL);
            }
        };
    }

    // ------------------------------------------------------------------------------------------------ piece

    /** One built dungeon: kind, body, 1.7 site and random state; every chunk of its box rebuilds the run and copies its share. */
    public static class Piece extends StructurePiece {

        private final Kind kind;
        private final Populate17.Body body;
        private final Site site;
        private final int y;

        public Piece(Kind kind, Populate17.Body body, Site site, int y, BoundingBox box) {
            super(SpaceStructureTypes.BODY_DUNGEON_PIECE.get(), 0, box);
            this.kind = kind;
            this.body = body;
            this.site = site;
            this.y = y;
        }

        public Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(SpaceStructureTypes.BODY_DUNGEON_PIECE.get(), tag);
            Kind k = Kind.byId(Tags.getStringOr(tag, "dungeon", ""));
            this.kind = k == null ? Kind.SPACESHIP : k;
            Populate17.Body b = populateBody(Tags.getStringOr(tag, "body", ""));
            this.body = b == null ? Populate17.Body.MUN : b;
            this.site = new Site(Tags.getIntOr(tag, "sx", 0), Tags.getIntOr(tag, "sz", 0), Tags.getLongOr(tag, "state", 0L));
            this.y = Tags.getIntOr(tag, "sy", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("dungeon", kind.id);
            tag.putString("body", body.name().toLowerCase(Locale.ROOT));
            tag.putInt("sx", site.x());
            tag.putInt("sy", y);
            tag.putInt("sz", site.z());
            tag.putLong("state", site.state());
        }

        public Kind kind() {
            return kind;
        }

        public BlockPos site() {
            return new BlockPos(site.x(), y, site.z());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                                BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            LegacyDungeonWorld.Terrain terrain = terrain(kind, generator, level.getLevel().getChunkSource().randomState(), level);
            LegacyDungeonWorld.Result result = run(kind, body, site, y, terrain);
            if (result != null) result.apply(level, chunkBox);
        }
    }
}
