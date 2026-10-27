package com.ntmspace.gametest;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.ntmspace.NtmSpace;
import com.ntmspace.entity.mob.siege.EntitySiegeCraft;
import com.ntmspace.entity.mob.siege.EntitySiegeUFO;
import com.ntmspace.entity.mob.siege.SiegeTier;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceMobs;
import com.ntmspace.registry.WarContent;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.satellite.SatelliteRailgun;
import com.ntmspace.tileentity.war.TileEntityMachineWarController;
import com.ntmspace.trait.CBT_Destroyed;
import com.ntmspace.trait.CBT_Invasion;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.trait.SolarSystemSavedData;
import com.ntmspace.war.Invasions;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The war between bodies and the invasions (hbm-space {@code CBT_War}, {@code CBT_Invasion}, {@code EntityBFAngel},
 * {@code TileEntityMachineWarController}, {@code SatelliteRailgun}, {@code EntitySiegeCraft}). Expected values are
 * 1.7's, worked by hand from the 1.7 classes. Tests that must touch a body's saved traits use Moho or the Mun and put
 * them back as they found them; the angel's own body is the test level's, Kerbin, whose invasion is removed before its
 * 122-tick countdown ends. The drop pod's landing is {@code DropPodGameTests}' ({@code drop_pod_lands_and_releases}).
 */
public final class WarGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** CBT_War and CBT_Invasion: saved by name with their progress; synced with the flash (1.7 perma-sync). */
    public static final GameTests.Function SAVE = TESTS.function("war_traits_save_and_sync", 20, () -> WarGameTests::saveAndSync);

    /** A war projectile: 1.7 travel countdown, impact flash curve, 100-tick impact, damage to the target, split shot. */
    public static final GameTests.Function PROGRESSION = TESTS.function("war_projectile_progression", 20,
            () -> WarGameTests::progression);

    /** The invasion's countdown, waves by kills, the boss at the final wave, pod bursts and the end on the boss's death. */
    public static final GameTests.Function WAVES = TESTS.function("invasion_waves_17", 40, () -> WarGameTests::waves);

    /** The saucers an invasion spawns, their 1.7 tier table and what each kill is worth. */
    public static final GameTests.Function SPAWNS = TESTS.function("invasion_spawns_and_kills", 20, () -> WarGameTests::spawns);

    /** The angel's death starts an invasion of its body. */
    public static final GameTests.Function ANGEL = TESTS.function("invasion_angel_death_starts_invasion", 200,
            () -> WarGameTests::angel);

    /** The war controller aims the railgun at the drive's body; a click starts the war, the next fires a projectile. */
    public static final GameTests.Function CONTROLLER = TESTS.function("war_controller_railgun", 60, () -> WarGameTests::controller);

    /** The siege craft: tier health x25, damage threshold, tier saved by name. */
    public static final GameTests.Function CRAFT = TESTS.function("war_siege_craft_tier", 20, () -> WarGameTests::craft);

    private WarGameTests() {}

    public static void init() {}

    // ------------------------------------------------------------------------------------------------ save / sync

    private static void saveAndSync(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        CBT_War war = new CBT_War(64, 7);
        war.launchProjectile(0F, 20, 50, 3F, 55, 20, CBT_War.ProjectileType.SPLITSHOT, "ntmspace:moho");
        CBT_War.Projectile p = war.projectiles.get(0);
        p.setAnimtime(12);
        p.setFlashtime(42.5F);
        CBT_Invasion invasion = new CBT_Invasion(3, 17.5, true);
        invasion.kills = 33;
        invasion.killreq = 150;
        invasion.podBurstCounter = 2;
        invasion.podCooldown = 321;
        invasion.bossSpawned = true;
        invasion.warningPlayed = true;

        SolarSystemSavedData data = new SolarSystemSavedData();
        data.setTraits("moho", war, invasion);
        Tag tag = SolarSystemSavedData.toTag(data);
        String nbt = tag.toString();
        for (String expected : new String[] {"\"SPLITSHOT\"", "\"ntmspace:moho\"", "animtime", "podCooldown", "bossSpawned"}) {
            if (!nbt.contains(expected)) errors.add("save lacks " + expected);
        }
        if (nbt.contains("flashtime")) errors.add("1.7 never saved the flash: " + nbt);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> back = SolarSystemSavedData.fromTag(tag).getTraits("moho");
        CBT_War war2 = back == null ? null : (CBT_War) back.get(CBT_War.class);
        CBT_Invasion inv2 = back == null ? null : (CBT_Invasion) back.get(CBT_Invasion.class);
        if (war2 == null || war2.health != 64 || war2.shield != 7 || war2.projectiles.size() != 1
                || war2.projectiles.get(0).getAnimtime() != 12 || war2.projectiles.get(0).getFlashtime() != 0F
                || war2.projectiles.get(0).getType() != CBT_War.ProjectileType.SPLITSHOT) {
            errors.add("war round trip " + (war2 == null ? null : war2.health + "/" + war2.shield + " " + war2.projectiles.size()));
        }
        if (inv2 == null || inv2.wave != 3 || inv2.waveTime != 17.5 || !inv2.isInvading || inv2.kills != 33 || inv2.killreq != 150
                || inv2.podBurstCounter != 2 || inv2.podCooldown != 321 || !inv2.bossSpawned || !inv2.warningPlayed) {
            errors.add("invasion round trip lost a field");
        }

        // Client sync preserves health and shield independently; the flash rides along (perma-sync).
        RegistryFriendlyByteBuf buf = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        war.writeToBytes(buf);
        CBT_War client = new CBT_War();
        client.readFromBytes(buf);
        if (client.health != 64) errors.add("synced war health: expected 64, got " + client.health);
        if (client.shield != 7) errors.add("synced war shield: expected 7, got " + client.shield);
        if (client.projectiles.size() != 1 || client.projectiles.get(0).getFlashtime() != 42.5F
                || client.projectiles.get(0).getAnimtime() != 12) {
            errors.add("synced projectile lost its flash or animation");
        }
        buf.release();

        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ projectiles

    private static void progression(GameTestHelper helper) {
        CelestialTraits.attach(helper.getLevel());
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> before = copy(CelestialTraits.getTraits("moho"));
        List<String> errors = new ArrayList<>();
        try {
            CelestialTraits.setTraits("moho", new HashMap<>(before) {{ remove(CBT_War.class); remove(CBT_Destroyed.class); }});

            CBT_War war = new CBT_War(100, 0);
            war.launchProjectile(3F, 20, 50, 0F, 55, 20, CBT_War.ProjectileType.SMALL, "ntmspace:moho");
            CBT_War.Projectile p = war.projectiles.get(0);
            for (int i = 0; i < 3; i++) war.update(false, "kerbin");
            // the tick it arrives: travel 0, still no animation, so impact() resets the flash to 0
            if (p.getTravel() != 0F || p.getAnimtime() != 0 || p.getFlashtime() != 0F) {
                errors.add("after 3 ticks travel " + p.getTravel() + " anim " + p.getAnimtime() + " flash " + p.getFlashtime());
            }
            war.update(false, "kerbin");
            // 1.7 impact(): f += 0.6; f = min(100, f + 0.1 (100 - f) 0.15) = 0.6 + 1.491
            float expected = 0.6F;
            expected = Math.min(100.0F, expected + 0.1F * (100.0F - expected) * 0.15F);
            if (p.getAnimtime() != 1 || p.getFlashtime() != expected) {
                errors.add("first impact tick anim " + p.getAnimtime() + " flash " + p.getFlashtime() + ", 1.7 1 / " + expected);
            }
            for (int i = 0; i < 98; i++) war.update(false, "kerbin");
            if (war.projectiles.size() != 1 || p.getAnimtime() != 99) errors.add("before the 100th impact tick anim " + p.getAnimtime());
            CBT_War target = CelestialTraits.getTrait("moho", CBT_War.class, false);
            if (target != null) errors.add("Moho damaged before the impact ended");
            war.update(false, "kerbin");
            target = CelestialTraits.getTrait("moho", CBT_War.class, false);
            if (!war.projectiles.isEmpty()) errors.add("the projectile outlived its impact");
            // CelestialBody.damage: a body without a war gets CBT_War() (100) and loses the damage (no shield)
            if (target == null || target.health != 50 || target.shield != 0) {
                errors.add("Moho after one hit: " + (target == null ? "no war" : target.health + "/" + target.shield) + ", 1.7 50/0");
            }

            // a beaten attacker: the hit destroys the target's planet instead (1.7 reads the attacker's health)
            CBT_War beaten = new CBT_War(0, 0);
            beaten.launchProjectile(0F, 20, 50, 0F, 55, 20, CBT_War.ProjectileType.MEDIUM, "ntmspace:moho");
            for (int i = 0; i < 100; i++) beaten.update(false, "kerbin");
            if (!CelestialTraits.hasTrait("moho", CBT_Destroyed.class, false)) errors.add("a 0-health war did not destroy Moho");

            // the split shot: arriving, it becomes four small shots 20, 30, 40, 50 ticks out, which the same pass of 1.7's
            // loop (it walks on into the appended shots) already moves one tick: 19, 29, 39, 49
            CBT_War split = new CBT_War(100, 0);
            split.launchProjectile(1F, 20, 50, 5F, 55, 20, CBT_War.ProjectileType.SPLITSHOT, "ntmspace:moho");
            split.update(false, "kerbin");
            List<Float> travels = new ArrayList<>();
            for (CBT_War.Projectile s : split.projectiles) {
                travels.add(s.getTravel());
                if (s.getType() != CBT_War.ProjectileType.SMALL) errors.add("split part of type " + s.getType());
            }
            if (!travels.equals(List.of(19F, 29F, 39F, 49F))) errors.add("split shot travels " + travels + ", 1.7 [19, 29, 39, 49]");
        } finally {
            CelestialTraits.setTraits("moho", before);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ waves

    private static void waves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();

        // the countdown: warning armed until the last 5 ticks, then the invasion
        CBT_Invasion countdown = new CBT_Invasion(0, 7, false);
        countdown.prepare();
        if (countdown.waveTime != 6 || !countdown.warningPlayed || countdown.isInvading) errors.add("countdown at 6");
        countdown.prepare();
        if (countdown.waveTime != 5 || countdown.warningPlayed) errors.add("1.7: the warning is due at 5 ticks left");
        for (int i = 0; i < 5; i++) countdown.prepare();
        if (!countdown.isInvading || countdown.waveTime != 0) errors.add("invading after the countdown: " + countdown.waveTime);

        // waves by kills, the boss through the spawner (swapped for a recorder inside this one tick)
        CBT_Invasion.Spawner real = CBT_Invasion.SPAWNER;
        int[] bosses = {0};
        CBT_Invasion.SPAWNER = new CBT_Invasion.Spawner() {
            @Override public void dropPod(ServerLevel l, CBT_Invasion i) {}
            @Override public LivingEntity ufo(ServerLevel l, CBT_Invasion i) { return null; }
            @Override public EntityUFO boss(ServerLevel l, CBT_Invasion i) {
                bosses[0]++;
                return new EntityUFO(ModNtmEntities.UFO.get(), l);
            }
        };
        try {
            CBT_Invasion inv = new CBT_Invasion(0, 0, true);
            inv.logicTick(level);
            if (inv.wave != 1) errors.add("wave 0 must start wave 1 at once");
            int[][] steps = {{1, 20}, {2, 100}, {3, 150}};
            for (int[] step : steps) {
                inv.kills = step[1] - 1;
                inv.logicTick(level);
                if (inv.wave != step[0] || inv.killreq != step[1]) {
                    errors.add("wave " + step[0] + " needs " + step[1] + " kills: wave " + inv.wave + " req " + inv.killreq);
                }
                inv.kills = step[1];
                inv.logicTick(level);
                if (inv.wave != step[0] + 1 || inv.kills != 0) errors.add("wave " + step[0] + " did not end at " + step[1] + " kills");
            }
            inv.logicTick(level);
            inv.logicTick(level);
            if (inv.wave != 4 || inv.killreq != 1 || !inv.bossSpawned || bosses[0] != 1) {
                errors.add("final wave: wave " + inv.wave + " req " + inv.killreq + " bosses " + bosses[0] + ", 1.7 4 / 1 / one boss");
            }
            if (inv.getMaxHealth() != 1F || inv.getHealth() != 1F) errors.add("final boss bar " + inv.getHealth() + "/" + inv.getMaxHealth());
        } finally {
            CBT_Invasion.SPAWNER = real;
        }

        // pod bursts: wave 1 drops 3 pods (3 + wave - 1), then rests 500 ticks; pods roll on ticks divisible by 10
        helper.succeedWhen(() -> {
            if (level.getGameTime() % 10 != 0) {
                helper.fail("waiting for a tick divisible by 10");
                return;
            }
            CBT_Invasion.DropPod realPod = CBT_Invasion.DROP_POD;
            int[] pods = {0};
            CBT_Invasion.DROP_POD = (l, i) -> pods[0]++;
            CBT_Invasion burst = new CBT_Invasion(1, 0, true);
            int calls = 0;
            try {
                while (burst.podCooldown == 0 && calls < 2000) {
                    burst.handleBurstSpawning(level);
                    calls++;
                }
                burst.handleBurstSpawning(level);
                CBT_Invasion late = new CBT_Invasion(4, 0, true);
                late.handleBurstSpawning(level);
            } finally {
                CBT_Invasion.DROP_POD = realPod;
            }
            if (pods[0] != 3 || burst.podBurstCounter != 0 || burst.podCooldown != 499) {
                errors.add("wave 1 burst: " + pods[0] + " pods, counter " + burst.podBurstCounter + ", cooldown " + burst.podCooldown
                        + " after " + calls + " calls; 1.7: 3, 0, 500 then 499");
            }
            check(helper, errors);
        });
    }

    // ------------------------------------------------------------------------------------------------ spawns, kills

    private static void spawns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();

        // 1.7 tier table, wave factor 0 and 0.2
        Object[][] table = {
                {0.10F, 0F, SiegeTier.CLAY}, {0.20F, 0F, SiegeTier.STONE}, {0.40F, 0F, SiegeTier.IRON}, {0.60F, 0F, SiegeTier.SILVER},
                {0.70F, 0F, SiegeTier.GOLD}, {0.85F, 0F, SiegeTier.DESH}, {0.95F, 0F, SiegeTier.SCHRAB}, {0.98F, 0F, SiegeTier.DNT},
                {0.0F, 0.2F, SiegeTier.STONE}, {0.15F, 0.2F, SiegeTier.IRON}, {0.35F, 0.2F, SiegeTier.SILVER},
                {0.50F, 0.2F, SiegeTier.GOLD}, {0.80F, 0.2F, SiegeTier.DESH}, {0.90F, 0.2F, SiegeTier.SCHRAB}};
        for (Object[] row : table) {
            SiegeTier got = Invasions.smallTier((Float) row[0], (Float) row[1]);
            if (got != row[2]) errors.add("roll " + row[0] + " factor " + row[1] + ": " + got.name + ", 1.7 " + ((SiegeTier) row[2]).name);
        }

        // kills: a small saucer 1 (2 from GOLD up), the craft 10, anything else 0; the UFO ends the final wave only
        EntitySiegeUFO stone = Entities.createDetached(SpaceMobs.SIEGE_UFO.get(), level);
        stone.setTier(SiegeTier.STONE);
        EntitySiegeUFO gold = Entities.createDetached(SpaceMobs.SIEGE_UFO.get(), level);
        gold.setTier(SiegeTier.GOLD);
        EntitySiegeCraft craft = Entities.createDetached(WarContent.SIEGE_CRAFT.get(), level);
        craft.setTier(SiegeTier.IRON);
        LivingEntity ufo = Entities.createDetached(ModNtmEntities.UFO.get(), level);
        if (Invasions.kills(stone) != 1 || Invasions.kills(gold) != 2 || Invasions.kills(craft) != 10 || Invasions.kills(ufo) != 0) {
            errors.add("kill values " + Invasions.kills(stone) + "/" + Invasions.kills(gold) + "/" + Invasions.kills(craft) + "/"
                    + Invasions.kills(ufo) + ", 1.7 1/2/10/0");
        }
        CBT_Invasion.KillValue kills = CBT_Invasion.KILLS;
        if (kills == null || !kills.endsInvasion(ufo, 4) || kills.endsInvasion(ufo, 3) || kills.endsInvasion(craft, 4)) {
            errors.add("only the UFO's death in wave 4 ends the invasion");
        }

        // onKill on a body: a craft adds 10, the boss ends it
        CelestialTraits.attach(level);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> before = copy(CelestialTraits.getTraits("moho"));
        try {
            CBT_Invasion inv = new CBT_Invasion(4, 1000, false);
            CelestialTraits.modifyTraits("moho", inv);
            inv.onKill(craft, "moho");
            if (inv.kills != 10) errors.add("a craft kill counted " + inv.kills);
            inv.onKill(ufo, "moho");
            if (CelestialTraits.hasTrait("moho", CBT_Invasion.class, false)) errors.add("the boss's death did not end the invasion");
        } finally {
            CelestialTraits.setTraits("moho", before);
        }

        // a real spawn over a player at y 100: a saucer 30..49 blocks up, near the player
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        BlockPos abs = helper.absolutePos(new BlockPos(0, 1, 0));
        try {
            player.setPos(abs.getX() + 0.5, 100, abs.getZ() + 0.5);
            // with other tests' players in the level the spawner picks one at random: accept a spawn over any of them
            LivingEntity spawned = null;
            for (int i = 0; i < 20 && spawned == null; i++) spawned = Invasions.ufo(level, 1);
            if (spawned == null) {
                errors.add("no saucer spawned in 20 tries");
            } else {
                boolean near = false;
                for (ServerPlayer p : level.players()) {
                    double dy = spawned.getY() - p.getY();
                    if (dy >= 30 && dy < 50 && Math.abs(spawned.getX() - p.getX()) < 200 && Math.abs(spawned.getZ() - p.getZ()) < 200) near = true;
                }
                if (!near) errors.add("saucer at " + spawned.position() + " is 30..49 over no player");
                if (!(spawned instanceof EntitySiegeUFO)) errors.add("wave 1 spawned a " + spawned.getType() + ", 1.7 only small saucers");
                spawned.discard();
            }
        } finally {
            level.getServer().getPlayerList().remove(player);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ the angel

    private static void angel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CelestialTraits.attach(level);
        Object owner = new Object();
        LivingEntity[] angel = new LivingEntity[1];
        List<String> result = new ArrayList<>();
        boolean[] done = new boolean[1];
        helper.succeedWhen(() -> {
            if (done[0]) {
                // judged once: a retry only reports it again and never touches Kerbin, which another test may own now
                check(helper, result);
                return;
            }
            if (angel[0] == null) {
                if (!KerbinInvasion.claim(level, owner)) {
                    helper.fail("waiting for the other angel test to give Kerbin's invasion back");
                    return;
                }
                // start from no invasion, so the one found afterwards is this angel's
                Invasions.stop(level);
                // (0, 2, 0): the column of the test's forced chunk on 26.1, as mobs_angel_core (the death takes 50 ticks)
                angel[0] = helper.spawn(SpaceMobs.BF_ANGEL.get(), new BlockPos(0, 2, 0));
                Compat.hurt(angel[0], level, level.damageSources().generic(), 1.0E7F);
                helper.fail("the angel is dying");
                return;
            }
            if (angel[0].isAlive() || !angel[0].isRemoved()) {
                helper.fail("the angel is still dying");
                return;
            }
            CBT_Invasion invasion = CelestialTraits.getTrait(level, CBT_Invasion.class);
            if (invasion == null) {
                result.add("no invasion on " + CelestialTraits.bodyOf(level) + " after the angel's death");
            } else if (invasion.isInvading || invasion.waveTime <= 0 || invasion.waveTime > 122) {
                result.add("invasion " + invasion.wave + " invading " + invasion.isInvading + " countdown " + invasion.waveTime
                        + ", 1.7 CBT_Invasion(0, 122, false)");
            }
            Invasions.stop(level);
            if (CelestialTraits.hasTrait(level, CBT_Invasion.class)) result.add("the test could not remove the invasion");
            KerbinInvasion.release(owner);
            done[0] = true;
            check(helper, result);
        });
    }

    /**
     * Kerbin's invasion is one trait of the GameTest level's body, shared by every test in a batch. Two tests kill an
     * angel there ({@code invasion_angel_death_starts_invasion} and {@code SpaceMobGameTests}' {@code mobs_angel_core}),
     * and each must remove the invasion its angel starts before the 122-tick countdown ends. Run side by side they broke
     * each other: below 26.1 a test waits 20 ticks for its structure unless it was already placed
     * ({@code GameTestInfo.ticksToWaitForChunkLoading}), so batch-mates start 0 or 20 ticks apart; when this test started
     * 20 ticks after mobs_angel_core its angel died on mobs_angel_core's tick 70, found that test's invasion (so started
     * none, as 1.7) and mobs_angel_core then removed it before this test looked: "no invasion on kerbin" (1 run in 10 on
     * 1.21.1 Fabric). So one test at a time owns Kerbin's invasion: it claims it, clears it, kills its angel, checks and
     * removes the invasion, then releases it. A claim left by a test that failed half way lapses after 200 ticks.
     */
    static final class KerbinInvasion {

        private static final long LAPSE_TICKS = 200;

        private static @org.jspecify.annotations.Nullable Object owner;
        private static @org.jspecify.annotations.Nullable ServerLevel ownerLevel;
        private static long claimedAt;

        private KerbinInvasion() {}

        /** True when {@code who} owns Kerbin's invasion now (it already did, or nobody did). Server thread only. */
        static boolean claim(ServerLevel level, Object who) {
            long now = level.getGameTime();
            if (owner == who) return true;
            if (owner != null && ownerLevel == level && now - claimedAt < LAPSE_TICKS) return false;
            owner = who;
            ownerLevel = level;
            claimedAt = now;
            return true;
        }

        static void release(Object who) {
            if (owner != who) return;
            owner = null;
            ownerLevel = null;
        }
    }

    // ------------------------------------------------------------------------------------------------ controller

    private static void controller(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CelestialTraits.attach(level);
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, WarContent.WAR_CONTROLLER.get());
        BlockPos abs = helper.absolutePos(rel);
        if (!(level.getBlockEntity(abs) instanceof TileEntityMachineWarController te)) {
            helper.fail("war_controller has no tile entity");
            return;
        }
        int freq = 900_000 + level.getRandom().nextInt(90_000);
        SatelliteSavedData data = SatelliteSavedData.get(level, abs);
        SatelliteRailgun railgun = new SatelliteRailgun();
        data.put(freq, railgun);
        ItemStack chip = new ItemStack(ModSatelliteItems.sat_chip.get());
        ISatChip.setFreqS(chip, freq);
        te.setItem(2, chip);
        te.setItem(1, new ItemStack(RocketItems.drive(DriveBody.MUN)));
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> before = copy(CelestialTraits.getTraits("mun"));
        CelestialTraits.setTraits("mun", new HashMap<>(before) {{ remove(CBT_War.class); }});

        // setpos through the control packet path
        CompoundTag setpos = new CompoundTag();
        setpos.putInt("xcoord", 123);
        setpos.putInt("zcoord", -456);
        te.receiveControl(setpos);

        long start = level.getGameTime();
        int[] phase = {0};
        String[] failure = {null};
        helper.succeedWhen(() -> {
            if (failure[0] != null) {
                helper.fail(failure[0]);
                return;
            }
            if (level.getGameTime() - start < 2) {
                helper.fail("waiting for the controller to tick");
                return;
            }
            List<String> errors = new ArrayList<>();
            if (phase[0] == 0) {
                if (te.id != freq || !te.satPresent || !"SatelliteRailgun".equals(te.satName)) {
                    errors.add("controller sees " + te.id + " " + te.satPresent + " " + te.satName);
                }
                ItemVOTVdrive.Destination dest = ItemVOTVdrive.getDestination(te.getItem(1));
                if (dest == null || dest.x != 123 || dest.z != -456) errors.add("setpos did not reach the drive");
                // first click: no war yet on the aimed body, so the war starts (CBT_War(100, 0))
                railgun.onClick(level, 0, 0);
                CBT_War war = CelestialTraits.getTrait("mun", CBT_War.class, false);
                if (war == null || war.health != 100 || war.shield != 0 || !war.projectiles.isEmpty()) {
                    errors.add("the first click must start a war on the Mun");
                }
                if (!errors.isEmpty()) {
                    cleanup(data, freq, before);
                    failure[0] = String.join("; ", errors);
                    NtmSpace.LOG.error("GameTest failure: {}", failure[0]);
                    helper.fail(failure[0]);
                    return;
                }
                phase[0] = 1;
                helper.fail("waiting for the flash");
                return;
            }
            // the flash runs while armed (WarSatellites ticks fire())
            if (railgun.interp <= 0) {
                helper.fail("the railgun's flash did not start");
                return;
            }
            railgun.onClick(level, 0, 0);
            CBT_War war = CelestialTraits.getTrait("mun", CBT_War.class, false);
            if (war == null || war.projectiles.size() != 1 || war.projectiles.get(0).getType() != CBT_War.ProjectileType.SMALL
                    || war.projectiles.get(0).getTravel() != 100F || war.projectiles.get(0).getDamage() != 50
                    || !"ntmspace:mun".equals(war.projectiles.get(0).getTarget())) {
                errors.add("the second click must fire one small projectile (travel 100, damage 50) at ntmspace:mun");
            }
            cleanup(data, freq, before);
            if (!errors.isEmpty()) {
                failure[0] = String.join("; ", errors);
                NtmSpace.LOG.error("GameTest failure: {}", failure[0]);
                helper.fail(failure[0]);
            }
        });
    }

    private static void cleanup(SatelliteSavedData data, int freq,
                                Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> mun) {
        data.sats.remove(freq);
        data.setDirty();
        CelestialTraits.setTraits("mun", mun);
    }

    // ------------------------------------------------------------------------------------------------ craft

    private static void craft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        EntitySiegeCraft craft = Entities.createDetached(WarContent.SIEGE_CRAFT.get(), level);
        craft.setTier(SiegeTier.DNT);
        if (craft.getMaxHealth() != 500 * 25 || craft.getHealth() != craft.getMaxHealth()) {
            errors.add("DNT craft health " + craft.getMaxHealth() + ", 1.7 12500");
        }
        if (craft.getBbWidth() != 7F || craft.getBbHeight() != 1F || !craft.fireImmune()) errors.add("1.7 7 x 1, fireproof");
        CompoundTag saved = Entities.saveToTag(craft);
        if (!"dnt".equals(dev.golem.core.data.Tags.getStringOr(saved, "siegeTier", ""))) errors.add("tier not saved by name: " + saved);
        EntitySiegeCraft loaded = Entities.createDetached(WarContent.SIEGE_CRAFT.get(), level);
        Entities.loadFromTag(loaded, saved);
        if (loaded.getTier() != SiegeTier.DNT) errors.add("tier round trip " + loaded.getTier().name);

        // DNT: 20 damage threshold, 90 % resistance
        LivingEntity spawned = helper.spawn(WarContent.SIEGE_CRAFT.get(), new BlockPos(1, 3, 1));
        ((EntitySiegeCraft) spawned).setTier(SiegeTier.DNT);
        float full = spawned.getHealth();
        Compat.hurt(spawned, level, level.damageSources().generic(), 15F);
        if (spawned.getHealth() != full) errors.add("15 damage passed DNT's threshold of 20");
        spawned.invulnerableTime = 0;
        Compat.hurt(spawned, level, level.damageSources().generic(), 120F);
        float expected = full - (120F - 20F) * (1F - 0.9F);
        if (Math.abs(spawned.getHealth() - expected) > 0.01F) {
            errors.add("120 damage left " + spawned.getHealth() + ", 1.7 " + expected);
        }
        spawned.discard();
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> copy(
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> out = new HashMap<>();
        for (Map.Entry<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> e : traits.entrySet()) out.put(e.getKey(), e.getValue().copy());
        return out;
    }

    /** Inside {@code succeedWhen}: throws on errors (logged), returns otherwise so the test succeeds. */
    private static void check(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) return;
        String message = String.join("; ", errors);
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            String message = String.join("; ", errors);
            NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
        }
    }
}
