package com.ntmspace.war;

import com.hbm.entity.logic.EntityBomber;
import com.ntmspace.fork.ForkBombers;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.platform.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * The server half of hbm-space's {@code WorldProviderThatmo.updateWeather}: while Thatmo has its battlefield trait
 * ({@code CBT_BATTLEFIELD}), every 8 seconds a B2X carpet jet ({@link ForkBombers#carpetJet}, 1.7
 * {@code EntityBomber.statFacCarpetJet}) is sent over each player in Thatmo.
 *
 * <ul>
 *   <li>Target: the player's block position ({@code fauxpointtwelve.BlockPos}, floored) moved by
 *       {@code rand.nextInt(81) - 40} on x, then the same on z, with the level's random (1.7 {@code worldObj.rand}); y
 *       unchanged. The jet flies 20 blocks above it (1.7 {@code bomber.posY = y + 20} after {@code fac}).</li>
 *   <li>Timing: 1.7 tested {@code System.currentTimeMillis() % 8000 < 50} on every world tick, a 50 ms window every
 *       8 s, which at 20 ticks a second is one tick in 160. Here it is the level's game time, {@code time % 160 == 0}:
 *       the same rate, but deterministic (the wall-clock window hit no tick or two ticks when the server lagged or ran
 *       ahead). As in 1.7 the test sits inside the player loop, so one due tick sends one jet per player.</li>
 *   <li>Only Thatmo strikes: the code lived in Thatmo's world provider, so the trait on another body did nothing (the
 *       1.7 comment says as much). 1.7 ticked only a loaded Thatmo, which a Thatmo without players soon stopped being;
 *       with no players there is nothing to strike anyway.</li>
 *   <li>1.7 spawned with {@code WorldUtil.loadAndSpawnEntityInWorld} (loads the 5 x 5 chunks around the jet first); the
 *       HBM bomber's {@code fac} already handles its start, and the jet is added like the bomb caller's
 *       ({@code ItemBombCaller}).</li>
 * </ul>
 */
public final class ThatmoStrikes {

    /** 8,000 ms at 50 ms a tick. */
    public static final int INTERVAL = 160;
    /** 1.7 {@code radius}. */
    public static final int RADIUS = 40;
    /** 1.7 {@code bomber.posY = targetPos.getY() + 20}. */
    public static final int HEIGHT = 20;

    private ThatmoStrikes() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        Lifecycle.onServerTick(ThatmoStrikes::serverTick);
    }

    private static void serverTick(MinecraftServer server) {
        ServerLevel thatmo = server.getLevel(SpaceDimensions.THATMO);
        if (thatmo == null || thatmo.players().isEmpty()) return;
        for (Strike strike : strikes(thatmo, thatmo.players(), thatmo.getGameTime(), battlefield(thatmo))) {
            thatmo.addFreshEntity(strike.bomber());
        }
    }

    /** 1.7 {@code CelestialBody.getTrait(worldObj, CBT_BATTLEFIELD.class) != null}. */
    public static boolean battlefield(Level level) {
        return CelestialTraits.hasTrait(level, CelestialBodyTrait.CBT_Battlefield.class);
    }

    /** A jet and the block it was sent at (1.7 {@code targetPos}). */
    public record Strike(BlockPos target, EntityBomber bomber) {}

    /**
     * One tick of 1.7's server branch: the carpet jets to send at game time {@code time} (not yet added to the level).
     * Uses {@code level}'s random, one pair of draws per jet, in player order.
     */
    public static List<Strike> strikes(Level level, List<? extends Entity> players, long time, boolean battlefield) {
        List<Strike> out = new ArrayList<>();
        if (!battlefield) return out;
        for (Entity player : players) {
            if (time % INTERVAL != 0) continue;
            BlockPos target = target(level.getRandom(), player);
            out.add(new Strike(target, ForkBombers.carpetJet(level, target.getX(), target.getY(), target.getZ())));
        }
        return out;
    }

    /** 1.7 {@code targetPos}: the player's floored position, x then z moved by {@code rand.nextInt(81) - 40}. */
    public static BlockPos target(RandomSource rand, Entity player) {
        int offsetX = rand.nextInt(RADIUS * 2 + 1) - RADIUS;
        int offsetZ = rand.nextInt(RADIUS * 2 + 1) - RADIUS;
        return new BlockPos(Mth.floor(player.getX()) + offsetX, Mth.floor(player.getY()), Mth.floor(player.getZ()) + offsetZ);
    }
}
