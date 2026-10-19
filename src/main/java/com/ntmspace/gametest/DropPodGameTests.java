package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import com.ntmspace.entity.missile.CombatDropPods;
import com.ntmspace.entity.missile.EntityCombatDropPod;
import com.ntmspace.registry.DropPodContent;
import com.ntmspace.tileentity.machine.storage.TileEntityCombatDropPod;
import com.ntmspace.trait.CBT_Invasion;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The combat drop pod (hbm-space {@code EntityCombatDropPod}, {@code TileEntityCombatDropPod},
 * {@code CBT_Invasion.spawnCattle}). Expected values are 1.7's: the pod lands where the block above its position is
 * solid and becomes {@code combat_drop} at {@code (int)x, (int)y + 1, (int)z}; the block waits 40 ticks, then opens and
 * releases {@code amount} payload entities one block up; wave 1 pods carry two {@code hbm:entity_glyphid}, colour 2,
 * 250 blocks up.
 */
public final class DropPodGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;
    private static final int PADDING = 8;

    /** A falling pod lands on a floor, becomes the block, and after the 1.7 delay releases its two glyphids. */
    public static final GameTests.Function LANDS = TESTS.function("drop_pod_lands_and_releases", 160, PADDING,
            () -> DropPodGameTests::landsAndReleases);

    /** The invasion's drop-pod hook spawns a wave-1 pod 250 blocks over a player. */
    public static final GameTests.Function INVASION = TESTS.function("drop_pod_invasion_spawns_pod", 20,
            () -> DropPodGameTests::invasion);

    /** Pod entity and block entity save their payload by registry name and survive a round trip. */
    public static final GameTests.Function SAVES = TESTS.function("drop_pod_saves_by_name", 20,
            () -> DropPodGameTests::saves);

    private DropPodGameTests() {}

    public static void init() {}

    private static void landsAndReleases(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = GameTests.paddedCentre(PADDING, 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), Blocks.STONE);
            }
        }
        BlockPos floor = helper.absolutePos(centre);
        double x = floor.getX() + 0.5, y = floor.getY() + 10, z = floor.getZ() + 0.5;
        // 1.7: the block goes to (int)x, (int)y + 1, (int)z at the step where (int)(y + 1) is the floor
        BlockPos expected = new BlockPos((int) x, floor.getY(), (int) z);

        EntityCombatDropPod pod = EntityCombatDropPod.create(level);
        pod.setPos(x, y, z);
        CompoundTag payload = CombatDropPods.payload(level, "hbm:entity_glyphid");
        if (payload == null) {
            fail(helper, "no hbm:entity_glyphid to carry");
            return;
        }
        pod.setPayload(payload, 2, 2);
        level.addFreshEntity(pod);

        long start = level.getGameTime();
        helper.succeedWhen(() -> {
            if (!level.getBlockState(expected).is(DropPodContent.COMBAT_DROP.get())) {
                retry(helper, "no combat_drop at " + expected + " (pod alive: " + pod.isAlive() + ", y " + pod.getY() + ")");
            }
            if (pod.isAlive()) retry(helper, "the pod entity is still there after landing");
            if (!(level.getBlockEntity(expected) instanceof TileEntityCombatDropPod te)) {
                retry(helper, "combat_drop has no tile entity");
                return;
            }
            List<Entity> glyphids = glyphids(level, expected);
            if (te.amount != 0 || glyphids.size() != 2) {
                retry(helper, "after " + (level.getGameTime() - start) + " ticks: amount " + te.amount + ", glyphids "
                        + glyphids.size() + ", delay " + te.delay);
            }
            // 1.7: the hatches step 20% of the way to +-90 per tick from the release tick on (18, 32.4, ...)
            if (te.delay != 0 || te.color != 2 || te.hatchopen < 17.9 || te.hatchopen >= 90.0
                    || te.hatchopen2 != -te.hatchopen) {
                retry(helper, "pod state delay " + te.delay + " color " + te.color + " hatches " + te.hatchopen + "/"
                        + te.hatchopen2 + ", 1.7: 0, 2, opening symmetrically");
            }
            for (Entity e : glyphids) {
                if (Math.abs(e.getX() - (expected.getX() + 0.5)) > 1.0 || Math.abs(e.getY() - (expected.getY() + 1)) > 1.0) {
                    retry(helper, "glyphid released at " + e.position() + ", 1.7: block centre one block up");
                }
            }
            for (Entity e : glyphids) e.discard();
        });
    }

    private static List<Entity> glyphids(ServerLevel level, BlockPos pos) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(3))) {
            if ("hbm:entity_glyphid".equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())) out.add(e);
        }
        return out;
    }

    private static void invasion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(0, 1, 0));
        player.setPos(abs.getX() + 0.5, 100, abs.getZ() + 0.5);
        List<String> errors = new ArrayList<>();
        try {
            if (CBT_Invasion.DROP_POD == null) errors.add("CBT_Invasion.DROP_POD is not installed");

            // 1.7 picks a random player of the level (other GameTests' mock players share it; for one below y 50 outside
            // a flat world it spawns nothing) and puts the pod at its column +- a gaussian 15 blocks, which can be a chunk
            // outside the loaded area, where the level holds the pod but does not show it: such a call is repeated (that
            // pod discarded), a null only while another player is in the level
            EntityCombatDropPod pod = null;
            int calls = 0;
            while (calls < 64) {
                pod = CombatDropPods.spawnCattle(level, 1);
                calls++;
                if (pod == null && level.players().size() <= 1) break;
                if (pod != null && level.getEntity(pod.getUUID()) == pod) break;
                if (pod != null) pod.discard();
            }
            NtmSpace.LOG.info("drop_pod_invasion_spawns_pod: {} spawnCattle call(s) for a visible pod", calls);
            if (pod == null) {
                errors.add("spawnCattle spawned nothing");
            } else {
                if (level.getEntity(pod.getUUID()) != pod) errors.add("the pod is not in the level");
                if (pod.getY() != 250) errors.add("pod y " + pod.getY() + ", 1.7 250");
                boolean near = false;
                for (ServerPlayer p : level.players()) {
                    if (Math.abs(pod.getX() - p.getX()) < 150 && Math.abs(pod.getZ() - p.getZ()) < 150) near = true;
                }
                if (!near) errors.add("pod at " + pod.position() + " is near no player");
                CompoundTag payload = pod.getPayload();
                String id = payload == null ? "" : Tags.getStringOr(payload, "id", "");
                if (!"hbm:entity_glyphid".equals(id) || pod.getAmount() != 2 || pod.getColor() != 2) {
                    errors.add("wave 1 payload " + id + " x" + pod.getAmount() + " colour " + pod.getColor()
                            + ", 1.7 hbm:entity_glyphid x2 colour 2");
                }
                pod.discard();
            }

            // through the invasion's hook: one more pod in the level
            if (CBT_Invasion.DROP_POD != null) {
                // one pod per call. The hook returns nothing, so the pod is found among the level's visible entities; its
                // column is the player's +- a gaussian 15 blocks (1.7), which can fall in a chunk outside this test's
                // loaded area (or the pick was another test's player): a call that left no visible pod is repeated, and a
                // hook that spawns nothing fails all 64
                List<EntityCombatDropPod> after = new ArrayList<>();
                int hookCalls = 0;
                while (hookCalls < 64 && after.isEmpty()) {
                    List<EntityCombatDropPod> before = pods(level);
                    CBT_Invasion.DROP_POD.dropPod(level, new CBT_Invasion(1, 0, true));
                    hookCalls++;
                    after = pods(level);
                    after.removeAll(before);
                }
                NtmSpace.LOG.info("drop_pod_invasion_spawns_pod: {} hook call(s) for a visible pod, {} player(s) in the level",
                        hookCalls, level.players().size());
                if (after.size() != 1) errors.add("the DROP_POD hook spawned " + after.size() + " pods, expected 1");
                for (EntityCombatDropPod p : after) p.discard();
            }
        } finally {
            level.getServer().getPlayerList().remove(player);
        }
        if (errors.isEmpty()) helper.succeed();
        else fail(helper, String.join("; ", errors));
    }

    private static List<EntityCombatDropPod> pods(ServerLevel level) {
        List<EntityCombatDropPod> out = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof EntityCombatDropPod pod && pod.isAlive()) out.add(pod);
        }
        return out;
    }

    private static void saves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        CompoundTag payload = CombatDropPods.payload(level, "hbm:entity_glyphid_brawler");
        if (payload == null) {
            fail(helper, "no hbm:entity_glyphid_brawler");
            return;
        }

        // the entity
        EntityCombatDropPod pod = EntityCombatDropPod.create(level);
        pod.setPayload(payload, 3, 1);
        CompoundTag saved = Entities.saveToTag(pod);
        CompoundTag savedPayload = saved.get("entityType") instanceof CompoundTag c ? c : null;
        if (savedPayload == null || !"hbm:entity_glyphid_brawler".equals(Tags.getStringOr(savedPayload, "id", ""))) {
            errors.add("entity save has no payload named hbm:entity_glyphid_brawler: " + saved);
        }
        noNumericId(saved.get("entityType"), "entity payload", errors);
        EntityCombatDropPod loaded = EntityCombatDropPod.create(level);
        Entities.loadFromTag(loaded, saved);
        if (loaded.getAmount() != 3 || loaded.getColor() != 1 || loaded.getPayload() == null
                || !"hbm:entity_glyphid_brawler".equals(Tags.getStringOr(loaded.getPayload(), "id", ""))) {
            errors.add("entity round trip: amount " + loaded.getAmount() + " colour " + loaded.getColor());
        }

        // the block entity
        BlockPos rel = new BlockPos(0, 1, 0);
        helper.setBlock(rel, DropPodContent.COMBAT_DROP.get());
        BlockPos abs = helper.absolutePos(rel);
        if (level.getBlockEntity(abs) instanceof TileEntityCombatDropPod te) {
            te.setPayload(payload, 4, 2);
            te.delay = 17;
            te.hatchopen = 33.5;
            te.hatchopen2 = -12.25;
            TileEntityCombatDropPod copy = new TileEntityCombatDropPod(abs, level.getBlockState(abs));
            te.copyPersistentDataTo(copy);
            if (copy.amount != 4 || copy.color != 2 || copy.delay != 17 || copy.hatchopen != 33.5 || copy.hatchopen2 != -12.25
                    || copy.entityType == null || !"hbm:entity_glyphid_brawler".equals(Tags.getStringOr(copy.entityType, "id", ""))) {
                errors.add("block entity round trip: amount " + copy.amount + " colour " + copy.color + " delay " + copy.delay);
            }
            Entity made = TileEntityCombatDropPod.createEntityFromNBT(copy.entityType, level);
            if (made == null || !"hbm:entity_glyphid_brawler".equals(BuiltInRegistries.ENTITY_TYPE.getKey(made.getType()).toString())) {
                errors.add("the saved payload does not rebuild a brawler");
            }
            CompoundTag unknown = new CompoundTag();
            unknown.putString("id", "ntmspace:no_such_entity");
            if (TileEntityCombatDropPod.createEntityFromNBT(unknown, level) != null) {
                errors.add("an unknown payload id built an entity (1.7: null)");
            }
        } else {
            errors.add("combat_drop has no tile entity");
        }
        // 1.7: getItemDropped = ingot_steel, quantityDropped = 16 (loot table)
        if (errors.isEmpty()) helper.succeed();
        else fail(helper, String.join("; ", errors));
    }

    private static void noNumericId(Tag tag, String what, List<String> errors) {
        if (tag instanceof CompoundTag compound) {
            Tag id = compound.get("id");
            if (id != null && id.getId() != Tag.TAG_STRING) errors.add(what + " stores a numeric id");
        }
    }

    /** A not-yet condition inside {@code succeedWhen}: retried every tick, so it is not logged. */
    private static void retry(GameTestHelper helper, String message) {
        helper.fail(message);
    }

    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
