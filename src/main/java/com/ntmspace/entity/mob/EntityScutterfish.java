package com.ntmspace.entity.mob;

import com.ntmspace.registry.SpaceMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** hbm-space {@code EntityScutterfish}: Laythe's common fish (swim speed 1.5, watches players 6 blocks away). */
public class EntityScutterfish extends EntityFish {

    public EntityScutterfish(EntityType<? extends EntityScutterfish> type, Level level) {
        super(type, level, 1.5, 6.0F);
    }

    /**
     * 1.7 {@code getDropItem() = scuttertail} through {@code EntityLiving.dropFewItems}: 0..2 tails plus 0..looting
     * ({@code rand.nextInt(3)}, then {@code rand.nextInt(1 + looting)} when looting is above 0).
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = source.getEntity() instanceof LivingEntity attacker ? dev.golem.core.world.Compat.lootingLevel(level, attacker) : 0;
        int j = this.random.nextInt(3);
        if (looting > 0) j += this.random.nextInt(looting + 1);
        for (int k = 0; k < j; ++k) spawnAtLocation(level, new ItemStack(SpaceMobs.SCUTTERTAIL.get()));
    }
}
