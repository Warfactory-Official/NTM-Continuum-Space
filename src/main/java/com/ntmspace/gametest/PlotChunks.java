package com.ntmspace.gametest;

import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * Holds chunks around a GameTest plot entity-ticking while a test waits on an entity's own ticks (a rocket flying,
 * landing, docking on its first tick), through Golem Core's {@link GameTests#holdChunks}. Every test's own plot (padding,
 * at least 16 blocks) is already held by Golem Core before the test starts, so the radius-16 holds below only restate
 * what a test needs; the flight corridor is the one that reaches past it.
 *
 * <p>These used to be forced chunks, released by the test that forced them. A chunk forced by one test and then asked
 * for by another (whose {@code setChunkForced} returned false, so it did not own it) was unforced under the second test
 * when the first finished: the satellite rocket's 840-block corridor runs through the landing test's plot, and its far
 * chunk then never became entity-ticking ("flight corridor chunk ... is not entity-ticking yet on tick 1202"). Golem's
 * holds are counted per chunk.
 */
final class PlotChunks {

    private PlotChunks() {}

    /** Holds the chunks within {@code radius} blocks of the plot's origin; the returned action releases the hold. */
    static Runnable force(GameTestHelper helper, int radius) {
        return force(helper, -radius, radius, -radius, radius);
    }

    /**
     * Holds the chunks under the absolute box {@code origin + (minX..maxX, minZ..maxZ)} (blocks, relative to the plot's
     * origin); the returned action releases the hold. For an entity that travels: its own ticket only asks for the next
     * chunk as it reaches the border, and while the server is busy generating chunks for the rest of the batch the next
     * chunk is not entity-ticking yet, so the entity freezes there until it is.
     */
    static Runnable force(GameTestHelper helper, int minX, int maxX, int minZ, int maxZ) {
        return GameTests.holdChunks(helper, minX, maxX, minZ, maxZ);
    }

    /**
     * The first chunk under {@code origin + (minX..maxX, minZ..maxZ)} that is not entity-ticking yet, as "cx, cz", or
     * null once every one is.
     */
    static String notTicking(GameTestHelper helper, int minX, int maxX, int minZ, int maxZ) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int cx = (origin.getX() + minX) >> 4; cx <= (origin.getX() + maxX) >> 4; cx++) {
            for (int cz = (origin.getZ() + minZ) >> 4; cz <= (origin.getZ() + maxZ) >> 4; cz++) {
                if (!level.isPositionEntityTicking(new BlockPos(cx << 4, origin.getY(), cz << 4))) return cx + ", " + cz;
            }
        }
        return null;
    }
}
