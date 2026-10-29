package com.ntmspace.gametest;

import com.hbm.api.environment.Environment;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.gametest.ForeignEnergyFixture;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.block.machine.MachineSolar;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.inventory.material.SpaceMaterials;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.tileentity.machine.TileEntityMachineGasDock;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.tileentity.machine.TileEntityMachineSolarPanel;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of the progression machines, part C: magma drill, solar panel, steam radiator, gas giant harvester and
 * troll_disturber, each running 1.7 steps with 1.7's numbers.
 */
public final class MachineGameTestsC {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function MAGMA = TESTS.function("magma_drill_step", 40, 8,
            () -> MachineGameTestsC::magma);
    public static final GameTests.Function SOLAR = TESTS.function("solar_panel_output", 40, 8,
            () -> MachineGameTestsC::solar);
    public static final GameTests.Function SOLAR_FOREIGN = TESTS.function("solar_foreign_live_connections", 40, 8,
            () -> MachineGameTestsC::solarForeign);
    public static final GameTests.Function RADIATOR = TESTS.function("radiator_step", 40, 8,
            () -> MachineGameTestsC::radiator);
    public static final GameTests.Function GAS_DOCK = TESTS.function("gas_dock_step", 40, 8,
            () -> MachineGameTestsC::gasDock);
    public static final GameTests.Function TROLL = TESTS.function("troll_disturber_jams", 60, 0,
            () -> MachineGameTestsC::troll);

    private MachineGameTestsC() {}

    public static void init() {}

    /**
     * The drill: on Kerbin it does nothing; on Moho over a 3x3 of lava it draws 10,000 HE a tick and on a tick that is a
     * multiple of 10 adds 72 quanta slag + 4 quanta mineral-rich magma; with no lava it refuses and says so; a full
     * buffer (16 blocks) takes nothing more.
     */
    private static void magma(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 5));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesC.MACHINE_MAGMA.get(), Direction.NORTH);
        TileEntityMachineMagma te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineMagma.class);
        BlockState state = helper.getLevel().getBlockState(core);
        List<String> errors = new ArrayList<>();
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.offset(3, 3, 3)))) errors.add("corner not part of the drill");

        te.power = 1_000_000;
        te.step(helper.getLevel(), state, "kerbin", 0);
        if (te.operating || te.power != 1_000_000 || !te.liquids.isEmpty()) errors.add("ran on Kerbin");

        te.step(helper.getLevel(), state, "moho", 0);
        if (te.operating || te.validPosition) errors.add("ran without lava (valid " + te.validPosition + ")");

        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) helper.getLevel().setBlock(core.offset(x, -4, z), Blocks.LAVA.defaultBlockState(), 2);
        te.step(helper.getLevel(), state, "moho", 0);
        if (!te.operating || !te.validPosition) errors.add("did not run on Moho over lava");
        MachineGameTests.expectTank(errors, "power", (int) te.power, 990_000);
        MachineGameTests.expectTank(errors, "slag", quanta(te, Mats.MAT_SLAG), 72);
        MachineGameTests.expectTank(errors, "rich magma", quanta(te, SpaceMaterials.RICH_MAGMA), 4);

        te.step(helper.getLevel(), state, "moho", 5); // not a tenth tick: power only
        MachineGameTests.expectTank(errors, "power after tick 5", (int) te.power, 980_000);
        MachineGameTests.expectTank(errors, "slag after tick 5", quanta(te, Mats.MAT_SLAG), 72);

        te.liquids.clear();
        te.liquids.add(new Mats.MaterialStack(Mats.MAT_SLAG, TileEntityMachineMagma.maxLiquid - 2));
        te.step(helper.getLevel(), state, "moho", 10); // the slag does not fit: 1.7 breaks before the magma too
        MachineGameTests.expectTank(errors, "full slag", quanta(te, Mats.MAT_SLAG), TileEntityMachineMagma.maxLiquid - 2);
        MachineGameTests.expectTank(errors, "full rich magma", quanta(te, SpaceMaterials.RICH_MAGMA), 0);

        te.power = 9_999;
        te.step(helper.getLevel(), state, "moho", 20);
        if (te.operating) errors.add("ran below 10,000 HE");
        MachineGameTests.finish(helper, errors);
    }

    private static int quanta(TileEntityMachineMagma te, Object material) {
        int sum = 0;
        for (Mats.MaterialStack stack : te.liquids) if (stack.material == material) sum += stack.amount;
        return sum;
    }

    /**
     * 1.7 {@code getOutput}: {@code ceil(sun * 25 * sunPower)}, sun power from the planet's distance (1.7 constants:
     * Kerbin's orbit 13,599,840 km is 1 AU, Moho's 5,263,138 km, Duna's 20,726,155 km); and the panel in the test
     * world produces exactly that for the sky it sees.
     */
    private static void solar(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesC.MACHINE_SOLAR.get(), Direction.NORTH);
        TileEntityMachineSolarPanel te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineSolarPanel.class);

        float au = 13_599_840F;
        float moho = 5_263_138F / au, duna = 20_726_155F / au;
        for (int sun = 1; sun <= 4; sun++) {
            expect(errors, "kerbin sun " + sun, te.getOutput(sun, SolarSystem.body("kerbin").getSunPower()), Mth.ceil(sun * 25 * 1F));
            expect(errors, "moho sun " + sun, te.getOutput(sun, SolarSystem.body("moho").getSunPower()), Mth.ceil(sun * 25 * (1 / (moho * moho))));
            expect(errors, "duna sun " + sun, te.getOutput(sun, SolarSystem.body("ike").getSunPower()), Mth.ceil(sun * 25 * (1 / (duna * duna))));
        }
        expect(errors, "Earth noon", te.getOutput(4, 1F), 100);

        te.setPower(0);
        helper.runAfterDelay(1, () -> {
            int sun = helper.getLevel().getBrightness(LightLayer.SKY, core.above()) - helper.getLevel().getSkyDarken() - 11;
            boolean sky = helper.getLevel().canSeeSky(core.above());
            long want = sun <= 0 || !sky ? 0 : Math.min(1_000, te.getOutput(sun, (float) Environment.sunlight(helper.getLevel(), core)));
            expect(errors, "one world tick (sun " + sun + ", sky " + sky + ")", te.getPower(), want);
            MachineGameTests.finish(helper, errors);
        });
    }

    private static void solarForeign(GameTestHelper helper) {
        // Fabric reaches foreign energy only through Team Reborn Energy
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        var foreign = core.east();
        int east = 1 << Direction.EAST.ordinal();
        MultiblockHandlerXR.placeAssembled(level, core, SpaceMachinesC.MACHINE_SOLAR.get(), Direction.NORTH);
        level.setBlockAndUpdate(foreign, ForeignEnergyFixture.OPTIONAL_CELL.get().defaultBlockState());
        helper.assertTrue((MachineSolar.powerConnectionMask(level, core) & east) == 0,
                "An unexposed foreign block must not create a solar arm");
        try {
            ForeignEnergyFixture.receiver(level, foreign, 1_000, 1 << Direction.WEST.ordinal());
            var receiver = EnergyCompat.find(level, foreign, Direction.WEST);
            helper.assertTrue(receiver.insertable() == 1_000 && receiver.amount() == 0,
                    "Simulating insertion must preserve the foreign buffer");
            helper.assertTrue(receiver.insert(1_000) == 1_000 && receiver.amount() == 1_000
                            && receiver.insertable() == 0,
                    "A committed insertion must fill the native foreign buffer");
            helper.assertTrue((MachineSolar.powerConnectionMask(level, core) & east) != 0,
                    "A full foreign receiver must appear immediately in the live solar arms");
            helper.assertTrue(EnergyCompat.find(level, foreign, Direction.EAST) == null,
                    "The receiver's closed face must refuse the native capability");
            ForeignEnergyFixture.receiver(level, foreign, 1_000, 1 << Direction.EAST.ordinal());
            helper.assertTrue((MachineSolar.powerConnectionMask(level, core) & east) == 0,
                    "Turning the receiving face away must immediately remove the solar arm");
            ForeignEnergyFixture.receiver(level, foreign, 1_000, 1 << Direction.WEST.ordinal());
            helper.assertTrue((MachineSolar.powerConnectionMask(level, core) & east) != 0,
                    "Restoring the receiving face must immediately restore the solar arm");
            ForeignEnergyFixture.removeEnergy(level, foreign);
            helper.assertTrue((MachineSolar.powerConnectionMask(level, core) & east) == 0,
                    "Removing the foreign handler must immediately remove the solar arm");
        } finally {
            ForeignEnergyFixture.forget(level, foreign);
        }
        helper.succeed();
    }

    private static void expect(List<String> errors, String what, long actual, long expected) {
        if (actual != expected) errors.add(what + ": " + actual + ", 1.7 " + expected);
    }

    /**
     * One radiator tick: as much spent steam as the 500 mB water tank has room for becomes water (vacuum-optimised, so
     * also in a vacuum); the corner cells take steam from behind.
     */
    private static void radiator(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 2));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesC.MACHINE_RADIATOR.get(), Direction.NORTH);
        TileEntityRadiator te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityRadiator.class);
        BlockState state = helper.getLevel().getBlockState(core);
        List<String> errors = new ArrayList<>();

        ForgeDirection dir = ForgeDirection.NORTH, rot = dir.getRotation(ForgeDirection.UP);
        BlockPos corner = core.offset(rot.offsetX, 1, rot.offsetZ);
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), corner))) errors.add("corner not part of the radiator");
        else if (!SpaceMachinesC.MACHINE_RADIATOR.get().canConnect(Fluids.SPENTSTEAM, helper.getLevel(),
                corner.getX(), corner.getY(), corner.getZ(), dir.getOpposite())) {
            errors.add("corner does not take steam from behind");
        }
        if (te.input.getMaxFill() != 500 || te.output.getMaxFill() != 500) errors.add("tanks " + te.input.getMaxFill() + "/" + te.output.getMaxFill());
        if (!te.vacuumOptimised) errors.add("not vacuum-optimised");

        te.input.setFill(300);
        TileEntityRadiator.serverTick(helper.getLevel(), core, state, te);
        MachineGameTests.expectTank(errors, "steam", te.input.getFill(), 0);
        MachineGameTests.expectTank(errors, "water", te.output.getFill(), 300);

        te.input.setFill(500);
        TileEntityRadiator.serverTick(helper.getLevel(), core, state, te);
        MachineGameTests.expectTank(errors, "steam (water tank nearly full)", te.input.getFill(), 300);
        MachineGameTests.expectTank(errors, "water full", te.output.getFill(), 500);
        MachineGameTests.finish(helper, errors);
    }

    /**
     * The harvester: no gas on Kerbin; around Jool (Laythe) the fuelled rocket leaves on the twentieth tick, scooping
     * 8,000 mB Jool gas for 500 + 500 mB hydrogen and oxygen, and lands 101 ticks later.
     */
    private static void gasDock(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachinesC.GAS_DOCK.get(), Direction.NORTH);
        TileEntityMachineGasDock te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineGasDock.class);
        List<String> errors = new ArrayList<>();

        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.offset(1, 0, 1)))) errors.add("pad corner not part of the dock");
        if (TileEntityMachineGasDock.gasOf("laythe") != SpaceFluids.JOOLGAS || TileEntityMachineGasDock.gasOf("sarnus") != SpaceFluids.SARNUSGAS
                || TileEntityMachineGasDock.gasOf("kerbin") != null || TileEntityMachineGasDock.gasOf("mun") != null) {
            errors.add("planet gases " + TileEntityMachineGasDock.gasOf("laythe") + " " + TileEntityMachineGasDock.gasOf("kerbin"));
        }

        te.tanks[1].setFill(1_000);
        te.tanks[2].setFill(1_000);
        for (int i = 0; i < 30; i++) te.step(helper.getLevel(), "kerbin");
        MachineGameTests.expectTank(errors, "kerbin gas", te.tanks[0].getFill(), 0);
        MachineGameTests.expectTank(errors, "kerbin hydrogen", te.tanks[1].getFill(), 1_000);

        te.launchTicks = 0;
        te.hasRocket = true;
        for (int i = 0; i < 19; i++) te.step(helper.getLevel(), "laythe");
        if (!te.hasRocket || te.tanks[0].getFill() != 0) errors.add("left before the twentieth tick");
        te.step(helper.getLevel(), "laythe");
        if (te.hasRocket) errors.add("did not leave on the twentieth tick (" + te.launchTicks + ")");
        if (te.tanks[0].getTankType() != SpaceFluids.JOOLGAS) errors.add("gas type " + te.tanks[0].getTankType());
        MachineGameTests.expectTank(errors, "jool gas", te.tanks[0].getFill(), 8_000);
        MachineGameTests.expectTank(errors, "hydrogen", te.tanks[1].getFill(), 500);
        MachineGameTests.expectTank(errors, "oxygen", te.tanks[2].getFill(), 500);
        // launchTicks climbs 0, 1, ... from the next tick: it reaches 100, and the rocket lands, on the 101st
        for (int i = 0; i < 100; i++) te.step(helper.getLevel(), "laythe");
        if (te.hasRocket) errors.add("back before the 101st tick (" + te.launchTicks + ")");
        te.step(helper.getLevel(), "laythe");
        if (!te.hasRocket) errors.add("not back on the 101st tick (" + te.launchTicks + ")");
        MachineGameTests.expectTank(errors, "jool gas after landing", te.tanks[0].getFill(), 8_000);
        MachineGameTests.finish(helper, errors);
    }

    /** Placed, it takes 10,000,000 RAD out of its chunk; its ticks keep its {@code EntityNukeExplosionMK3.at} claim. */
    private static void troll(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockPos abs = helper.absolutePos(rel);
        ChunkRadiationManager.proxy.setRadiation(helper.getLevel(), abs, 5000);
        helper.setBlock(rel, SpaceMachinesC.TROLL_DISTURBER.get().defaultBlockState());
        EntityNukeExplosionMK3.ATEntry entry = new EntityNukeExplosionMK3.ATEntry(helper.getLevel().dimension(), abs.getX(), abs.getY(), abs.getZ());
        helper.succeedWhen(() -> {
            double rad = ChunkRadiationManager.proxy.getRadiation(helper.getLevel(), abs);
            helper.assertTrue(rad < 1, "radiation left: " + rad);
            Long until = EntityNukeExplosionMK3.at.get(entry);
            helper.assertTrue(until != null && until > helper.getLevel().getGameTime(), "no field-jammer claim: " + until);
        });
    }
}
