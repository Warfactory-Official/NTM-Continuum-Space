package com.ntmspace.item.special;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * hbm-space {@code HbmPotion.run} ("GOD WATCHES YOU", liquid colour 1118481, a bad effect), given by {@code animan}: in its
 * last 10 ticks it deals 1000 {@code run} damage, sets health to 0, sets off a size-12 antimatter explosion
 * ({@code ExplosionVNT.makeAmat}), plays the mini-nuke explosion at volume 100 and removes any non-player outright.
 */
public class EffectRun extends CompatMobEffect {

    /** 1.7 {@code ModDamageSource.run}: absolute, bypasses armour ("%1$s atoned for their sins."). */
    public static ResourceKey<DamageType> runDamage() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Ids.of(NtmSpace.MOD_ID, "run"));
    }

    public EffectRun() {
        super(MobEffectCategory.HARMFUL, 1118481);
    }

    @Override
    protected boolean applyCompatEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;

        Compat.hurt(entity, level, ModDamageSources.source(level, runDamage()), 1000F);
        entity.setHealth(0.0F);

        new ExplosionVNT(level, entity.getX(), entity.getY(), entity.getZ(), 12).makeAmat().explode();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.MUKE_EXPLOSION.get(), SoundSource.AMBIENT,
                100.0F, 1.0F);

        if (!(entity instanceof Player)) entity.discard();
        return true;
    }

    /** 1.7 {@code isReady}: {@code duration <= 10}. */
    @Override
    protected boolean shouldApplyTick(int duration, int amplifier) {
        return duration <= 10;
    }
}
