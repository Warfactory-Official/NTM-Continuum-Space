package com.ntmspace.world;

import com.ntmspace.NtmSpace;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Space's one-in-32 strawberry and mint patches; their biome selection is declared by OverworldFloraData. */
public final class OverworldFlora {
    public static final int FLOWER_ODDS = 32;
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(() -> Registries.FEATURE, NtmSpace.MOD_ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STRAWBERRY = FEATURES.register("strawberry",
            () -> new WildFlowerPatch(LeftoverContent.PLANT_FLOWER_STRAWBERRY));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MINT = FEATURES.register("mint",
            () -> new WildFlowerPatch(LeftoverContent.PLANT_FLOWER_MINT));

    private OverworldFlora() {}
    public static void init() {}
}
