package com.ntmspace.block.fluid;

import com.hbm.blocks.fluid.BlockToxic;
import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.ntmspace.NtmSpace;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.registry.Substitutions;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Space's eight-quanta, water-physics Schrabidic acid and its neighbour reactions after each flow tick. */
public final class SpaceSchrabidicBlock extends BlockToxic {

    public static void substitute() {
        ModWorldFluids.SCHRABIDIC.spec().quanta(8).tickDelay(2).physics(WorldFluids.Physics.WATER);
        Substitutions.block(NtmSpace.MOD_ID, "hbm:schrabidic_block", SpaceSchrabidicBlock::new);
    }

    public SpaceSchrabidicBlock(Properties properties) {
        super(properties, ModWorldFluids.SCHRABIDIC, false);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        var motion = entity.getDeltaMovement();
        if (motion.y < -0.2D) entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
        if (level instanceof ServerLevel server) {
            Compat.hurt(entity, server, ModDamageSources.source(level, ModDamageSources.ACID), 10F);
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, boolean movedByPiston) {
        if (!level.isClientSide()) scheduleFlow(level, pos);
    }

    @Override
    protected void afterFlow(ServerLevel level, BlockPos pos, RandomSource random) {
        for (var direction : Direction.values()) SchrabidicReactions.react(level, pos.relative(direction), random);
    }
}
