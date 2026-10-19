package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.AtmosphereRecipes;
import com.ntmspace.registry.AtmosphereContent;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.tileentity.TileEntityAirPump;
import com.ntmspace.tileentity.atmosphere.TileEntityAirScrubber;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmoTower;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphereEditor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The atmosphere machines against hbm-space's numbers. The GameTest world is Kerbin, so the machines that change the
 * body's air change Kerbin's; each test gives back exactly what it took or added (a gas the air did not have is gone
 * again afterwards).
 *
 * <ul>
 *   <li>tower: 5,000 mB Earth air -> 100 mB nitrogen per 10,000 HE (hbm-space's 1/100 rule), three steps from 30,000 HE;</li>
 *   <li>compressor: 200 mB of the air's main gas per 20,000 HE, taken out of Kerbin's air (two steps from 40,000 HE);</li>
 *   <li>emitter: half the tank a tick (at least 1 mB) into the air for 10 HE per mB: 1,000 mB xenon and 10,000 HE end as
 *       0 mB and 0 HE in eleven ticks with exactly 1,000 mB of xenon in Kerbin's air;</li>
 *   <li>CO2 scrubber: in a sealed room with an oxygen vent, it claims the vent and stores its waste gas as CO2 at 10 HE per
 *       mB;</li>
 *   <li>editor: 10^2 mB of neon per 5 ticks while on, captured again in capture mode, saved by fluid name;</li>
 *   <li>atmospheric chemistry: 64 x (16,000 lithium hydroxide + 256,000 Duna air) -> 64 x 32,000 lithium carbonate;</li>
 *   <li>the four recipes that make the machines (three anvil constructions, one assembly row).</li>
 * </ul>
 */
public final class AtmosphereMachineGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;
    private static final double MB = CelestialTraits.MB_PER_ATM;

    public static final GameTests.Function TOWER = TESTS.function("atmo_tower_step", 60, 8,
            () -> AtmosphereMachineGameTests::tower);
    public static final GameTests.Function COMPRESSOR = TESTS.function("atmo_vent_compresses_air", 60, 8,
            () -> AtmosphereMachineGameTests::compressor);
    public static final GameTests.Function EMITTER = TESTS.function("atmo_emitter_releases_gas", 80, 8,
            () -> AtmosphereMachineGameTests::emitter);
    public static final GameTests.Function SCRUBBER = TESTS.function("air_scrubber_takes_co2", 400, 12,
            () -> AtmosphereMachineGameTests::scrubber);
    public static final GameTests.Function EDITOR = TESTS.function("atmosphere_editor_emits_captures", 80, 4,
            () -> AtmosphereMachineGameTests::editor);
    public static final GameTests.Function CHEMISTRY = TESTS.function("atmosphere_chemistry_17", 20,
            () -> AtmosphereMachineGameTests::chemistry);
    public static final GameTests.Function RECIPES = TESTS.function("atmosphere_machine_recipes", 20,
            () -> AtmosphereMachineGameTests::recipes);

    private AtmosphereMachineGameTests() {}

    public static void init() {}

    private static double pressure(Level level, FluidType fluid) {
        CBT_Atmosphere air = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        return air == null ? 0 : air.getPressure(fluid);
    }

    private static void tower(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, AtmosphereMachines.ATMO_TOWER.get(), Direction.NORTH);
        TileEntityAtmoTower te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityAtmoTower.class);
        List<String> errors = new ArrayList<>();
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.above(6)))) errors.add("the tower is not seven high");
        if (MultiblockSurface.coreOfCell(helper.getLevel(), core.above(7)) != null) errors.add("the tower is more than seven high");
        if (te.tanks[0].getTankType() != SpaceFluids.EARTHAIR || te.tanks[0].getMaxFill() != 50_000
                || te.tanks[1].getTankType() != SpaceFluids.NITROGEN || te.tanks[1].getMaxFill() != 5_000 || te.getMaxPower() != 1_000_000) {
            errors.add("tanks/power not 1.7's: " + te.tanks[0].getTankType() + " " + te.tanks[0].getMaxFill() + ", "
                    + te.tanks[1].getTankType() + " " + te.tanks[1].getMaxFill() + ", " + te.getMaxPower());
        }
        if (!errors.isEmpty()) {
            MachineGameTests.finish(helper, errors);
            return;
        }
        te.tanks[0].setFill(50_000);
        te.setPower(30_000);
        helper.succeedWhen(() -> {
            // 30,000 HE: three steps of 10,000 (hbm-space: needs max/100 and > 200 HE), then it stops
            if (te.getPower() != 0 || te.tanks[0].getFill() != 35_000 || te.tanks[1].getFill() != 300) {
                helper.fail("tower: power " + te.getPower() + ", earth air " + te.tanks[0].getFill() + ", nitrogen "
                        + te.tanks[1].getFill() + " (1.7: 0, 35000, 300)");
            }
        });
    }

    private static void compressor(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(level, core, AtmosphereMachines.ATMO_VENT.get(), Direction.NORTH);
        TileEntityAtmosphericCompressor te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityAtmosphericCompressor.class);
        if (!core.equals(MultiblockSurface.coreOfCell(level, core.above(3))) || MultiblockSurface.coreOfCell(level, core.above(4)) != null) {
            MachineGameTests.finish(helper, List.of("the compressor is not four high"));
            return;
        }
        CBT_Atmosphere air = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        if (air == null) {
            MachineGameTests.finish(helper, List.of("Kerbin has no air"));
            return;
        }
        FluidType gas = air.getMainFluid();
        te.tank.setTankType(gas);
        double before = pressure(level, gas);
        te.setPower(40_000);
        helper.succeedWhen(() -> {
            if (te.getPower() != 0 || te.tank.getFill() != 400 || te.tank.getTankType() != gas) {
                helper.fail("compressor: power " + te.getPower() + ", tank " + te.tank.getFill() + " " + te.tank.getTankType()
                        + " (1.7: two steps of 200 mB for 20,000 HE)");
            }
            double taken = (before - pressure(level, gas)) * MB;
            if (Math.abs(taken - 400) > 1e-3) helper.fail("compressor took " + taken + " mB of " + gas + " from Kerbin's air, 1.7 400");
            com.ntmspace.NtmSpace.LOG.info("atmo_vent_compresses_air: took {} mB of {} from Kerbin's air", taken, gas.getRegistryName());
            CelestialTraits.release(level, gas, 400); // give it back
        });
    }

    private static void emitter(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(level, core, AtmosphereMachines.ATMO_EMITTER.get(), Direction.NORTH);
        TileEntityAtmosphericEmitter te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityAtmosphericEmitter.class);
        if (!core.equals(MultiblockSurface.coreOfCell(level, core.above(2)))) {
            MachineGameTests.finish(helper, List.of("the emitter is not three high"));
            return;
        }
        FluidType gas = Fluids.XENON;
        double before = pressure(level, gas);
        te.tank.setTankType(gas);
        te.tank.setFill(1000);
        te.setPower(10_000);
        helper.succeedWhen(() -> {
            // 500, 250, 125, 62, 31, 16, 8, 4, 2, 1, 1 mB for 10 HE each: 1,000 mB and 10,000 HE, exactly
            if (te.tank.getFill() != 0 || te.getPower() != 0) {
                helper.fail("emitter: tank " + te.tank.getFill() + ", power " + te.getPower() + " (1.7: 0, 0)");
            }
            double added = (pressure(level, gas) - before) * MB;
            if (Math.abs(added - 1000) > 1e-3) helper.fail("emitter released " + added + " mB of xenon into Kerbin's air, 1.7 1000");
            com.ntmspace.NtmSpace.LOG.info("atmo_emitter_releases_gas: released {} mB xenon", added);
            CelestialTraits.capture(level, gas, 1000.001); // take it back (a hair more, so no rounding residue keeps a xenon entry)
        });
    }

    private static void scrubber(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(12, 4);
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            boolean wall = Math.abs(x) == 2 || Math.abs(y) == 2 || Math.abs(z) == 2;
            helper.setBlock(centre.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        helper.setBlock(centre, AtmosphereContent.AIR_VENT.get().defaultBlockState());
        TileEntityAirPump vent = GameTestHelpers.blockEntity(helper, centre, TileEntityAirPump.class);
        vent.tank.setFill(16000);
        BlockPos scrubberCore = helper.absolutePos(centre.offset(1, -1, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), scrubberCore, AtmosphereMachines.AIR_SCRUBBER.get(), Direction.NORTH);
        if (!(helper.getLevel().getBlockEntity(scrubberCore) instanceof TileEntityAirScrubber scrubber)) {
            MachineGameTests.finish(helper, List.of("no scrubber block entity at its core"));
            return;
        }
        if (!scrubberCore.equals(MultiblockSurface.coreOfCell(helper.getLevel(), scrubberCore.above()))) {
            MachineGameTests.finish(helper, List.of("the scrubber is not two high"));
            return;
        }
        scrubber.setPower(10_000);
        helper.succeedWhen(() -> {
            if (scrubber.pump() != vent) helper.fail("the scrubber did not claim the room's vent");
            int co2 = scrubber.tank.getFill();
            if (co2 < 5) helper.fail("the scrubber holds " + co2 + " mB of CO2 so far");
            if (scrubber.tank.getTankType() != Fluids.CARBONDIOXIDE) helper.fail("scrubber tank " + scrubber.tank.getTankType());
            if (scrubber.getPower() != 10_000 - 10L * co2) {
                helper.fail("scrubber power " + scrubber.getPower() + " for " + co2 + " mB, 1.7 " + (10_000 - 10L * co2));
            }
            com.ntmspace.NtmSpace.LOG.info("air_scrubber_takes_co2: {} mB CO2 from the vent's room, power {}", co2, scrubber.getPower());
        });
    }

    private static void editor(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, AtmosphereMachines.ATMOSPHERE_EDITOR.get().defaultBlockState());
        TileEntityAtmosphereEditor te = GameTestHelpers.blockEntity(helper, rel, TileEntityAtmosphereEditor.class);
        List<String> errors = new ArrayList<>();
        if (te.isOn || te.throughputFactor != 10 || te.fluid != SpaceFluids.EARTHAIR || !te.isEmitting) {
            errors.add("defaults not 1.7's (off, 10, Earth air, emitting)");
        }
        // saved by fluid name, never a number
        te.fluid = SpaceFluids.NEON;
        String nbt = dev.golem.core.registry.BlockEntities.saveWithoutMetadata(te, level.registryAccess()).toString();
        if (!nbt.contains("\"ntmspace:neon\"")) errors.add("editor save does not name its fluid: " + nbt);
        if (!errors.isEmpty()) {
            MachineGameTests.finish(helper, errors);
            return;
        }
        FluidType gas = SpaceFluids.NEON;
        double before = pressure(level, gas);
        te.throughputFactor = 2;
        te.isOn = true;
        // every 5 ticks while on: 100 mB; after 21 ticks capture mode, which takes it all back (Kerbin has no neon)
        helper.runAfterDelay(21, () -> {
            double added = (pressure(level, gas) - before) * MB;
            long steps = Math.round(added / 100);
            if (steps < 4 || Math.abs(added - steps * 100) > 1e-3) {
                te.isOn = false;
                helper.fail("editor added " + added + " mB of neon in 21 ticks, 1.7: 100 mB per 5 ticks");
                return;
            }
            com.ntmspace.NtmSpace.LOG.info("atmosphere_editor_emits_captures: {} mB neon after 21 ticks", added);
            te.isEmitting = false;
        });
        helper.runAfterDelay(61, () -> {
            te.isOn = false;
            double left = (pressure(level, gas) - before) * MB;
            if (Math.abs(left) > 1e-3) {
                helper.fail("capture mode left " + left + " mB of neon in Kerbin's air");
                return;
            }
            helper.succeed();
        });
    }

    private static void chemistry(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        List<AtmosphereRecipes.Reaction> reactions = AtmosphereRecipes.INSTANCE.reactions();
        if (reactions.size() != 1) errors.add(reactions.size() + " reactions, 1.7 has one");
        AtmosphereRecipes.Reaction r = reactions.isEmpty() ? null : reactions.get(0);
        if (r == null || r.inputs()[0].type != SpaceFluids.LITHYDRO || r.inputs()[0].fill != 16_000
                || r.inputs()[1].type != SpaceFluids.DUNAAIR || r.inputs()[1].fill != 256_000
                || r.output().type != SpaceFluids.LITHCARBONATE || r.output().fill != 32_000) {
            errors.add("the reaction is not 1.7's 16,000 LITHYDRO + 256,000 DUNAAIR -> 32,000 LITHCARBONATE");
            MachineGameTests.finish(helper, errors);
            return;
        }
        // on a test body: exactly 64 reactions' worth of lithium hydroxide, a little more Duna air
        String body = "ntmspace_gametest_chemistry";
        try {
            CelestialTraits.emitGas(body, SpaceFluids.LITHYDRO, 16_000D * 64);
            CelestialTraits.emitGas(body, SpaceFluids.DUNAAIR, 256_000D * 64 + 5);
            if (!CelestialTraits.reactAtmosphere(helper.getLevel(), body, r)) errors.add("did not react with 64x the inputs");
            CBT_Atmosphere air = CelestialTraits.getTrait(body, CBT_Atmosphere.class, false);
            if (air == null || air.hasFluid(SpaceFluids.LITHYDRO) || Math.abs(air.getPressure(SpaceFluids.DUNAAIR) * MB - 5) > 1e-3
                    || Math.abs(air.getPressure(SpaceFluids.LITHCARBONATE) * MB - 32_000D * 64) > 1e-3) {
                errors.add("after the reaction: " + (air == null ? "no air" : describe(air)));
            }
            if (CelestialTraits.reactAtmosphere(helper.getLevel(), body, r)) errors.add("reacted again without the inputs");
        } finally {
            CelestialTraits.clearTraits(body);
        }
        MachineGameTests.finish(helper, errors);
    }

    private static String describe(CBT_Atmosphere air) {
        StringBuilder out = new StringBuilder();
        for (CBT_Atmosphere.FluidEntry entry : air.fluids) out.append(entry.fluid.getRegistryName()).append('=').append(entry.pressure * MB).append(' ');
        return out.toString();
    }

    private static void recipes(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        construction(errors, "machine_atmo_tower", new int[] {6, 8, 4, 1});
        construction(errors, "machine_atmo_vent", new int[] {4, 4, 4, 1});
        construction(errors, "machine_atmo_emitter", new int[] {6, 8, 4, 1});
        AssemblyMachineRecipes table = AssemblyMachineRecipes.INSTANCE;
        table.registerRecipes();
        GenericRecipe row = table.getRecipe("ass.airscrubber");
        if (row == null) {
            errors.add("ass.airscrubber missing");
        } else {
            ItemStack out = row.outputItem != null && row.outputItem.length == 1 ? row.outputItem[0] : ItemStack.EMPTY;
            int[] counts = Arrays.stream(row.normalInputItems()).mapToInt(s -> s.stacksize).toArray();
            if (row.duration != 200 || row.power != 100 || !"ntmspace:air_scrubber".equals(id(out)) || out.getCount() != 1
                    || !Arrays.equals(counts, new int[] {6, 4, 12, 1, 1})) {
                errors.add("ass.airscrubber: " + row.duration + "/" + row.power + " -> " + id(out) + " inputs " + Arrays.toString(counts));
            }
        }
        MachineGameTests.finish(helper, errors);
    }

    /** hbm-space AnvilRecipes: tier 2, the machine out, the four input counts. */
    private static void construction(List<String> errors, String file, int[] counts) {
        String rid = "ntmspace:anvil_construction/" + file;
        AnvilRecipes.AnvilConstructionRecipe r = AnvilRecipes.constructionById(rid);
        if (r == null) {
            errors.add(rid + " missing");
            return;
        }
        int[] got = r.input.stream().mapToInt((AStack s) -> s.stacksize).toArray();
        ItemStack out = r.output.isEmpty() ? ItemStack.EMPTY : r.output.get(0).stack;
        if (r.tierLower != 2 || !Arrays.equals(got, counts) || !("ntmspace:" + file).equals(id(out)) || out.getCount() != 1) {
            errors.add(rid + ": tier " + r.tierLower + ", inputs " + Arrays.toString(got) + " -> " + id(out));
        }
    }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
