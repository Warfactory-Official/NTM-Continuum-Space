package com.ntmspace.fork;

import com.hbm.lib.ModDamageSources;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's three non-space {@code HbmPotion}s. Colours, "bad effect" flags and tick cadences ({@code isReady}) are
 * hbm-space's; icons are its {@code textures/gui/potions.png} cells.
 */
public final class ForkEffects {

    private ForkEffects() {}

    /** 1.7 {@code ModDamageSource.nitan}: absolute, bypasses armour, allowed in creative ("was consumed by taint"). */
    public static ResourceKey<DamageType> nitanDamage() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Ids.of(NtmSpace.MOD_ID, "nitan"));
    }

    /**
     * {@code HbmPotion.nitan} ("Ambrosia", colour 8388736, not bad), from the NITAN flask. Every tick, a player whose
     * NITAN count ({@link Nitan#count}) is exactly 3 takes 1000 {@code nitan} damage: the third flask in a row kills
     * while the effect lasts (ipecac or PTSD medication resets the count).
     */
    public static final class Nitan extends CompatMobEffect {
        public Nitan() {
            super(MobEffectCategory.BENEFICIAL, 8388736);
        }

        @Override
        protected boolean applyCompatEffectTick(LivingEntity entity, int amplifier) {
            tick(entity);
            return true;
        }

        /** One 1.7 {@code performEffect} of nitan. */
        public static void tick(LivingEntity entity) {
            if (!(entity.level() instanceof ServerLevel level)) return;
            if (entity instanceof Player player && com.ntmspace.fork.Nitan.count(player) == 3) {
                Compat.hurt(entity, level, ModDamageSources.source(level, nitanDamage()), 1000F);
            }
        }

        /** 1.7 {@code isReady}: every tick. */
        @Override
        protected boolean shouldApplyTick(int duration, int amplifier) {
            return true;
        }
    }

    /**
     * {@code HbmPotion.flashbang} (colour 0xD0D0D0, not bad). hbm-space wrote a tick body (set undead on fire, slowness
     * X) but its {@code isReady} never returns true for flashbang, so the body never ran: the only effect is the client
     * overlay ({@code client/ForkHud}). Nothing in hbm-space applies it except {@code /effect}.
     */
    public static final class Flashbang extends CompatMobEffect {
        public Flashbang() {
            super(MobEffectCategory.BENEFICIAL, 0xD0D0D0);
        }

        @Override
        protected boolean applyCompatEffectTick(LivingEntity entity, int amplifier) {
            return true;
        }

        @Override
        protected boolean shouldApplyTick(int duration, int amplifier) {
            return false;
        }
    }

    /**
     * {@code HbmPotion.slippery} ("Butterfingers", colour 0xD0D0D0, not bad): every tick on the server the entity's
     * vertical motion grows by 0.026 (a slowed fall). The ice-like sliding on the ground is {@link Slippery}. Nothing in hbm-space applies it except {@code /effect}.
     */
    public static final class Slippery extends CompatMobEffect {
        public Slippery() {
            super(MobEffectCategory.BENEFICIAL, 0xD0D0D0);
        }

        @Override
        protected boolean applyCompatEffectTick(LivingEntity entity, int amplifier) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, motion.y + 0.026D, motion.z);
            return true;
        }

        @Override
        protected boolean shouldApplyTick(int duration, int amplifier) {
            return true;
        }
    }
}
