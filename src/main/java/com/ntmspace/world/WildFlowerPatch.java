package com.ntmspace.world;

import com.hbm.saveddata.TomSaveData;
import com.hbm.world.feature.LegacyFlowerFeature;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import dev.golem.core.world.LightLevels;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** 1.7 Space HbmWorldGen's flower roll and column selection, using HBM's shared 64-attempt flower placer. */
public final class WildFlowerPatch extends Feature<NoneFeatureConfiguration> {
    private static final LegacyFlowerFeature PATCH = new LegacyFlowerFeature(LegacyFlowerFeature.Config.CODEC);
    private final Supplier<? extends Block> flower;

    public WildFlowerPatch(Supplier<? extends Block> flower) {
        super(NoneFeatureConfiguration.CODEC);
        this.flower = flower;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        if (TomSaveData.impact(level.getLevel()) || random.nextInt(OverworldFlora.FLOWER_ODDS) != 0) return false;
        var x = (context.origin().getX() & ~15) + random.nextInt(16);
        var z = (context.origin().getZ() & ~15) + random.nextInt(16);
        var y = LightLevels.lightBlockingHeight(level, x, z);
        return PATCH.place(new FeaturePlaceContext<>(Optional.empty(), level, context.chunkGenerator(), random,
                new BlockPos(x, y, z),
                new LegacyFlowerFeature.Config(flower.get().defaultBlockState(), 64, 8, 4)));
    }
}
