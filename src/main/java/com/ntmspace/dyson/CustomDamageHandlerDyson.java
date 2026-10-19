package com.ntmspace.dyson;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * hbm-space's {@code CustomDamageHandlerDyson}: whatever a Dyson beam's explosion reaches also takes the beam's energy
 * per tick / 1000 as explosion damage.
 */
public class CustomDamageHandlerDyson implements ICustomDamageHandler {

    protected long energy;

    public CustomDamageHandlerDyson(long energy) {
        this.energy = energy;
    }

    @Override
    public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
        if (entity instanceof LivingEntity && entity.level() instanceof ServerLevel server) {
            dev.golem.core.world.Compat.hurt(entity, server, EntityProcessorStandard.setExplosionSource(explosion), (float) energy / 1_000);
        }
    }
}
