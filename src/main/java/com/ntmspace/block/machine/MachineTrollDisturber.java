package com.ntmspace.block.machine;

import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.handler.radiation.ChunkRadiationManager;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineTrollDisturber}: a creative-only field jammer (the High Energy Field Jammer's texture) that,
 * when placed, takes {@link #ABSORB} radiation out of its chunk ({@code ChunkRadiationManager.proxy.decrementRad}), and
 * every ten ticks, like {@code MachineFieldDisturber}, renews its claim in {@link EntityNukeExplosionMK3#at} for the next
 * 100 ticks. 1.7 also set it random-ticking, and a random tick ran the same update (it renews the claim and schedules a
 * tick; the scheduler drops a duplicate), so random ticks are kept.
 */
public class MachineTrollDisturber extends CompatBlock {

    /** 1.7 {@code new MachineTrollDisturber(10000000F)}. */
    public static final float ABSORB = 10_000_000F;

    public MachineTrollDisturber(Properties properties) {
        super(properties);
    }

    public int tickRate() {
        return 10;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, tickRate());
        // 1.7 called this on both sides; the proxy does nothing on the client
        ChunkRadiationManager.proxy.decrementRad(level, pos, ABSORB);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        update(level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        update(level, pos);
    }

    /** 1.7 {@code updateTick} (its {@code super.onBlockAdded} call is a no-op on a plain block). */
    private void update(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, this, tickRate());
        EntityNukeExplosionMK3.at.put(new EntityNukeExplosionMK3.ATEntry(level.dimension(), pos.getX(), pos.getY(), pos.getZ()),
                level.getGameTime() + 100);
    }
}
