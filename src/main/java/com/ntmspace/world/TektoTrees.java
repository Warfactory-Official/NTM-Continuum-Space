package com.ntmspace.world;

import dev.golem.core.world.RegistryLookups;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Saplings place the same configured tree that /place feature and world-generation data select. */
public final class TektoTrees {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(() -> Registries.FEATURE, "ntmspace");
    public static final RegistryObject<RubberPlantFeature> RUBBER_PLANT = FEATURES.register("rubber_plant", RubberPlantFeature::new);
    public static final RegistryObject<TektoTreeFeature> TTREE = FEATURES.register("ttree", TektoTreeFeature::new);
    public static final ResourceKey<ConfiguredFeature<?, ?>> VINYL_TREE = key("sapling_vinyl");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PVC_TREE = key("sapling_pvc");

    private TektoTrees() {}

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Ids.of("ntmspace", name));
    }

    /** The regular TreeGrower selection draw precedes the configured feature's own draws. */
    public static boolean sapling(ServerLevel level, BlockPos pos, boolean pvc, RandomSource random) {
        random.nextFloat();
        var key = pvc ? PVC_TREE : VINYL_TREE;
        var tree = RegistryLookups.holder(
                level.registryAccess(), Registries.CONFIGURED_FEATURE, key).value();
        return tree.place(level, level.getChunkSource().getGenerator(), random, pos);
    }

    public static void init() {}
}
