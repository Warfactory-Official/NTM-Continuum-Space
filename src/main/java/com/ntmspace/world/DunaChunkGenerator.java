package com.ntmspace.world;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockOreBasalt;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.world.gen.legacy.DunaTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.Ores17;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Duna's chunk generator: hbm-space's {@code ChunkProviderDuna} ({@link DunaTerrain17}) on {@link LayeredChunkGenerator},
 * with Duna's five biomes from the 1.7 detail map ({@link LayeredBiomeSource}), and, at decoration, the ore veins of
 * {@code WorldGeneratorDuna} whose ores exist (the five basalt ores; Duna iron and zinc wait for hbm-space's per-body
 * ores, their veins are drawn so the rest land where 1.7 put them).
 */
public final class DunaChunkGenerator extends LayeredChunkGenerator {

    public static final MapCodec<DunaChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, DunaChunkGenerator::new));

    public DunaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> mapCodec() {
        return CODEC;
    }

    @Override
    protected LayeredBodyTerrain17 createTerrain(long seed, Random session) {
        return new DunaTerrain17(seed, session);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        // hbm-space's populate (decorator, WorldGeneratorDuna with its ores, WorldGeneratorCelestial): Populate17
        com.ntmspace.world.populate.Populate17.populate(com.ntmspace.world.populate.Populate17.Body.DUNA, level, seed(),
                chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4, terrain(), this);
    }

    /** {@code ChunkProviderCelestial.seaLevel}, which Duna keeps. */
    @Override
    public int getSeaLevel() {
        return 63;
    }
}
