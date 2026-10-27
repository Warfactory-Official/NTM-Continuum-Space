package com.ntmspace.block.generic;

import com.ntmspace.trait.CBT_Temperature;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Space's melted-flesh film: heat chars it in stages; ice and transparent floors cannot hold it. */
public final class MeltedFlesh extends CompatBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    private final @Nullable Supplier<? extends Block> charred;
    private final ParticleOptions[] fumes;

    public MeltedFlesh(Properties properties, @Nullable Supplier<? extends Block> charred, ParticleOptions... fumes) {
        super(properties);
        this.charred = charred;
        this.fumes = fumes;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        var floor = pos.below();
        var below = level.getBlockState(floor);
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) return false;
        if (below.is(BlockTags.LEAVES)) return true;
        return Compat.isSolidRender(below, level, floor) && below.blocksMotion();
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighborPos, BlockState neighborState) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    private static boolean hot(Level level) {
        var temperature = CelestialTraits.getTrait(level, CBT_Temperature.class);
        return temperature != null && temperature.degrees > 100;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (charred != null && hot(level)) level.setBlockAndUpdate(pos, charred.get().defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (fumes.length == 0 || !hot(level)) return;
        double x = pos.getX() + random.nextFloat();
        double y = pos.getY() + random.nextFloat() * 0.5F + 0.5F;
        double z = pos.getZ() + random.nextFloat();
        for (var fume : fumes) level.addParticle(fume, x, y, z, 0, 0, 0);
    }
}
