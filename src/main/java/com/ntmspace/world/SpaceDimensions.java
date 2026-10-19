package com.ntmspace.world;

import com.ntmspace.NtmSpace;
import dev.golem.core.world.Dimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * NTM: Space's celestial bodies as dimensions. Each body's dimension, dimension type and biome are authored JSON
 * ({@code data/ntmspace/dimension}, {@code dimension_type}, {@code worldgen/biome}); code registers only the chunk
 * generators that reproduce hbm-space's 1.7 terrain, and declares each dimension so a world with it is not "experimental".
 */
public final class SpaceDimensions {

    /** {@code ntmspace:mun}, the Mun's level. */
    public static final ResourceKey<Level> MUN = Dimensions.dimension(NtmSpace.MOD_ID, "mun");

    /** {@code ntmspace:mun}: hbm-space's {@code ChunkProviderMoon}. */
    public static final Dimensions.GeneratorType MUN_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "mun", () -> MunChunkGenerator.CODEC);

    /** {@code ntmspace:eve}, {@code ntmspace:moho}, {@code ntmspace:minmus}: the levels of Eve, Moho and Minmus. */
    public static final ResourceKey<Level> EVE = Dimensions.dimension(NtmSpace.MOD_ID, "eve");
    public static final ResourceKey<Level> MOHO = Dimensions.dimension(NtmSpace.MOD_ID, "moho");
    public static final ResourceKey<Level> MINMUS = Dimensions.dimension(NtmSpace.MOD_ID, "minmus");

    /** hbm-space's {@code ChunkProviderEve}, {@code ChunkProviderMoho} and {@code ChunkProviderMinmus}. */
    public static final Dimensions.GeneratorType EVE_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "eve", () -> EveChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType MOHO_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "moho", () -> MohoChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType MINMUS_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "minmus", () -> MinmusChunkGenerator.CODEC);

    /** {@code ntmspace:duna}, {@code ntmspace:ike}, {@code ntmspace:dres}: the levels of Duna, Ike and Dres. */
    public static final ResourceKey<Level> DUNA = Dimensions.dimension(NtmSpace.MOD_ID, "duna");
    public static final ResourceKey<Level> IKE = Dimensions.dimension(NtmSpace.MOD_ID, "ike");
    public static final ResourceKey<Level> DRES = Dimensions.dimension(NtmSpace.MOD_ID, "dres");

    /** hbm-space's {@code ChunkProviderDuna}, {@code ChunkProviderIke} and {@code ChunkProviderDres}. */
    public static final Dimensions.GeneratorType DUNA_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "duna", () -> DunaChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType IKE_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "ike", () -> IkeChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType DRES_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "dres", () -> DresChunkGenerator.CODEC);

    /** {@code ntmspace:laythe}, {@code ntmspace:tekto}, {@code ntmspace:thatmo}: the levels of Laythe, Tekto and Thatmo. */
    public static final ResourceKey<Level> LAYTHE = Dimensions.dimension(NtmSpace.MOD_ID, "laythe");
    public static final ResourceKey<Level> TEKTO = Dimensions.dimension(NtmSpace.MOD_ID, "tekto");
    public static final ResourceKey<Level> THATMO = Dimensions.dimension(NtmSpace.MOD_ID, "thatmo");

    /** hbm-space's {@code ChunkProviderLaythe}, {@code ChunkProviderTekto} and {@code ChunkProviderThatmo}. */
    public static final Dimensions.GeneratorType LAYTHE_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "laythe", () -> LaytheChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType TEKTO_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "tekto", () -> TektoChunkGenerator.CODEC);
    public static final Dimensions.GeneratorType THATMO_GENERATOR =
            Dimensions.chunkGenerator(NtmSpace.MOD_ID, "thatmo", () -> ThatmoChunkGenerator.CODEC);

    /**
     * {@code ntmspace:layered}: the biome source of a body with a 1.7 biome map ({@link LayeredBiomeSource}; Duna, Dres,
     * Eve, Moho, Minmus, Laythe, Tekto).
     */
    public static final Dimensions.BiomeSourceType LAYERED_BIOMES =
            Dimensions.biomeSource(NtmSpace.MOD_ID, "layered", () -> LayeredBiomeSource.CODEC);

    private SpaceDimensions() {}

    public static void init() {}
}
