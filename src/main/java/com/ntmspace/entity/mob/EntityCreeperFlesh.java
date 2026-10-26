package com.ntmspace.entity.mob;

import com.hbm.entity.mob.EntityCreeperBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ParticleCreators;
import com.ntmspace.registry.PopulateBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * hbm-space {@code EntityCreeperFlesh}, the creeper of Laythe's tumor caves: it bursts in a shower of giblets and its
 * explosion turns the blocks it breaks into {@code ntmspace:tumor} (radius 4, or 7 when charged; HBM's bulkie allocator
 * and mutator, as the volatile creeper's slag).
 */
public class EntityCreeperFlesh extends EntityCreeperBase {

    public EntityCreeperFlesh(EntityType<? extends EntityCreeperFlesh> type, Level level) {
        super(type, level);
    }

    /**
     * 1.7 {@code func_146077_cc}: the giblets packet ({@code cDiv} 2; HBM's giblet handler has no count divisor, so the
     * full count shows), then the explosion. The creeper is removed by {@link EntityCreeperBase} before this runs.
     */
    @Override
    protected void detonate(ServerLevel level) {
        ParticleCreators.giblets(level, this, 0, 2);

        ExplosionVNT vnt = new ExplosionVNT(level, getX(), getY(), getZ(), isPowered() ? 7 : 4, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(30, isPowered() ? 16 : 8));
        vnt.setBlockProcessor(new BlockProcessorStandard()
                .withBlockEffect(new BlockMutatorBulkie(PopulateBlocks.TUMOR.get().defaultBlockState())));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.25F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();
    }

    /**
     * hbm-space's {@code ChunkProviderLaythe.getPossibleCreatures}: a monster spawn on Laythe draws from the flesh
     * creeper list (and only from it) when the block below is {@code tumor}; Laythe's biomes list no other monster. The
     * creeper is listed on the Laythe biomes and this keeps it to the tumor. The type's rule is vanilla's monster rule.
     */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return true;
        BlockPos below = blockPosition().below();
        return level.getBlockState(below).is(PopulateBlocks.TUMOR.get()) && super.checkSpawnRules(level, reason);
    }
}
