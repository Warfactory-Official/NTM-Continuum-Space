package com.ntmspace.atmosphere;

import com.ntmspace.breathing.SpaceDamageTypes;
import com.ntmspace.registry.AtmosphereContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A body killed by Eve's atmosphere leaves flesh wherever the surrounding film can rest. */
public final class EveDeath {
    private EveDeath() {}

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !source.is(SpaceDamageTypes.EVE)) return;
        var flesh = AtmosphereContent.FLESH_BLOCK.get().defaultBlockState();
        var center = entity.blockPosition();
        for (var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (level.getBlockState(pos).isAir() && flesh.canSurvive(level, pos)) level.setBlockAndUpdate(pos, flesh);
        }
    }
}
