package com.ntmspace.gametest;

import com.hbm.gametest.ForeignEnergySweep;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/**
 * Other mods' power cables reach every NTM: Space energy machine, and the HBM machines Space substitutes, where a cable
 * can: HBM's sweep builds each in turn and asks the foreign lookup on its power ports, or on the faces HBM's cable
 * connects to when it has none.
 */
public final class ForeignEnergyGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;
    private static final int PADDING = 24;
    /** HBM machines whose block Space replaces under HBM's own id. */
    private static final String[] SUBSTITUTED = {"hbm:machine_chungus", "hbm:machine_lpw2", "hbm:machine_condenser_powered"};

    static {
        TESTS.function("space_foreign_energy_faces", 3000, PADDING, () -> ForeignEnergyGameTests::faces);
    }

    private ForeignEnergyGameTests() {}

    public static void init() {}

    private static void faces(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        List<Block> blocks = ForeignEnergySweep.energyBlocks(NtmSpace.MOD_ID);
        for (String id : SUBSTITUTED) BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).ifPresent(blocks::add);
        ForeignEnergySweep.run(helper, blocks, PADDING, result -> {
            helper.assertTrue(result.checked().size() >= 15, "only " + result.checked().size()
                    + " energy machines were found: " + result.checked());
            helper.assertTrue(result.missing().isEmpty(), result.missing().size()
                    + " cable faces of Space machines expose no foreign energy: " + result.missing());
            helper.succeed();
        });
    }
}
