package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.MinmusTerrain17;
import java.util.Random;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Minmus's chunk generator: hbm-space's {@code ChunkProviderMinmus} (no session randomness) on {@link LayeredChunkGenerator}. */
public final class MinmusChunkGenerator extends LayeredChunkGenerator {

    public static final MapCodec<MinmusChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, MinmusChunkGenerator::new));

    public MinmusChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    @Override
    protected LayeredBodyTerrain17 createTerrain(long seed, Random session) {
        return new MinmusTerrain17(seed);
    }

    /**
     * hbm-space's populate of this chunk ({@code com.ntmspace.world.populate.Populate17}): the biome decorator, then the
     * FML world generators (ores, caves and layers, deposits, keyholes, bedrock ores), after vanilla's own features.
     */
    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                     net.minecraft.world.level.StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.MINMUS, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, terrain(), this);
    }
}
