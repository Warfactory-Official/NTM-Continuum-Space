package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.MohoTerrain17;
import java.util.Random;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Moho's chunk generator: hbm-space's {@code ChunkProviderMoho} on {@link LayeredChunkGenerator}. */
public final class MohoChunkGenerator extends LayeredChunkGenerator {

    public static final MapCodec<MohoChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, MohoChunkGenerator::new));

    public MohoChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    @Override
    protected LayeredBodyTerrain17 createTerrain(long seed, Random session) {
        return new MohoTerrain17(seed, session);
    }

    /**
     * hbm-space's populate of this chunk ({@code com.ntmspace.world.populate.Populate17}): the biome decorator, then the
     * FML world generators (ores, caves and layers, deposits, keyholes, bedrock ores), after vanilla's own features.
     */
    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                     net.minecraft.world.level.StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.MOHO, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, terrain(), this);
    }
}
