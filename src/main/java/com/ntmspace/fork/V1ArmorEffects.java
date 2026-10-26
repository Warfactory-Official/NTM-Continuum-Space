package com.ntmspace.fork;

import com.hbm.handler.ArmorModHandler;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.particle.helper.ParticleCreators;
import com.ntmspace.item.special.ItemModV1;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.Armors;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Deaths;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/** The V1 chestplate module's blood burst, hit healing and kill reward. */
public final class V1ArmorEffects {
    private static boolean installed;
    private V1ArmorEffects() {}

    public static void init() {
        if (installed) return;
        installed = true;
        DamageEvents.install();
        Deaths.install();
        var phase = "ntmspace:v1";
        DamageEvents.HURT.addPhaseOrdering(Hook.DEFAULT_PHASE, phase);
        DamageEvents.HURT.register(phase, event -> {
            if (!event.isCanceled()) onHurt(event.entity(), event.source(), event.amount());
        });
        Deaths.onDeath(V1ArmorEffects::onDeath);
    }

    private static @Nullable Player attacker(DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return null;
        var plate = player.getItemBySlot(EquipmentSlot.CHEST);
        return Armors.isArmor(plate)
                && ArmorModHandler.modItem(plate, ArmorModHandler.plate_only) instanceof ItemModV1 ? player : null;
    }

    public static void onHurt(LivingEntity victim, DamageSource source, float amount) {
        var player = attacker(source);
        if (player == null || !(victim.level() instanceof ServerLevel level)) return;
        var data = new CompoundTag();
        data.putInt("block", BuiltInRegistries.BLOCK.getId(Blocks.REDSTONE_BLOCK));
        data.putInt("count", (int) Math.min(victim.getMaxHealth() / 2F, 250));
        data.putDouble("motion", 0.1D);
        HbmEffectNT.VanillaBurst_BlockDust.send(level, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D,
                victim.getZ(), data, 50D);
        if (player.distanceToSqr(victim) < 25D) player.heal(amount * 0.5F);
    }

    public static void onDeath(LivingEntity victim, DamageSource source) {
        var player = attacker(source);
        if (player == null || !(victim.level() instanceof ServerLevel level)) return;
        ParticleCreators.giblets(level, victim, 0, 0);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                SoundSource.HOSTILE, 2F, 0.95F + level.getRandom().nextFloat() * 0.2F);
        if (player.distanceToSqr(victim) < 100D) player.heal(victim.getMaxHealth() * 0.25F);
    }
}
