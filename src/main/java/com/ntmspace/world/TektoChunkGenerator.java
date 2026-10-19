package com.ntmspace.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.TektoTerrain17;
import java.util.Random;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Tekto's chunk generator: hbm-space's {@code ChunkProviderTekto} on {@link LayeredChunkGenerator}. */
public final class TektoChunkGenerator extends LayeredChunkGenerator {

    public static final MapCodec<TektoChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, TektoChunkGenerator::new));

    public TektoChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    @Override
    protected LayeredBodyTerrain17 createTerrain(long seed, Random session) {
        return new TektoTerrain17(seed, session);
    }

    /** 1.7's {@code populate} for this chunk, after vanilla's own (empty) features and structures; see {@link LaytheTektoDecoration}. */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
        // hbm-space's decorator (the global ore layers), WorldGeneratorTekto below, then HBM's surface pass and
        // WorldGeneratorCelestial (Populate17)
        com.ntmspace.world.populate.PopulateWorld populate = new com.ntmspace.world.populate.LevelPopulateWorld(level, chunkX, chunkZ);
        com.ntmspace.world.populate.Populate17.decorator(com.ntmspace.world.populate.Populate17.Body.TEKTO, populate, seed(), chunkX, chunkZ);
        LaytheTektoDecoration.tekto(this, (TektoTerrain17) terrain(), seed(), new World17(level, chunkX, chunkZ), chunkX, chunkZ);
        com.ntmspace.world.populate.Populate17.generators(com.ntmspace.world.populate.Populate17.Body.TEKTO, populate, seed(), chunkX, chunkZ, terrain(), this, false);
    }
}
