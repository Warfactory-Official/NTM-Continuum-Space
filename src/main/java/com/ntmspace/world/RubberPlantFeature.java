package com.ntmspace.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.block.terrain.BlockRubberPlant;
import com.ntmspace.block.terrain.BlockRubberTall;
import com.ntmspace.block.terrain.PlantType;
import com.ntmspace.registry.LaytheTektoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Tekto's explicit rubber decoration, including the forest's extra bush and cactus rolls. */
public final class RubberPlantFeature extends Feature<RubberPlantFeature.Config> {
    public record Config(boolean forest) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("forest").forGetter(Config::forest)).apply(i, Config::new));
    }

    public RubberPlantFeature() { super(Config.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        var level = context.level();
        var random = context.random();
        var origin = context.origin();
        var pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int i = 0; i < 64; i++) {
            pos.set(origin.getX() + random.nextInt(8) - random.nextInt(8),
                    origin.getY() + random.nextInt(4) - random.nextInt(4),
                    origin.getZ() + random.nextInt(8) - random.nextInt(8));
            var below = level.getBlockState(pos.below());
            if (below.is(LaytheTektoBlocks.RUBBER_GRASS.get())) {
                level.setBlock(pos, plant(PlantType.RubberPlant.values()[random.nextInt(3)]), Block.UPDATE_CLIENTS);
                placed = true;
            } else if (below.is(LaytheTektoBlocks.VINYL_SAND.get())) {
                if (random.nextInt(64) == 0) {
                    // Ground is the first free cell above the surface; do not use a global height or consume another draw.
                    int y = World17.live(level, pos.getX() >> 4, pos.getZ() >> 4)
                            .topSolidOrLiquid(pos.getX(), pos.getZ());
                    level.setBlock(new BlockPos(pos.getX(), y, pos.getZ()), tall(PlantType.RubberTall.CACT), Block.UPDATE_CLIENTS);
                }
            }
            if (context.config().forest() && below.is(LaytheTektoBlocks.RUBBER_GRASS.get())) {
                var bush = random.nextInt(2) == 0 ? PlantType.RubberTall.BUSH : PlantType.RubberTall.FLOWER;
                var other = PlantType.RubberPlant.values()[random.nextInt(3)];
                level.setBlock(pos, random.nextInt(4) == 0 ? tall(bush) : plant(other), Block.UPDATE_CLIENTS);
                if (random.nextInt(10) == 0) level.setBlock(pos, tall(PlantType.RubberTall.CACT), Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }

    private static BlockState plant(PlantType.RubberPlant type) {
        return LaytheTektoBlocks.RUBBER_PLANT.get().defaultBlockState().setValue(BlockRubberPlant.TYPE, type);
    }

    private static BlockState tall(PlantType.RubberTall type) {
        return LaytheTektoBlocks.RUBBER_TALL.get().defaultBlockState().setValue(BlockRubberTall.TYPE, type);
    }
}
