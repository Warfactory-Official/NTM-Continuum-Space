package com.ntmspace.fork;

import com.hbm.handler.PlayerShield;
import com.hbm.items.food.ItemLemon;
import dev.golem.core.world.Effects;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.data.EntityData;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.ItemUses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's NITAN counter ({@code HbmPlayerProps.nitanCount}, saved as {@code "nitan"}): +1 per NITAN flask, the
 * nitan effect kills at exactly 3, and eating ipecac or PTSD medication ({@code ItemLemon.onFoodEaten}) clears the
 * effect and resets the count when it is above 0. 1.7 copied every player prop to the respawned player
 * ({@code onPlayerClone}), so the count survives death and the End return here too.
 */
public final class Nitan {

    private Nitan() {}

    /** Per-player state. */
    public static final class State {
        public int count;
        public float health;
    }

    public static final EntityData<State> STATE = EntityData.of(NtmSpace.MOD_ID, "nitan", Player.class, State::new,
            (state, out) -> {
                out.putInt("count", state.count);
                out.putFloat("health", state.health);
            },
            (state, in) -> {
                state.count = in.getIntOr("count", 0);
                state.health = in.getFloatOr("health", 0F);
            });

    public static int count(Player player) {
        return STATE.get(player).count;
    }

    public static void setCount(Player player, int count) {
        STATE.get(player).count = count;
    }

    public static int getNitanCount(Player player) {
        return count(player);
    }

    public static void setNitanCount(Player player, int count) {
        setCount(player, count);
    }

    public static void addNitanDose(Player player) {
        var state = STATE.get(player);
        state.count++;
        state.health += 10F;
    }

    public static float getNitanHealth(Player player) {
        return STATE.get(player).health;
    }

    public static void clearNitanHealth(Player player) {
        STATE.get(player).health = 0F;
    }

    public static float absorbNitan(Player player, float amount) {
        var state = STATE.get(player);
        if (!Effects.hasRegistered(player, ForkContent.NITAN_EFFECT.get())) {
            state.health = 0F;
            return amount;
        }
        float consumed = Math.min(state.health, Math.max(amount, 0F));
        state.health -= consumed;
        return amount - consumed;
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Lifecycle.onPlayerCopy((oldPlayer, newPlayer, alive) -> {
            var original = STATE.get(oldPlayer);
            var copy = STATE.get(newPlayer);
            copy.count = original.count;
            copy.health = original.health;
        });
        PlayerShield.AFTER_ABSORB.register(Nitan::absorbNitan);
        Effects.onRemoved((entity, effect) -> {
            if (entity instanceof Player player && !player.level().isClientSide()
                    && Effects.id(effect).equals(NtmSpace.MOD_ID + ":nitan")) {
                clearNitanHealth(player);
            }
        });
        ItemLemon.EMETIC_USED.register(Nitan::purge);
        ItemUses.onFinish((entity, stack) -> {
            if (entity instanceof Player player && clearsNitan(stack)) purge(player);
        });
    }

    private static void purge(Player player) {
        if (count(player) <= 0) return;
        Effects.removeRegistered(player, ForkContent.NITAN_EFFECT.get());
        setCount(player, 0);
        clearNitanHealth(player);
    }

    /** 1.7: {@code this == ModItems.med_ipecac || this == ModItems.med_ptsd}. */
    public static boolean clearsNitan(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(Ids.of("hbm", "med_ipecac")) || id.equals(Ids.of("hbm", "med_ptsd"));
    }
}
