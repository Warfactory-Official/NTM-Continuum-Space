package com.ntmspace.gametest;

import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.ntmspace.NtmSpace;
import com.ntmspace.entity.mob.siege.EntitySiegeUFO;
import com.ntmspace.entity.mob.siege.SiegeTier;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Biomes;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Spawns;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's mobs (History §19fb-mobs) against hbm-space's 1.7 code:
 *
 * <ul>
 *   <li>{@code mobs_17_attributes}: each of the eleven types spawns, with hbm-space's {@code applyEntityAttributes}
 *       values (1.7 defaults otherwise: 20 health, speed 0.7, follow 16) and {@code setSize} hitbox;</li>
 *   <li>{@code mobs_17_drops}: each mob killed drops what its 1.7 {@code dropFewItems} could (counts in range, nothing
 *       else; twenty Mun cows for the cheese/leather bounds), the siege saucer its tier's list exactly;</li>
 *   <li>{@code mobs_angel_core}: the angel's 50-tick death ends in the Angelic Core;</li>
 *   <li>{@code mobs_natural_spawns}: the planet biomes list hbm-space's {@code BiomeGenBaseCelestial} spawn entries
 *       (NTM: Space's entries; HBM's own global creeper entries may sit beside them), and the Mun's creature
 *       spawning chance is 1.7's 0.008;</li>
 *   <li>{@code mobs_suffocation_immune}: the {@code ntmspace:suffocation_immune} tag holds hbm-space's
 *       {@code ISuffocationImmune} mobs, and not the fish;</li>
 *   <li>{@code mobs_siege_tier_saved_by_name}: the saucer's tier survives a save as its name.</li>
 * </ul>
 */
public final class SpaceMobGameTests {

    private SpaceMobGameTests() {}

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function ATTRIBUTES = TESTS.function("mobs_17_attributes", 20, () -> SpaceMobGameTests::attributes);
    public static final GameTests.Function DROPS = TESTS.function("mobs_17_drops", 20, () -> SpaceMobGameTests::drops);
    public static final GameTests.Function ANGEL_CORE = TESTS.function("mobs_angel_core", 200, () -> SpaceMobGameTests::angelCore);
    public static final GameTests.Function SPAWNS = TESTS.function("mobs_natural_spawns", 20, () -> SpaceMobGameTests::naturalSpawns);
    public static final GameTests.Function TAG = TESTS.function("mobs_suffocation_immune", 20, () -> SpaceMobGameTests::suffocationTag);
    public static final GameTests.Function SIEGE_SAVE = TESTS.function("mobs_siege_tier_saved_by_name", 20, () -> SpaceMobGameTests::siegeSave);

    public static void init() {}

    // ------------------------------------------------------------------------------------------- attributes

    private static void attributes(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // EntityCow: 10 / 0.2, 0.9 x 1.3
        stats(helper, errors, SpaceMobs.MOON_COW.get(), 10, 0.2, 16, 0.9F, 1.3F);
        // EntityFish: 15 health, follow 10, the 2 x 2 box, EntityLivingBase speed 0.7
        stats(helper, errors, SpaceMobs.SCUTTERFISH.get(), 15, 0.7, 10, 2F, 2F);
        stats(helper, errors, SpaceMobs.SCRAPFISH.get(), 15, 0.7, 10, 2F, 2F);
        stats(helper, errors, SpaceMobs.SIFTER_EEL.get(), 15, 0.7, 10, 2F, 2F);
        // EntityWaterMob defaults, the default 0.6 x 1.8
        stats(helper, errors, SpaceMobs.SCUTTLECRAB.get(), 20, 0.7, 16, 0.6F, 1.8F);
        // EntitySquid: 10 health, 0.95 x 0.95
        stats(helper, errors, SpaceMobs.DEPTH_SQUID.get(), 10, 0.7, 16, 0.95F, 0.95F);
        // EntityCreeper: 20 / 0.25
        stats(helper, errors, SpaceMobs.CREEPER_FLESH.get(), 20, 0.25, 16, 0.6F, 1.8F);
        // EntityTankbot: 180, damage 15, follow 60, knockback 1, 2.45 x 3.25
        LivingEntity tankbot = stats(helper, errors, SpaceMobs.TANKBOT.get(), 180, 0.7, 60, 2.45F, 3.25F);
        check(errors, tankbot, "attack damage", tankbot.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 15);
        check(errors, tankbot, "knockback resistance", tankbot.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 1);
        if (!tankbot.fireImmune()) errors.add("tankbot is not fireproof (1.7 isImmuneToFire)");
        // EntityWarBehemoth: 200, damage 15, follow 50, knockback 100 (vanilla's bound 1: never knocked), 1.75 x 6.35
        LivingEntity behemoth = stats(helper, errors, SpaceMobs.WAR_BEHEMOTH.get(), 200, 0.7, 50, 1.75F, 6.35F);
        check(errors, behemoth, "attack damage", behemoth.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 15);
        check(errors, behemoth, "knockback resistance", behemoth.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1);
        // EntityBFAngel: 10000, 5 x 4, fireproof
        LivingEntity angel = stats(helper, errors, SpaceMobs.BF_ANGEL.get(), 10000, 0.7, 16, 5F, 4F);
        if (!angel.fireImmune()) errors.add("angel is not fireproof (1.7 isImmuneToFire)");
        // EntitySiegeUFO: EntityFlying defaults before a tier is set, 1.5 x 1
        EntitySiegeUFO ufo = (EntitySiegeUFO) stats(helper, errors, SpaceMobs.SIEGE_UFO.get(), 20, 0.7, 16, 1.5F, 1F);
        // setTier: health a quarter of the tier's, speed the tier's
        ufo.setTier(SiegeTier.SCHRAB);
        check(errors, ufo, "schrab max health", ufo.getAttributeBaseValue(Attributes.MAX_HEALTH), 62.5);
        check(errors, ufo, "schrab speed", ufo.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 1);
        check(errors, ufo, "schrab health", ufo.getHealth(), 62.5);
        finish(helper, errors);
    }

    private static LivingEntity stats(GameTestHelper helper, List<String> errors, EntityType<? extends LivingEntity> type,
                                      double health, double speed, double follow, float width, float height) {
        // created without spawn initialisation (the saucer's egg spawn would roll a tier); the drop test spawns them
        LivingEntity entity = Entities.createTriggered(type, helper.getLevel());
        if (entity == null) {
            errors.add("could not create " + EntityType.getKey(type));
            return null;
        }
        check(errors, entity, "max health", entity.getAttributeBaseValue(Attributes.MAX_HEALTH), health);
        check(errors, entity, "movement speed", entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), speed);
        check(errors, entity, "follow range", entity.getAttributeBaseValue(Attributes.FOLLOW_RANGE), follow);
        check(errors, entity, "width", entity.getBbWidth(), width);
        check(errors, entity, "height", entity.getBbHeight(), height);
        return entity;
    }

    private static void check(List<String> errors, Entity entity, String what, double actual, double expected) {
        if (Math.abs(actual - expected) >= 1.0E-4D) {
            errors.add(EntityType.getKey(entity.getType()) + " " + what + " = " + actual + ", 1.7 " + expected);
        }
    }

    // ------------------------------------------------------------------------------------------- drops

    private static void drops(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();

        Map<Item, Integer> cows = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            Map<Item, Integer> one = killAndCollect(helper, SpaceMobs.MOON_COW.get());
            int cheese = one.getOrDefault(ModFoodItems.cheese.get(), 0);
            int leather = one.getOrDefault(Items.LEATHER, 0);
            if (cheese < 1 || cheese > 3) errors.add("a Mun cow dropped " + cheese + " cheese, 1.7 1..3");
            if (leather > 2) errors.add("a Mun cow dropped " + leather + " leather, 1.7 0..2");
            one.forEach((item, n) -> cows.merge(item, n, Integer::sum));
        }
        only(errors, "Mun cows", cows, ModFoodItems.cheese.get(), Items.LEATHER);

        range(errors, "scutterfish", killAndCollect(helper, SpaceMobs.SCUTTERFISH.get()), SpaceMobs.SCUTTERTAIL.get(), 0, 2);
        range(errors, "scuttlecrab", killAndCollect(helper, SpaceMobs.SCUTTLECRAB.get()), Items.CLAY_BALL, 0, 2);
        range(errors, "depth squid", killAndCollect(helper, SpaceMobs.DEPTH_SQUID.get()), Items.INK_SAC, 1, 3);
        range(errors, "flesh creeper", killAndCollect(helper, SpaceMobs.CREEPER_FLESH.get()), Items.GUNPOWDER, 0, 2);
        Map<Item, Integer> eel = killAndCollect(helper, SpaceMobs.SIFTER_EEL.get());
        if (!eel.isEmpty()) errors.add("sifter eel dropped " + eel + ", 1.7 nothing");
        Map<Item, Integer> tankbot = killAndCollect(helper, SpaceMobs.TANKBOT.get());
        if (!tankbot.isEmpty()) errors.add("tankbot dropped " + tankbot + ", 1.7 nothing");

        // scrapfish: rand.nextInt(3) + 1 stacks from the machine-parts pool
        BlockPos pos = new BlockPos(2, 2, 2);
        LivingEntity fish = helper.spawn(SpaceMobs.SCRAPFISH.get(), pos);
        kill(helper, fish);
        List<ItemEntity> stacks = itemsAround(helper, pos);
        if (stacks.size() < 1 || stacks.size() > 3) errors.add("scrapfish dropped " + stacks.size() + " stacks, 1.7 1..3");
        stacks.forEach(Entity::discard);

        // the siege saucer drops its tier's whole list: desh = 16 steel and 3 capacitors
        EntitySiegeUFO ufo = helper.spawn(SpaceMobs.SIEGE_UFO.get(), pos);
        ufo.setTier(SiegeTier.DESH);
        kill(helper, ufo);
        List<ItemEntity> deshItems = itemsAround(helper, pos);
        Map<Item, Integer> desh = collect(deshItems);
        // no leftovers: the plots are 5..6 blocks apart and the next test (mobs_angel_core) looks 6 blocks around
        deshItems.forEach(Entity::discard);
        ufo.discard();
        Map<Item, Integer> expected = new HashMap<>();
        expected.put(ModItems.ingot_steel.get(), 16);
        expected.put(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 3);
        if (!desh.equals(expected)) errors.add("desh saucer dropped " + desh + ", 1.7 " + expected);

        finish(helper, errors);
    }

    private static void range(List<String> errors, String what, Map<Item, Integer> drops, Item item, int min, int max) {
        int n = drops.getOrDefault(item, 0);
        if (n < min || n > max) errors.add(what + " dropped " + n + " " + BuiltInRegistries.ITEM.getKey(item) + ", 1.7 " + min + ".." + max);
        only(errors, what, drops, item);
    }

    private static void only(List<String> errors, String what, Map<Item, Integer> drops, Item... allowed) {
        for (Item item : drops.keySet()) {
            boolean ok = false;
            for (Item a : allowed) ok |= a == item;
            if (!ok) errors.add(what + " dropped " + BuiltInRegistries.ITEM.getKey(item) + ", not a 1.7 drop");
        }
    }

    private static Map<Item, Integer> killAndCollect(GameTestHelper helper, EntityType<? extends LivingEntity> type) {
        BlockPos pos = new BlockPos(2, 2, 2);
        LivingEntity entity = helper.spawn(type, pos);
        kill(helper, entity);
        List<ItemEntity> items = itemsAround(helper, pos);
        Map<Item, Integer> out = collect(items);
        items.forEach(Entity::discard);
        entity.discard();
        return out;
    }

    private static void kill(GameTestHelper helper, LivingEntity entity) {
        ServerLevel level = helper.getLevel();
        Compat.hurt(entity, level, level.damageSources().generic(), 1.0E7F);
    }

    private static List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos relative) {
        BlockPos abs = helper.absolutePos(relative);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(6));
    }

    private static Map<Item, Integer> collect(List<ItemEntity> items) {
        Map<Item, Integer> out = new HashMap<>();
        for (ItemEntity e : items) {
            ItemStack stack = e.getItem();
            out.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------- angel

    private static void angelCore(GameTestHelper helper) {
        // The death takes 50 ticks, so the angel must be ticked: 26.1 force-loads (entity-ticks) only the chunk of the
        // test's 1x1x1 structure, and (2, 2, 2) lies in the next chunk whenever the plot is within two blocks of a chunk
        // edge; at the edge of a batch's grid nothing else forces that chunk, the angel never dies and no core drops (the
        // flaky failure found the neighbouring mobs_17_drops plot's saucer drops instead). Column (0, 0) is always in the
        // forced chunk.
        BlockPos pos = new BlockPos(0, 2, 0);
        // The death starts an invasion of Kerbin (the test level's body), which invasion_angel_death_starts_invasion
        // checks on the same body: one test at a time owns it (WarGameTests.KerbinInvasion says why).
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        Object owner = new Object();
        LivingEntity[] angel = new LivingEntity[1];
        long[] killedAt = new long[1];
        String[] result = new String[1];
        boolean[] done = new boolean[1];
        helper.succeedWhen(() -> {
            if (!done[0]) {
                if (angel[0] == null) {
                    if (!WarGameTests.KerbinInvasion.claim(level, owner)) {
                        helper.fail("waiting for the other angel test to give Kerbin's invasion back");
                        return;
                    }
                    angel[0] = helper.spawn(SpaceMobs.BF_ANGEL.get(), pos);
                    kill(helper, angel[0]);
                    killedAt[0] = level.getGameTime();
                    helper.fail("the angel is dying");
                    return;
                }
                if (level.getGameTime() - killedAt[0] < 70) {
                    helper.fail("the angel is dying");
                    return;
                }
                // stop the angel's invasion before its 122-tick countdown runs out, then give Kerbin back
                com.ntmspace.war.Invasions.stop(level);
                WarGameTests.KerbinInvasion.release(owner);
                done[0] = true;
                Map<Item, Integer> drops = collect(itemsAround(helper, pos));
                Integer core = drops.get(SpaceMobs.CORE_ANGEL.get());
                if (core == null || core != 1) {
                    result[0] = "the angel's death dropped " + drops + ", 1.7 one Angelic Core";
                } else if (angel[0].isAlive() || !angel[0].isRemoved()) {
                    result[0] = "the angel is still there after its death";
                }
                if (result[0] != null) NtmSpace.LOG.error("GameTest failure: {}", result[0]);
            }
            // judged once: a retry only reports it again and never touches Kerbin, which another test may own now
            if (result[0] != null) helper.fail(result[0]);
        });
    }

    // ------------------------------------------------------------------------------------------- natural spawns

    private record Entry(String type, int weight, int min, int max) {
        @Override
        public String toString() {
            return type + " " + weight + " " + min + "-" + max;
        }
    }

    private static void naturalSpawns(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Entry cowMun = new Entry("ntmspace:entity_moon_cow", 10, 4, 4);
        Entry scutter = new Entry("ntmspace:entity_scutterfish", 10, 4, 4);
        Entry eel = new Entry("ntmspace:entity_siftereel", 10, 1, 3);
        Entry flesh = new Entry("ntmspace:entity_mob_flesh_creeper", 10, 4, 4);

        expect(helper, errors, "mun", MobCategory.CREATURE, cowMun);
        expect(helper, errors, "thatmo", MobCategory.CREATURE, new Entry("ntmspace:entity_tankbot", 6, 1, 1),
                new Entry("ntmspace:entity_war_behemoth", 2, 1, 1), new Entry("ntmspace:entity_moon_cow", 10, 1, 1));
        for (String laythe : new String[] {"laythe_islands", "laythe_polar", "laythe_coast", "laythe_ocean"}) {
            expect(helper, errors, laythe, MobCategory.MONSTER, flesh);
        }
        expect(helper, errors, "laythe_islands", MobCategory.WATER_CREATURE, scutter, eel);
        expect(helper, errors, "laythe_polar", MobCategory.WATER_CREATURE, scutter, eel);
        expect(helper, errors, "laythe_coast", MobCategory.WATER_CREATURE, scutter, eel,
                new Entry("ntmspace:entity_scuttlecrab", 4, 1, 3));
        expect(helper, errors, "laythe_ocean", MobCategory.WATER_CREATURE, scutter, eel,
                new Entry("ntmspace:entity_scrapfish", 2, 1, 4), new Entry("ntmspace:entity_siftereel", 1, 1, 4),
                new Entry("ntmspace:entity_depthsquid", 1, 1, 4));

        Biome mun = Biomes.holder(helper.getLevel().registryAccess(), Biomes.key(NtmSpace.MOD_ID, "mun")).value();
        float chance = mun.getMobSettings().getCreatureProbability();
        if (Math.abs(chance - 0.008F) > 1.0E-6F) errors.add("mun creature spawning chance " + chance + ", 1.7 0.008");

        finish(helper, errors);
    }

    private static void expect(GameTestHelper helper, List<String> errors, String biome, MobCategory category, Entry... expected) {
        Holder<Biome> holder = Biomes.holder(helper.getLevel().registryAccess(), Biomes.key(NtmSpace.MOD_ID, biome));
        List<Entry> actual = new ArrayList<>();
        for (Spawns.SpawnEntry e : Spawns.entries(holder.value(), category)) {
            String id = EntityType.getKey(e.type()).toString();
            if (id.startsWith(NtmSpace.MOD_ID + ":")) actual.add(new Entry(id, e.weight(), e.minCount(), e.maxCount()));
        }
        if (!actual.equals(List.of(expected))) {
            errors.add(biome + " " + category.getName() + ": " + actual + ", hbm-space " + List.of(expected));
        }
    }

    // ------------------------------------------------------------------------------------------- tag

    private static void suffocationTag(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, dev.golem.core.Ids.of(NtmSpace.MOD_ID, "suffocation_immune"));
        for (EntityType<?> type : new EntityType<?>[] {SpaceMobs.MOON_COW.get(), SpaceMobs.TANKBOT.get(),
                SpaceMobs.WAR_BEHEMOTH.get(), SpaceMobs.BF_ANGEL.get(), SpaceMobs.SIEGE_UFO.get(),
                Entities.byId("hbm:entity_cyber_crab"), Entities.byId("hbm:entity_glyphid"), Entities.byId("hbm:entity_ufo")}) {
            if (type == null || !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag)) errors.add(type == null ? "a tagged HBM type is missing" : EntityType.getKey(type) + " is not suffocation immune");
        }
        for (EntityType<?> type : new EntityType<?>[] {SpaceMobs.SCUTTERFISH.get(), SpaceMobs.CREEPER_FLESH.get(),
                SpaceMobs.DEPTH_SQUID.get()}) {
            if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag)) errors.add(EntityType.getKey(type) + " is suffocation immune; hbm-space's is not");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------- siege tier

    private static void siegeSave(GameTestHelper helper) {
        EntitySiegeUFO ufo = helper.spawn(SpaceMobs.SIEGE_UFO.get(), new BlockPos(1, 2, 1));
        ufo.setTier(SiegeTier.GOLD);
        net.minecraft.nbt.CompoundTag tag = Entities.saveToTag(ufo);
        ufo.discard();
        String text = tag.toString();
        if (!text.contains("\"gold\"")) {
            fail(helper, "the saucer's save does not name its tier: " + text);
            return;
        }
        EntitySiegeUFO loaded = Entities.createDetached(SpaceMobs.SIEGE_UFO.get(), helper.getLevel());
        Entities.loadFromTag(loaded, tag);
        if (loaded.getTier() != SiegeTier.GOLD) {
            fail(helper, "the saucer loaded as " + loaded.getTier().name + ", saved gold");
            return;
        }
        helper.succeed();
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            fail(helper, String.join("; ", errors));
        }
    }

    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
