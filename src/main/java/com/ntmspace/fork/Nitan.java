package com.ntmspace.fork;

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
    }

    public static final EntityData<State> STATE = EntityData.of(NtmSpace.MOD_ID, "nitan", Player.class, State::new,
            (state, out) -> out.putInt("count", state.count),
            (state, in) -> state.count = in.getIntOr("count", 0));

    public static int count(Player player) {
        return STATE.get(player).count;
    }

    public static void setCount(Player player, int count) {
        STATE.get(player).count = count;
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Lifecycle.onPlayerCopy((oldPlayer, newPlayer, alive) -> setCount(newPlayer, count(oldPlayer)));
        ItemUses.onFinish((entity, stack) -> {
            if (!(entity instanceof Player player) || !clearsNitan(stack)) return;
            if (count(player) > 0) {
                dev.golem.core.world.Effects.removeRegistered(player, ForkContent.NITAN_EFFECT.get());
                setCount(player, 0);
            }
        });
    }

    /** 1.7: {@code this == ModItems.med_ipecac || this == ModItems.med_ptsd}. */
    public static boolean clearsNitan(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(Ids.of("hbm", "med_ipecac")) || id.equals(Ids.of("hbm", "med_ptsd"));
    }
}
