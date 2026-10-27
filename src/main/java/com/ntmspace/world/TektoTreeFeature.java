package com.ntmspace.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** The authored Tekto ellipsoid tree, configured as data and shared with the unchanged legacy decorator. */
public final class TektoTreeFeature extends Feature<TektoTreeFeature.Shape> {
    public record Shape(int offset, int smallest, int tallest, int xz, int y, boolean vines, BlockState log,
                        BlockState leaves) implements FeatureConfiguration {
        public static final Codec<Shape> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("offset").forGetter(Shape::offset),
                ExtraCodecs.POSITIVE_INT.fieldOf("smallest").forGetter(Shape::smallest),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("tallest").forGetter(Shape::tallest),
                ExtraCodecs.POSITIVE_INT.fieldOf("xz").forGetter(Shape::xz),
                ExtraCodecs.POSITIVE_INT.fieldOf("y").forGetter(Shape::y),
                Codec.BOOL.fieldOf("vines").forGetter(Shape::vines),
                BlockState.CODEC.fieldOf("log").forGetter(Shape::log),
                BlockState.CODEC.fieldOf("leaves").forGetter(Shape::leaves)).apply(i, Shape::new));
    }

    public TektoTreeFeature() {
        super(Shape.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Shape> context) {
        var shape = context.config();
        var origin = context.origin();
        var world = World17.live(context.level(), origin.getX() >> 4, origin.getZ() >> 4);
        return LaytheTektoDecoration.tree(world, context.random()::nextInt, origin.getX(), origin.getY(), origin.getZ(),
                shape.offset(), shape.smallest(), shape.tallest(), shape.xz(), shape.y(), shape.vines(),
                shape.log(), shape.leaves());
    }
}
