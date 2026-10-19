package com.ntmspace.orbit;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.CompatChunkGenerator;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * hbm-space's {@code ChunkProviderOrbit}: every chunk is empty ({@code new Chunk(world, new Block[65536], ...)}), every
 * column is the orbit biome ({@code SpaceConfig.orbitBiome}, here {@code ntmspace:orbit}, a fixed biome source in the
 * dimension JSON) and {@code populate} does nothing. Heights are 1.7's, 0 to 255 ({@code min_y} 0, {@code height} 256).
 */
public final class OrbitChunkGenerator extends CompatChunkGenerator {

    public static final MapCodec<OrbitChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, OrbitChunkGenerator::new));

    public OrbitChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    /** 1.7 {@code provideChunk}: nothing but air. */
    @Override
    protected void fill(ChunkAccess chunk) {}

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[256];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(0, states);
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    /** 1.7's {@code WorldProvider.getAverageGroundLevel} default for a non-flat world. */
    @Override
    public int getSeaLevel() {
        return 63;
    }

    // Public so StationGameTests can read it: vanilla getMinY is source-renamed below 26.1 (Golem CompatChunkGenerator).
    @Override
    public int minimumY() {
        return 0;
    }

    /** 1.7 {@code populate}: "EVEN LESS" ({@code BiomeGenOrbit.decorate} is empty too). */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {}
}
