package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.MunTerrain17;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The Mun's chunk generator: hbm-space's 1.7 Mun terrain ({@link MunTerrain17}, the 1.7 chunk primer with caves,
 * ravines, craters and brine bubbles) copied block for block into each chunk at the NOISE step. Heights are 1.7's,
 * 0 to 255: the Mun's dimension type has {@code min_y} 0 and {@code height} 256.
 *
 * <p>1.7 generated one chunk at a time on the server thread; the ported generators keep state between chunks (their
 * {@code Random}s, buffers, the cave noise), so generation is serialised on the terrain instance. A small cache of
 * finished primers answers height queries (spawn search, heightmaps for structures) without generating twice.
 */
public final class MunChunkGenerator extends CompatChunkGenerator {

    public static final MapCodec<MunChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, MunChunkGenerator::new));

    /** hbm-space's {@code WorldConfig.munBrineSpawn} default: a brine bubble every 100th chunk. */
    static final int BRINE_SPAWN = 100;

    private MunTerrain17 terrain;
    private final Map<Long, CelestialTerrain17.Primer> primers = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CelestialTerrain17.Primer> eldest) {
            return size() > 32;
        }
    };

    public MunChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    /** The production terrain and its biome descriptor, created on first use. */
    public synchronized MunTerrain17 terrain() {
        if (terrain == null) terrain = new MunTerrain17(seed(), BRINE_SPAWN);
        return terrain;
    }

    /** The 1.7 primer for a chunk, generated once and cached (GameTests compare chunks against it). */
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
                    set(chunk, pos.set(chunkX * 16 + x, y, chunkZ * 16 + z), LegacyBlocks.state(block, primer.metas[column + y]));
                }
            }
        }
    }

    private BlockState[] column(int x, int z) {
        CelestialTerrain17.Primer primer = primer(x >> 4, z >> 4);
        int column = ((x & 15) * 16 + (z & 15)) * 256;
        BlockState[] states = new BlockState[256];
        for (int y = 0; y < 256; y++) {
            Block17 block = primer.blocks[column + y];
            states[y] = block == null ? Blocks.AIR.defaultBlockState() : LegacyBlocks.state(block, primer.metas[column + y]);
        }
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

    /** 1.7's {@code ChunkProviderMoon.seaLevel}. */
    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    protected int minimumY() {
        return 0;
    }

    /**
     * Replays hbm-space's populate of chunk {@code (chunkX, chunkZ)} over the 1.7 primers alone (no live world): every
     * block the decorator and the FML generators change, by {@link com.ntmspace.world.populate.PrimerPopulateWorld#key}.
     * {@code /ntmspace check mun} uses it to tell ores and deposits from terrain.
     */
    public Map<Long, BlockState> populateReplay(int chunkX, int chunkZ) {
        com.ntmspace.world.populate.PrimerPopulateWorld.Base base = (x, y, z) -> {
            if (y < 0 || y > 255) return Blocks.AIR.defaultBlockState();
            CelestialTerrain17.Primer primer = primer(x >> 4, z >> 4);
            int index = ((x & 15) * 16 + (z & 15)) * 256 + y;
            Block17 block = primer.blocks[index];
            return block == null ? Blocks.AIR.defaultBlockState() : LegacyBlocks.state(block, primer.metas[index]);
        };
        com.ntmspace.world.populate.PrimerPopulateWorld world =
                new com.ntmspace.world.populate.PrimerPopulateWorld(base, chunkX, chunkZ, 0L);
        com.ntmspace.world.populate.Populate17.decorator(com.ntmspace.world.populate.Populate17.Body.MUN, world, seed(),
                chunkX, chunkZ);
        com.ntmspace.world.populate.Populate17.generators(com.ntmspace.world.populate.Populate17.Body.MUN, world, seed(),
                chunkX, chunkZ, null, this, true);
        Map<Long, BlockState> placed = new LinkedHashMap<>();
        for (Map.Entry<Long, BlockState> entry : world.written().entrySet()) {
            long key = entry.getKey();
            BlockState was = base.at(com.ntmspace.world.populate.PrimerPopulateWorld.x(key),
                    com.ntmspace.world.populate.PrimerPopulateWorld.y(key), com.ntmspace.world.populate.PrimerPopulateWorld.z(key));
            if (entry.getValue() != was) placed.put(key, entry.getValue());
        }
        return placed;
    }

    /**
     * hbm-space's populate of this chunk ({@code com.ntmspace.world.populate.Populate17}): the biome decorator, then the
     * FML world generators (ores, caves and layers, deposits, keyholes, bedrock ores), after vanilla's own features.
     */
    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                     net.minecraft.world.level.StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.MUN, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, null, this);
    }
}
