package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.block.machine.MachineHydroponic;
import com.ntmspace.recipe.VacuumCircuitRecipes;
import com.ntmspace.recipe.VacuumCircuitRecipes.VacuumCircuitRecipe;
import com.ntmspace.registry.SpaceMachineItems;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.tileentity.machine.HydroponicPlants;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of the progression machines, part B: the Vacuum Solderer's table against hbm-space's
 * {@code VacuumCircuitRecipes.registerDefaults} (copied here verbatim, in source order), one soldering run with 1.7's
 * duration and consumption, and the Industrial Greenhouse's structure, planting, CO2 to O2 conversion (15 mB per growth
 * stage), harvest, lamps and fertiliser intake.
 */
public final class MachineGameTestsB {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function VACUUM_CIRCUIT_TABLE = TESTS.function("vacuum_circuit_table_matches_17", 20,
            () -> MachineGameTestsB::vacuumCircuitTable);
    public static final GameTests.Function VACUUM_CIRCUIT = TESTS.function("vacuum_circuit_step", 40, 8,
            () -> MachineGameTestsB::vacuumCircuit);
    public static final GameTests.Function HYDROBAY = TESTS.function("hydrobay_step", 40, 8,
            () -> MachineGameTestsB::hydrobay);
    public static final GameTests.Function HYDROBAY_FERTILIZER = TESTS.function("hydrobay_fertilizer", 40, 8,
            () -> MachineGameTestsB::hydrobayFertilizer);

    private MachineGameTestsB() {}

    public static void init() {}

    // ---------------------------------------------------------------------------------------------- vacuum solderer

    private record Row(Item output, int duration, long consumption, AStack[] wafer, AStack[] pcb) {}

    private static AStack hbm(EnumCircuitType type, int count) {
        return new ComparableStack(ModIntermediateItems.circuit(type), count);
    }

    private static AStack space(String type, int count) {
        return new ComparableStack(SpaceMachineItems.circuit(type), count);
    }

    /** hbm-space {@code VacuumCircuitRecipes.registerDefaults}. */
    static List<Row> vacuum17() {
        List<Row> rows = new ArrayList<>();
        //t0 you can go to the mun when you got all the shit. :)
        rows.add(new Row(SpaceMachineItems.circuit("PROCESST1"), 200, 250,
                new AStack[] {hbm(EnumCircuitType.CHIP, 8)},
                new AStack[] {hbm(EnumCircuitType.PCB, 2), hbm(EnumCircuitType.CAPACITOR, 4)}));
        rows.add(new Row(SpaceMachineItems.circuit("PROCESST2"), 400, 1_000,
                new AStack[] {space("GASCHIP", 16)},
                new AStack[] {hbm(EnumCircuitType.BASIC, 4), hbm(EnumCircuitType.CAPACITOR, 32)}));
        rows.add(new Row(SpaceMachineItems.circuit("PROCESST3"), 800, 25_000,
                new AStack[] {space("GASCHIP", 48), space("HFCHIP", 16)},
                new AStack[] {hbm(EnumCircuitType.ADVANCED, 8), space("CAPACITOR_LANTHANIUM", 64)}));
        return rows;
    }

    private static String str(AStack stack) {
        ComparableStack c = (ComparableStack) stack;
        return BuiltInRegistries.ITEM.getKey(c.item) + " x" + c.stacksize;
    }

    private static void compareGroup(List<String> errors, String what, AStack[] actual, AStack[] expected) {
        if (actual.length != expected.length) {
            errors.add(what + ": " + actual.length + " ingredients, 1.7 " + expected.length);
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!(actual[i] instanceof ComparableStack a) || a.exactData() || a.item != ((ComparableStack) expected[i]).item
                    || a.stacksize != expected[i].stacksize) {
                errors.add(what + " " + i + ": " + (actual[i] instanceof ComparableStack ? str(actual[i]) : actual[i]) + " != " + str(expected[i]));
            }
        }
    }

    private static void vacuumCircuitTable(GameTestHelper helper) {
        VacuumCircuitRecipes table = VacuumCircuitRecipes.INSTANCE;
        table.registerRecipes();
        List<VacuumCircuitRecipe> rows = table.rows();
        List<Row> expected = vacuum17();
        List<String> errors = new ArrayList<>();
        if (rows.size() != expected.size()) errors.add(rows.size() + " rows, 1.7 has " + expected.size());
        for (int i = 0; i < Math.min(rows.size(), expected.size()); i++) {
            VacuumCircuitRecipe row = rows.get(i);
            Row want = expected.get(i);
            String name = row.getInternalName();
            if (row.output().getItem() != want.output() || row.output().getCount() != 1) {
                errors.add(name + " output " + BuiltInRegistries.ITEM.getKey(row.output().getItem()) + " x" + row.output().getCount());
            }
            if (row.duration != want.duration() || row.power != want.consumption()) {
                errors.add(name + ": " + row.duration + " t, " + row.power + " HE/t; 1.7 " + want.duration() + " t, " + want.consumption() + " HE/t");
            }
            compareGroup(errors, name + " wafer", row.wafer(), want.wafer());
            compareGroup(errors, name + " pcb", row.pcb(), want.pcb());
        }
        // 1.7 getRecipe: the wafer and board groups each match their own two slots
        ItemStack[] t1 = {new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8), ItemStack.EMPTY,
                new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 4),
                new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 2)};
        VacuumCircuitRecipe match = table.getRecipe(t1);
        if (match == null || match.output().getItem() != SpaceMachineItems.circuit("PROCESST1")) errors.add("PROCESST1 inputs do not match");
        ItemStack[] swapped = {t1[2], t1[3], t1[0], t1[1]};
        if (table.getRecipe(swapped) != null) errors.add("boards in the wafer slots matched a recipe");
        MachineGameTests.finish(helper, errors);
    }

    /**
     * One PROCESST1 run: 200 ticks at 250 HE each; the buffer becomes 20 ticks' worth (5,000 HE). The atmosphere gate
     * first: the test level's air decides whether the machine may run (1.7: none, or at most 0.001 atm).
     */
    private static void vacuumCircuit(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        ServerLevel level = helper.getLevel();
        MultiblockHandlerXR.placeAssembled(level, core, SpaceMachinesB.MACHINE_VACUUM_CIRCUIT.get(), Direction.NORTH);
        TileEntityMachineVacuumCircuit te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineVacuumCircuit.class);
        List<String> errors = new ArrayList<>();
        if (!core.equals(MultiblockSurface.coreOfCell(level, core.offset(1, 0, 1)))) errors.add("corner not part of the machine");

        te.setItem(0, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8));
        te.setItem(2, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 2));
        te.setItem(3, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 4));

        // the gate, through the real tick
        te.power = 250;
        TileEntityMachineVacuumCircuit.serverTick(level, core, level.getBlockState(core), te);
        boolean vacuum = TileEntityMachineVacuumCircuit.isNearVacuum(level, core);
        if (te.canOperate != vacuum) errors.add("canOperate " + te.canOperate + " in a level whose near-vacuum check says " + vacuum);
        if (!vacuum && te.progress != 0) errors.add("worked outside a vacuum: progress " + te.progress);

        // the run, as in a vacuum
        te.canOperate = true;
        te.progress = 0;
        for (int tick = 1; tick <= 200; tick++) {
            te.power = 250;
            te.work(level);
            if (tick == 1) {
                MachineGameTests.expectTank(errors, "processTime", te.processTime, 200);
                MachineGameTests.expectTank(errors, "consumption", (int) te.consumption, 250);
                MachineGameTests.expectTank(errors, "power after a tick", (int) te.power, 0);
                MachineGameTests.expectTank(errors, "buffer", (int) te.maxPower, 5_000);
            }
            if (tick == 199) {
                MachineGameTests.expectTank(errors, "progress at 199", te.progress, 199);
                if (!te.getItem(4).isEmpty()) errors.add("output before 200 ticks");
            }
        }
        ItemStack out = te.getItem(4);
        if (out.getItem() != SpaceMachineItems.circuit("PROCESST1") || out.getCount() != 1) errors.add("output " + out);
        MachineGameTests.expectTank(errors, "chips left", te.getItem(0).getCount(), 0);
        MachineGameTests.expectTank(errors, "boards left", te.getItem(2).getCount(), 0);
        MachineGameTests.expectTank(errors, "capacitors left", te.getItem(3).getCount(), 0);
        MachineGameTests.expectTank(errors, "progress after the run", te.progress, 0);

        // one HE short: no progress
        te.setItem(0, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CHIP), 8));
        te.setItem(2, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.PCB), 2));
        te.setItem(3, new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 4));
        te.power = 249;
        te.work(level);
        MachineGameTests.expectTank(errors, "progress without enough power", te.progress, 0);
        com.ntmspace.NtmSpace.LOG.info("vacuum_circuit_step: test level near vacuum {}, output {}", vacuum, out);
        MachineGameTests.finish(helper, errors);
    }

    // ---------------------------------------------------------------------------------------------- greenhouse

    private static int ages(ServerLevel level, BlockPos[] plants) {
        int sum = 0;
        for (BlockPos p : plants) sum += HydroponicPlants.stage(level.getBlockState(p));
        return sum;
    }

    private static void setAge(ServerLevel level, BlockPos pos, int age) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, HydroponicPlants.withStage(state, age), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /**
     * Structure, planting, conversion and harvest. The conversion is checked against the plants' real stages (a random
     * tick may grow one): CO2 down and O2 up by 15 mB per stage gained.
     */
    private static void hydrobay(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        ServerLevel level = helper.getLevel();
        MultiblockHandlerXR.placeAssembled(level, core, SpaceMachinesB.HYDROBAY.get(), Direction.NORTH);
        TileEntityHydroponic te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityHydroponic.class);
        List<String> errors = new ArrayList<>();

        ForgeDirection rot = ForgeDirection.getOrientation(Direction.NORTH).getRotation(ForgeDirection.UP);
        BlockPos[] plants = TileEntityHydroponic.plantPositions(core, Direction.NORTH);
        BlockPos[] lamps = {core.offset(-rot.offsetX, 2, -rot.offsetZ), core.offset(rot.offsetX, 2, rot.offsetZ)};
        for (BlockPos p : plants) if (!level.getBlockState(p).isAir()) errors.add("plant position " + p + " is not empty");
        for (BlockPos p : lamps) if (!core.equals(MultiblockSurface.coreOfCell(level, p))) errors.add("lamp cell " + p + " not part of the machine");
        for (BlockPos p : MachineHydroponic.extras(core, Direction.NORTH)) {
            if (!core.equals(MultiblockSurface.coreOfCell(level, p))) errors.add("proxy cell " + p + " not part of the machine");
        }
        if (!MachineHydroponic.sustainsPlant(level, plants[0].below())) errors.add("1.7 soil rule says no beside the core");
        // Golem's soil hook: wheat stands on the base next to the core (all three plant spots), not on the roof
        for (BlockPos p : plants) {
            BlockState soil = level.getBlockState(p.below());
            if (!(soil.getBlock() instanceof MachineHydroponic hydro) || hydro.sustainsPlant(level, p.below(), soil,
                    Blocks.WHEAT.defaultBlockState()) != dev.golem.core.hook.Verdict.ALLOW) errors.add("no soil for wheat at " + p);
            // the crop's own check (crops other than NeoForge's also want light 8 or the sky, which the lamps give later)
            boolean light = level.getRawBrightness(p, 0) >= 8 || level.canSeeSky(p);
            if (light && !Blocks.WHEAT.defaultBlockState().canSurvive(level, p)) errors.add("wheat cannot stand at " + p + " (soil hook)");
        }
        BlockPos roof = core.above(3);
        if (level.getBlockState(roof.below()).getBlock() instanceof MachineHydroponic
                && Blocks.WHEAT.defaultBlockState().canSurvive(level, roof)) errors.add("wheat may stand on the roof " + roof);

        te.tanks[0].setFill(1_000);
        te.power = 2_000;
        te.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 3));
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);

        MachineGameTests.expectTank(errors, "power after a tick", (int) te.power, 1_975);
        for (BlockPos p : lamps) {
            BlockState lamp = level.getBlockState(p);
            if (!lamp.getValue(MachineHydroponic.LIT) || lamp.getLightEmission() != 15) errors.add("lamp " + p + " not lit");
        }
        for (BlockPos p : plants) if (!(level.getBlockState(p).getBlock() instanceof CropBlock)) errors.add("no wheat at " + p);
        if (!te.getItem(0).isEmpty()) errors.add("seeds left: " + te.getItem(0));
        int stages = ages(level, plants);
        MachineGameTests.expectTank(errors, "CO2 after planting", te.tanks[0].getFill(), 1_000 - 15 * stages);
        MachineGameTests.expectTank(errors, "O2 after planting", te.tanks[1].getFill(), 15 * stages);

        // growth: the middle plant three stages on
        setAge(level, plants[1], HydroponicPlants.stage(level.getBlockState(plants[1])) + 3);
        te.power = 2_000;
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        stages = ages(level, plants);
        if (stages < 3) errors.add("growth lost: stages " + stages);
        MachineGameTests.expectTank(errors, "CO2 after growth", te.tanks[0].getFill(), 1_000 - 15 * stages);
        MachineGameTests.expectTank(errors, "O2 after growth", te.tanks[1].getFill(), 15 * stages);

        // harvest: the first plant ripe
        setAge(level, plants[0], 7);
        int others = HydroponicPlants.stage(level.getBlockState(plants[1])) + HydroponicPlants.stage(level.getBlockState(plants[2]));
        te.power = 2_000;
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        if (!level.getBlockState(plants[0]).isAir()) errors.add("ripe wheat not harvested: " + level.getBlockState(plants[0]));
        int wheat = 0;
        for (int i = 3; i <= 5; i++) if (te.getItem(i).is(Items.WHEAT)) wheat += te.getItem(i).getCount();
        if (wheat < 1) errors.add("no wheat in the output");
        others = Math.max(others, HydroponicPlants.stage(level.getBlockState(plants[1])) + HydroponicPlants.stage(level.getBlockState(plants[2])));
        MachineGameTests.expectTank(errors, "O2 after harvest", te.tanks[1].getFill(), 15 * (7 + others));

        // unpowered: lamps off, a plant held at its stage
        int held = HydroponicPlants.stage(level.getBlockState(plants[1]));
        setAge(level, plants[1], held + 1);
        te.power = 150;
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        MachineGameTests.expectTank(errors, "power below the lamp threshold", (int) te.power, 125);
        for (BlockPos p : lamps) if (level.getBlockState(p).getValue(MachineHydroponic.LIT)) errors.add("lamp " + p + " still lit");
        MachineGameTests.expectTank(errors, "held stage", HydroponicPlants.stage(level.getBlockState(plants[1])), held);
        if (!level.getBlockState(lamps[0]).getValue(BlockMultiblockMachine.CELL)) errors.add("lamp no longer a cell");
        com.ntmspace.NtmSpace.LOG.info("hydrobay_step: CO2 {} mB, O2 {} mB, harvested wheat {}", te.tanks[0].getFill(), te.tanks[1].getFill(), wheat);
        MachineGameTests.finish(helper, errors);
    }

    /** Fertiliser intake: bone meal 1, fertiliser powder 9, up to 200; nothing else runs without power. */
    private static void hydrobayFertilizer(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        ServerLevel level = helper.getLevel();
        MultiblockHandlerXR.placeAssembled(level, core, SpaceMachinesB.HYDROBAY.get(), Direction.EAST);
        TileEntityHydroponic te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityHydroponic.class);
        List<String> errors = new ArrayList<>();
        te.setItem(1, new ItemStack(Items.BONE_MEAL, 2));
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        MachineGameTests.expectTank(errors, "fertilizer from bone meal", te.fertilizer, 1);
        MachineGameTests.expectTank(errors, "bone meal left", te.getItem(1).getCount(), 1);
        te.fertilizer = 192;
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        MachineGameTests.expectTank(errors, "fertilizer at 192 + 1", te.fertilizer, 193);
        te.fertilizer = 200;
        te.setItem(1, new ItemStack(Items.BONE_MEAL, 1));
        TileEntityHydroponic.serverTick(level, core, level.getBlockState(core), te);
        MachineGameTests.expectTank(errors, "fertilizer full", te.fertilizer, 200);
        MachineGameTests.expectTank(errors, "bone meal kept when full", te.getItem(1).getCount(), 1);
        for (BlockPos p : TileEntityHydroponic.plantPositions(core, Direction.EAST)) {
            if (!level.getBlockState(p).is(Blocks.AIR)) errors.add("something planted without power at " + p);
        }
        MachineGameTests.finish(helper, errors);
    }
}
