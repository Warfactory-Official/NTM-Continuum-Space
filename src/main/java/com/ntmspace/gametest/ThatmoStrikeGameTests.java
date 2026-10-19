package com.ntmspace.gametest;

import com.hbm.config.GeneralConfig;
import com.ntmspace.fork.ForkBombers;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.war.ThatmoStrikes;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Thatmo's battlefield strikes ({@code WorldProviderThatmo.updateWeather}, server half; {@link ThatmoStrikes}): with the
 * trait, one B2X per player every 160 ticks (1.7's 8 s wall-clock window), aimed within 40 blocks of the player on x and
 * z at the player's height and flown 20 blocks above it; none without the trait. The jets are built, not spawned (no
 * carpet bombing of the test plot); stand-in "players" are armour stands that are never added to the level.
 */
public final class ThatmoStrikeGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function STRIKES = TESTS.function("thatmo_battlefield_strikes", 1, () -> ThatmoStrikeGameTests::strikes);

    private ThatmoStrikeGameTests() {}

    public static void init() {}

    private static void strikes(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        if (!CelestialTraits.hasDefaultTrait("thatmo", CelestialBodyTrait.CBT_Battlefield.class)) errors.add("thatmo has no battlefield trait");
        if (CelestialTraits.hasDefaultTrait("mun", CelestialBodyTrait.CBT_Battlefield.class)) errors.add("the mun has the battlefield trait");

        Vec3 a = helper.absoluteVec(new Vec3(0.7, 2.2, 0.3));
        Vec3 b = a.add(37.4, 5.5, -12.8);
        List<ArmorStand> players = List.of(new ArmorStand(level, a.x, a.y, a.z), new ArmorStand(level, b.x, b.y, b.z));
        long start = 1600; // a multiple of 160
        int ticks = 0;
        List<Long> strikeTimes = new ArrayList<>();
        for (long t = start; t < start + 2 * ThatmoStrikes.INTERVAL; t++) {
            List<ThatmoStrikes.Strike> strikes = ThatmoStrikes.strikes(level, players, t, true);
            if (strikes.isEmpty()) continue;
            strikeTimes.add(t);
            if (strikes.size() != 2) errors.add("tick " + t + ": " + strikes.size() + " jets for 2 players, 1.7 one each");
            for (int i = 0; i < strikes.size() && i < 2; i++) check(errors, "tick " + t + " player " + i, i == 0 ? a : b, strikes.get(i));
            for (ThatmoStrikes.Strike s : strikes) s.bomber().discard(); // never added; releases its forced chunk
            ticks++;
        }
        if (!strikeTimes.equals(List.of(start, start + 160))) errors.add("strikes at " + strikeTimes + ", 1.7 every 160 ticks: " + start + ", " + (start + 160));

        // the spread covers the whole +-40 square over many strikes
        int minX = 99, maxX = -99, minZ = 99, maxZ = -99;
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(42);
        int px = (int) Math.floor(a.x), pz = (int) Math.floor(a.z);
        for (int n = 0; n < 2000; n++) {
            BlockPos target = ThatmoStrikes.target(rand, players.get(0));
            int dx = target.getX() - px, dz = target.getZ() - pz;
            minX = Math.min(minX, dx); maxX = Math.max(maxX, dx); minZ = Math.min(minZ, dz); maxZ = Math.max(maxZ, dz);
        }
        if (minX != -40 || maxX != 40 || minZ != -40 || maxZ != 40) {
            errors.add("offsets span x " + minX + ".." + maxX + ", z " + minZ + ".." + maxZ + ", 1.7 -40..40");
        }

        for (long t = start; t < start + 2 * ThatmoStrikes.INTERVAL; t++) {
            if (!ThatmoStrikes.strikes(level, players, t, false).isEmpty()) {
                errors.add("a strike at tick " + t + " without the battlefield trait");
                break;
            }
        }
        if (ticks == 0) errors.add("no strikes at all");
        if (errors.isEmpty()) helper.succeed();
        else helper.fail(String.join("; ", errors));
    }

    private static void check(List<String> errors, String what, Vec3 player, ThatmoStrikes.Strike strike) {
        BlockPos target = strike.target();
        int dx = target.getX() - (int) Math.floor(player.x), dz = target.getZ() - (int) Math.floor(player.z);
        if (Math.abs(dx) > ThatmoStrikes.RADIUS || Math.abs(dz) > ThatmoStrikes.RADIUS) errors.add(what + ": target offset " + dx + ", " + dz + ", 1.7 within 40");
        if (target.getY() != (int) Math.floor(player.y)) errors.add(what + ": target y " + target.getY() + ", 1.7 the player's " + Math.floor(player.y));
        var bomber = strike.bomber();
        if (!ForkBombers.B2X_SKIN.equals(bomber.getSkin())) errors.add(what + ": airframe " + bomber.getSkin() + ", 1.7 the B2X");
        if (Math.abs(bomber.getY() - (target.getY() + ThatmoStrikes.HEIGHT)) > 1e-9) errors.add(what + ": jet y " + bomber.getY() + ", 1.7 target + 20");
        // fac(): 100 (short mode) or 200 blocks back along the heading, flying at 1.5x the heading
        double back = GeneralConfig.enableBomberShortMode ? 100 : 200;
        double dist = Math.hypot(bomber.getX() - target.getX(), bomber.getZ() - target.getZ());
        if (Math.abs(dist - back) > 1e-6) errors.add(what + ": jet starts " + dist + " from the target, 1.7 " + back);
        Vec3 m = bomber.getDeltaMovement();
        double speed = Math.hypot(m.x, m.z);
        if (Math.abs(speed - back / 100 * 1.5) > 1e-6) errors.add(what + ": jet speed " + speed + ", 1.7 " + (back / 100 * 1.5));
        // heading points at the target
        double ex = target.getX() - bomber.getX(), ez = target.getZ() - bomber.getZ();
        if (Math.abs(ex * m.z - ez * m.x) > 1e-6 || ex * m.x + ez * m.z <= 0) errors.add(what + ": jet does not head for the target");
    }
}
