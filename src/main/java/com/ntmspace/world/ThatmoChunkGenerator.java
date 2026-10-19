package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.ThatmoTerrain17;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Thatmo's chunk generator: hbm-space's {@code ChunkProviderThatmo} ({@link ThatmoTerrain17}, one biome everywhere)
 * copied block for block into each chunk at the NOISE step, as {@link MunChunkGenerator} does for the Mun. Heights are
 * 1.7's, 0 to 255 ({@code min_y} 0, {@code height} 256). The terrain gets a session {@code Random} in place of 1.7's
 * {@code Math.random()} (see {@link LayeredChunkGenerator}).
 *
 * <p>No 1.7 decoration is ported because 1.7 placed none: Thatmo's vanilla biome decorator had every organic type
 * denied (hbm-space's {@code preventOrganicSpawn}, bodies without life) and its ores find no stone in basalt
 * ({@link LaytheTektoDecoration}); its NBT structures (Thatmo's ruins and city) belong to the structures port.
 */
public final class ThatmoChunkGenerator extends CompatChunkGenerator {

    public static final MapCodec<ThatmoChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, ThatmoChunkGenerator::new));

    private ThatmoTerrain17 terrain;
    private Long sessionSeed;
    private final Map<Long, CelestialTerrain17.Primer> primers = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CelestialTerrain17.Primer> eldest) {
            return size() > 32;
        }
    };

    public ThatmoChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    /** Seeds the session {@code Random} (tests); call before the first chunk. Returns this generator. */
    public synchronized ThatmoChunkGenerator useSessionSeed(long seed) {
        if (terrain != null) throw new IllegalStateException("the terrain already exists");
        this.sessionSeed = seed;
        return this;
    }

    /** The body's terrain (created on first use). */
    public synchronized ThatmoTerrain17 terrain() {
        if (terrain == null) terrain = new ThatmoTerrain17(seed(), sessionSeed == null ? new Random() : new Random(sessionSeed));
        return terrain;
    }

    /** The 1.7 primer for a chunk, generated once and cached (GameTests and the check command compare against it). */
    public synchronized CelestialTerrain17.Primer primer(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ (chunkZ & 0xFFFFFFFFL);
        CelestialTerrain17.Primer primer = primers.get(key);
        if (primer == null) {
            primer = terrain().provide(chunkX, chunkZ);
            primers.put(key, primer);
        }
        return primer;
    }

    @Override
    protected void fill(ChunkAccess chunk) {
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
        CelestialTerrain17.Primer primer = primer(chunkX, chunkZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int column = (x * 16 + z) * 256;
                for (int y = 0; y < 256; y++) {
                    Block17 block = primer.blocks[column + y];
                    if (block == null) continue;
                    set(chunk, pos.set(chunkX * 16 + x, y, chunkZ * 16 + z), LayeredChunkGenerator.state(block, primer.metas[column + y]));
                }
            }
        }
    }

    private BlockState[] column(int x, int z) {
        CelestialTerrain17.Primer primer = primer(x >> 4, z >> 4);
        int column = ((x & 15) * 16 + (z & 15)) * 256;
        BlockState[] states = new BlockState[256];
        for (int y = 0; y < 256; y++) states[y] = LayeredChunkGenerator.state(primer.blocks[column + y], primer.metas[column + y]);
        return states;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = column(x, z);
        for (int y = 255; y >= 0; y--) {
            if (type.isOpaque().test(states[y])) return y + 1;
        }
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(0, column(x, z));
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    /** 1.7's {@code ChunkProviderThatmo.seaLevel}. */
    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    protected int minimumY() {
        return 0;
    }

    /**
     * hbm-space's populate of this chunk ({@code com.ntmspace.world.populate.Populate17}): the biome decorator, then the
     * FML world generators (ores, caves and layers, deposits, keyholes, bedrock ores), after vanilla's own features.
     */
    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                     net.minecraft.world.level.StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.THATMO, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, null, this);
    }
}
