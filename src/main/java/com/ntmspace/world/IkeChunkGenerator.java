package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.IkeTerrain17;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Ike's chunk generator: hbm-space's {@code ChunkProviderIke} ({@link IkeTerrain17}, one biome as 1.7's
 * {@code WorldChunkManagerHell} gave it) copied block for block into each chunk at the NOISE step, as
 * {@link MunChunkGenerator} does for the Mun. Ike has no session randomness. {@code WorldGeneratorIke}'s ores are all
 * hbm-space per-body ores (not ported yet); its bromine lakes wait for the bromine fluid.
 */
public final class IkeChunkGenerator extends CompatChunkGenerator {

    public static final MapCodec<IkeChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, IkeChunkGenerator::new));

    private IkeTerrain17 terrain;
    private final Map<Long, CelestialTerrain17.Primer> primers = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CelestialTerrain17.Primer> eldest) {
            return size() > 32;
        }
    };

    public IkeChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    /** The production terrain and fixed biome descriptor, created on first use. */
    public synchronized IkeTerrain17 terrain() {
        if (terrain == null) terrain = new IkeTerrain17(seed());
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

    /** {@code ChunkProviderCelestial.seaLevel}, which Ike keeps. */
    @Override
    public int getSeaLevel() {
        return 63;
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
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.IKE, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, null, this);
    }
}
