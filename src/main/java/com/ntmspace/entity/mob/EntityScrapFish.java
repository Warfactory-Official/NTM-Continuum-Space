package com.ntmspace.entity.mob;

import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code EntityScrapFish} (swim speed 0.8, watches players 4 blocks away), with one of seven skins rolled
 * per client as 1.7 did (see {@link EntitySifterEel}). It drops 1..3+looting machine parts from HBM's
 * {@code POOL_MACHINE_PARTS} item pool.
 */
public class EntityScrapFish extends EntityFish implements IEntityEnumMulti {

    public enum ScrapFish {
        STEEL,
        ALUMINIUM,
        ISOTOPE,
        CADMIUM,
        TECH,
        BLOOD,
        HORROR,
    }

    public final ScrapFish type;

    public EntityScrapFish(EntityType<? extends EntityScrapFish> type, Level level) {
        super(type, level, 0.8, 4.0F);
        this.type = ScrapFish.values()[level.getRandom().nextInt(ScrapFish.values().length)];
    }

    @Override
    public Enum<?> getEnum() {
        return this.type;
    }

    /** 1.7 {@code dropFewItems}: {@code rand.nextInt(3 + looting) + 1} stacks from the machine-parts pool. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = source.getEntity() instanceof LivingEntity attacker ? dev.golem.core.world.Compat.lootingLevel(level, attacker) : 0;
        java.util.List<ItemPool.Entry> pool = ItemPool.getPool(ItemPools.POOL_MACHINE_PARTS);
        int j = this.random.nextInt(3 + looting) + 1;
        for (int k = 0; k < j; ++k) spawnAtLocation(level, ItemPool.getStack(pool, this.random).copy(), 0.0F);
    }
}
