package com.ntmspace.recipes.data;

import com.google.gson.JsonObject;
import com.hbm.itempool.ItemPools;
import com.ntmspace.entity.mob.siege.SiegeTier;
import dev.golem.core.world.EntityLootData;
import java.util.ArrayList;

/** Space's 1.7 death drops, declared as tables so reloads and looting use the same data path. */
public final class SpaceEntityLoot extends EntityLootData {

    @Override
    protected void registerDefaults() {
        creeper("entity_mob_flesh_creeper", pool(item("minecraft:gunpowder",
                setCount(uniform(constant(0), constant(2))), addLooting(false))));
        var adult = new JsonObject();
        var flags = new JsonObject();
        flags.addProperty("is_baby", false);
        adult.add("flags", flags);
        register("entity_moon_cow", table(
                pool(item("minecraft:leather", setCount(uniform(constant(0), constant(2))), addLooting(false)),
                        entityProperties("this", adult)),
                pool(item("hbm:cheese", setCount(uniform(constant(1), constant(3))), addLooting(false)),
                        entityProperties("this", adult))));
        register("entity_scutterfish", table(pool(item("ntmspace:scuttertail",
                setCount(uniform(constant(0), constant(2))), addLooting(false)))));
        register("entity_scuttlecrab", table(pool(item("minecraft:clay_ball",
                setCount(uniform(constant(0), constant(2))), addLooting(false)))));
        // EntitySquid.dropFewItems rolls 1..(3 + looting), rather than two independent counts.
        register("entity_depthsquid", table(pool(item("minecraft:ink_sac",
                setCount(uniform(constant(1), looting(1, 3, false)))))));
        var parts = new JsonObject();
        parts.addProperty("type", "minecraft:loot_table");
        parts.addProperty("value", "hbm:chests/" + ItemPools.slug(ItemPools.POOL_MACHINE_PARTS));
        var draws = pool(parts);
        draws.add("rolls", uniform(constant(1), looting(1, 3, false)));
        register("entity_scrapfish", table(draws));
        register("entity_meme_ufo", siege(false));
        register("entity_micro_ufo", siege(true));
        register("entity_bfa", table(pool(item("ntmspace:core_angel"))));
    }

    private static JsonObject siege(boolean playerOnly) {
        var pools = new ArrayList<JsonObject>();
        for (var i = 0; i < SiegeTier.getLength(); i++) {
            var tier = SiegeTier.tiers[i];
            var predicate = new JsonObject();
            // Space persists tiers by name; runtime watcher indices are not the save schema.
            predicate.addProperty("nbt", "{siegeTier:\"" + tier.name + "\"}");
            for (var drop : tier.lootDrops()) {
                var entry = item(drop.itemId(), setCount(constant(drop.count())));
                var condition = entityProperties("this", predicate);
                pools.add(playerOnly ? pool(entry, condition, killedByPlayer()) : pool(entry, condition));
            }
        }
        return table(pools.toArray(JsonObject[]::new));
    }
}
