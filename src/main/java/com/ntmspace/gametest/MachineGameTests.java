package com.ntmspace.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.AlkylationRecipes;
import com.ntmspace.recipe.CryoRecipes;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.ntmspace.tileentity.machine.TileEntityMachineMilkReformer;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTests of the progression machines (part one): the cryogenic distiller, alkylation unit and milk reformer. The
 * expected tables are hbm-space's {@code CryoRecipes} and {@code AlkylationRecipes} {@code registerDefaults}, copied
 * here verbatim in source order; the datapack rows must equal them. Each machine runs one 1.7 step with 1.7's power and
 * amounts.
 */
public final class MachineGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function CRYO_TABLE = TESTS.function("cryo_table_matches_17", 20,
            () -> MachineGameTests::cryoTable);
    public static final GameTests.Function ALKYLATION_TABLE = TESTS.function("alkylation_table_matches_17", 20,
            () -> MachineGameTests::alkylationTable);
    public static final GameTests.Function CRYO_DISTILL = TESTS.function("cryo_distill_step", 40, 8,
            () -> MachineGameTests::cryoDistill);
    public static final GameTests.Function ALKYLATION = TESTS.function("alkylation_step", 40, 8,
            () -> MachineGameTests::alkylation);
    public static final GameTests.Function MILK_REFORMER = TESTS.function("milk_reformer_step", 40, 8,
            () -> MachineGameTests::milkReformer);

    /** The rows hbm-space added to HBM's shredder, centrifuge, crystallizer, mixer and furnace for the body ores. */
    public static final GameTests.Function HBM_ROWS = TESTS.function("space_rows_in_hbm_tables", 20,
            () -> MachineGameTests::hbmRows);

    private MachineGameTests() {}

    public static void init() {}

    // ---------------------------------------------------------------------------------------------- tables

    private static FluidType f(String name) {
        FluidType type = Fluids.fromName(name);
        if (type == null || type == Fluids.NONE) throw new IllegalStateException("no fluid " + name);
        return type;
    }

    private static FluidStack s(String name, int amount) {
        return new FluidStack(f(name), amount);
    }

    /** hbm-space {@code CryoRecipes.registerDefaults}. */
    static Map<FluidType, FluidStack[]> cryo17() {
        Map<FluidType, FluidStack[]> m = new LinkedHashMap<>();
        m.put(f("EARTHAIR"), new FluidStack[] {s("NITROGEN", 60), s("OXYGEN", 25), s("KRYPTON", 10), s("CARBONDIOXIDE", 5)});
        m.put(f("TEKTOAIR"), new FluidStack[] {s("CHLOROMETHANE", 55), s("CHLORINE", 25), s("GAS", 15), s("HCL", 5)});
        m.put(f("JOOLGAS"), new FluidStack[] {s("HELIUM3", 40), s("HYDROGEN", 30), s("CHLORINE", 20), s("NEON", 10)});
        m.put(f("NGAS"), new FluidStack[] {s("GAS", 45), s("AMMONIA", 25), s("UNSATURATEDS", 15), s("XENON", 15)});
        m.put(f("UGAS"), new FluidStack[] {s("OXYGEN", 45), s("NITROGEN", 25), s("METHANOL", 15), s("AROMATICS", 15)});
        m.put(f("SARNUSGAS"), new FluidStack[] {s("HYDROGEN", 45), s("HELIUM3", 35), s("GAS", 15), s("NITROGEN", 5)});
        m.put(f("EVEAIR"), new FluidStack[] {s("KMnO4", 40), s("MERCURY", 35), s("XENON", 15), s("AMMONIA", 10)});
        m.put(f("DUNAAIR"), new FluidStack[] {s("CARBONDIOXIDE", 70), s("NITROGEN", 15), s("ARGON", 10), s("OXYGEN", 5)});
        return m;
    }

    /** hbm-space {@code AlkylationRecipes.registerDefaults}: X (secondary input), Y, Z. */
    static Map<FluidType, FluidStack[]> alkylation17() {
        Map<FluidType, FluidStack[]> m = new LinkedHashMap<>();
        m.put(f("CHLOROMETHANE"), new FluidStack[] {new FluidStack(Fluids.NONE, 0), s("UNSATURATEDS", 75), s("CHLORINE", 25)});
        m.put(f("UNSATURATEDS"), new FluidStack[] {new FluidStack(Fluids.NONE, 0), s("AROMATICS", 70), s("PETROLEUM", 30)});
        m.put(f("AROMATICS"), new FluidStack[] {s("RADIOSOLVENT", 40), s("XYLENE", 100), s("CHLORINE", 10)});
        m.put(f("PETROLEUM"), new FluidStack[] {s("SULFURIC_ACID", 50), s("LIGHTOIL", 50), s("PEROXIDE", 20)});
        m.put(f("XYLENE"), new FluidStack[] {s("CCL", 60), s("REFORMGAS", 100), s("CHLOROMETHANE", 50)});
        m.put(f("CHLOROETHANE"), new FluidStack[] {s("UNSATURATEDS", 60), s("CBENZ", 70), s("CHLOROMETHANE", 10)});
        return m;
    }

    private static boolean same(FluidStack a, FluidStack b) {
        return a.type == b.type && a.fill == b.fill;
    }

    private static String str(FluidStack s) {
        return s.type.getRegistryName() + " x" + s.fill;
    }

    private static void cryoTable(GameTestHelper helper) {
        CryoRecipes table = CryoRecipes.INSTANCE;
        table.registerRecipes();
        Map<FluidType, FluidStack[]> expected = cryo17();
        List<String> errors = new ArrayList<>();
        if (table.recipeOrderedList.size() != expected.size()) {
            errors.add(table.recipeOrderedList.size() + " rows, 1.7 has " + expected.size());
        }
        int index = 0;
        for (Map.Entry<FluidType, FluidStack[]> row : expected.entrySet()) {
            GenericRecipe recipe = index < table.recipeOrderedList.size() ? table.recipeOrderedList.get(index) : null;
            index++;
            CryoRecipes.Output out = table.getOutput(row.getKey());
            if (recipe == null || out == null) {
                errors.add("missing " + row.getKey().getRegistryName());
                continue;
            }
            if (recipe.inputFluid[0].type != row.getKey()) errors.add("order: row " + index + " is " + recipe.getInternalName());
            if (out.input() != 100 || out.power() != 20_000 || recipe.duration != 1) {
                errors.add(row.getKey().getRegistryName() + ": " + out.input() + " mB, " + out.power() + " HE, " + recipe.duration + " t");
            }
            for (int i = 0; i < 4; i++) {
                if (!same(out.fractions()[i], row.getValue()[i])) {
                    errors.add(row.getKey().getRegistryName() + " fraction " + i + ": " + str(out.fractions()[i]) + " != " + str(row.getValue()[i]));
                }
            }
        }
        finish(helper, errors);
    }

    private static void alkylationTable(GameTestHelper helper) {
        AlkylationRecipes table = AlkylationRecipes.INSTANCE;
        table.registerRecipes();
        Map<FluidType, FluidStack[]> expected = alkylation17();
        List<String> errors = new ArrayList<>();
        // rows of another installed addon (the dev probe adds one) are its own: count NTM: Space's 1.7 rows only
        long own = table.recipeOrderedList.stream().filter(r -> r.inputFluid != null && r.inputFluid.length > 0
                && expected.containsKey(r.inputFluid[0].type)).count();
        if (own != expected.size()) {
            errors.add(own + " rows, 1.7 has " + expected.size());
        }
        for (Map.Entry<FluidType, FluidStack[]> row : expected.entrySet()) {
            AlkylationRecipes.Output out = table.getOutput(row.getKey());
            if (out == null) {
                errors.add("missing " + row.getKey().getRegistryName());
                continue;
            }
            FluidStack[] want = row.getValue();
            if (out.input() != 100 || out.power() != 4_000) errors.add(row.getKey().getRegistryName() + ": " + out.input() + " mB, " + out.power() + " HE");
            if (!same(out.secondary(), want[0])) errors.add(row.getKey().getRegistryName() + " secondary " + str(out.secondary()) + " != " + str(want[0]));
            if (!same(out.out1(), want[1])) errors.add(row.getKey().getRegistryName() + " out1 " + str(out.out1()) + " != " + str(want[1]));
            if (!same(out.out2(), want[2])) errors.add(row.getKey().getRegistryName() + " out2 " + str(out.out2()) + " != " + str(want[2]));
        }
        finish(helper, errors);
    }

    // ---------------------------------------------------------------------------------------------- machines

    /** One distillation step: 100 mB Earth air and 20,000 HE become 60/25/10/5 mB; the input cell takes air from outside. */
    private static void cryoDistill(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 3));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachines.MACHINE_CRYO_DISTILL.get(), Direction.NORTH);
        TileEntityMachineCryoDistill te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineCryoDistill.class);
        List<String> errors = new ArrayList<>();

        // structure: the side input cell belongs to the core and takes Earth air through its outward face
        ForgeDirection dir = ForgeDirection.NORTH, rot = dir.getRotation(ForgeDirection.UP);
        BlockPos inputCell = core.offset(-dir.offsetX - 2 * rot.offsetX, -2, -dir.offsetZ - 2 * rot.offsetZ);
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), inputCell))) errors.add("input cell not part of the machine");
        else if (!SpaceMachines.MACHINE_CRYO_DISTILL.get().canConnect(SpaceFluids.EARTHAIR, helper.getLevel(),
                inputCell.getX(), inputCell.getY(), inputCell.getZ(), rot.getOpposite())) {
            errors.add("input cell does not take fluid from the side");
        }

        te.tanks[0].setFill(250);
        te.power = 20_000;
        TileEntityMachineCryoDistill.serverTick(helper.getLevel(), core, helper.getLevel().getBlockState(core), te);
        expectTank(errors, "air", te.tanks[0].getFill(), 150);
        expectTank(errors, "nitrogen", te.tanks[1].getFill(), 60);
        expectTank(errors, "oxygen", te.tanks[2].getFill(), 25);
        expectTank(errors, "krypton", te.tanks[3].getFill(), 10);
        expectTank(errors, "co2", te.tanks[4].getFill(), 5);
        expectTank(errors, "power", (int) te.power, 0);
        // without power nothing happens
        TileEntityMachineCryoDistill.serverTick(helper.getLevel(), core, helper.getLevel().getBlockState(core), te);
        expectTank(errors, "air after unpowered tick", te.tanks[0].getFill(), 150);
        finish(helper, errors);
    }

    /** One alkylation step on 1.7's even tick: petroleum + sulfuric acid -> light oil + peroxide, 4,000 HE. */
    private static void alkylation(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachines.MACHINE_ALKYLATION.get(), Direction.NORTH);
        TileEntityMachineAlkylation te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineAlkylation.class);
        List<String> errors = new ArrayList<>();
        te.tanks[0].setTankType(Fluids.PETROLEUM);
        te.tanks[0].setFill(100);
        te.alkylate(); // sets the tank types from the recipe
        te.tanks[1].setFill(50);
        te.power = 4_000;
        te.alkylate();
        expectTank(errors, "petroleum", te.tanks[0].getFill(), 0);
        expectTank(errors, "sulfuric acid", te.tanks[1].getFill(), 0);
        expectTank(errors, "light oil", te.tanks[2].getFill(), 50);
        expectTank(errors, "peroxide", te.tanks[3].getFill(), 20);
        expectTank(errors, "power", (int) te.power, 0);
        if (te.tanks[1].getTankType() != Fluids.SULFURIC_ACID || te.tanks[2].getTankType() != Fluids.LIGHTOIL
                || te.tanks[3].getTankType() != Fluids.PEROXIDE) {
            errors.add("tank types " + te.tanks[1].getTankType() + " " + te.tanks[2].getTankType() + " " + te.tanks[3].getTankType());
        }
        finish(helper, errors);
    }

    /** One step: 100 mB milk and 10,000 HE -> 50 enriched, 35 condensed, 15 cream. */
    private static void milkReformer(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(8, 1));
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, SpaceMachines.MACHINE_MILK_REFORMER.get(), Direction.NORTH);
        TileEntityMachineMilkReformer te = GameTestHelpers.blockEntity(helper, GameTests.relativePos(helper, core), TileEntityMachineMilkReformer.class);
        List<String> errors = new ArrayList<>();
        te.tanks[0].setFill(100);
        te.power = 10_000;
        TileEntityMachineMilkReformer.serverTick(helper.getLevel(), core, helper.getLevel().getBlockState(core), te);
        expectTank(errors, "milk", te.tanks[0].getFill(), 0);
        expectTank(errors, "enriched milk", te.tanks[1].getFill(), 50);
        expectTank(errors, "condensed milk", te.tanks[2].getFill(), 35);
        expectTank(errors, "cream", te.tanks[3].getFill(), 15);
        expectTank(errors, "power", (int) te.power, 0);
        if (!core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.offset(1, 6, 1)))) errors.add("top corner not part of the tower");
        finish(helper, errors);
    }

    private static net.minecraft.world.item.ItemStack stack(String id, int count) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("no item " + id);
        return new net.minecraft.world.item.ItemStack(item, count);
    }

    private static void expectStack(List<String> errors, String what, net.minecraft.world.item.ItemStack actual, String id, int count) {
        net.minecraft.world.item.ItemStack want = stack(id, count);
        if (actual == null || actual.isEmpty() || actual.getItem() != want.getItem() || actual.getCount() != count) {
            errors.add(what + ": " + (actual == null ? "nothing" : actual.getCount() + "x " + actual.getItem()) + ", 1.7 " + count + "x " + id);
        }
    }

    /**
     * hbm-space's {@code ShredderRecipes}, {@code CentrifugeRecipes}, {@code CrystallizerRecipes} (base time 600,
     * productivity 0.05), {@code MixerRecipes} (MINSOL) and {@code SmeltingRecipes} rows for the body ores, as the HBM
     * tables answer them (any body's form of an ore; 1.7's wildcard metadata).
     */
    private static void hbmRows(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        com.hbm.inventory.recipes.ShredderRecipes.register();
        expectStack(errors, "shredder glowstone ore (Duna)", com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(stack("ntmspace:ore_glowstone_duna", 1)), "minecraft:glowstone_dust", 4);
        expectStack(errors, "shredder quartz ore", com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(stack("ntmspace:ore_quartz", 1)), "hbm:powder_quartz", 2);
        expectStack(errors, "shredder phosphorus ore", com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(stack("ntmspace:ore_fire", 1)), "hbm:powder_fire", 6);
        expectStack(errors, "shredder mineral ore", com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(stack("ntmspace:ore_mineral", 1)), "ntmspace:mineral_dust", 1);
        expectStack(errors, "shredder nickel ore", com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(stack("ntmspace:ore_nickel", 1)), "ntmspace:chunk_ore_pentlandite", 2);

        net.minecraft.world.item.ItemStack[] centrifuge = com.hbm.inventory.recipes.CentrifugeRecipes.getOutput(stack("ntmspace:ore_nickel", 1));
        if (centrifuge == null || centrifuge.length != 4) {
            errors.add("centrifuge nickel ore: " + (centrifuge == null ? "no recipe" : centrifuge.length + " outputs"));
        } else {
            expectStack(errors, "centrifuge nickel 1", centrifuge[0], "ntmspace:chunk_ore_pentlandite", 2);
            expectStack(errors, "centrifuge nickel 2", centrifuge[1], "hbm:sulfur", 1);
            expectStack(errors, "centrifuge nickel 3", centrifuge[2], "hbm:powder_iron", 1);
            expectStack(errors, "centrifuge nickel 4", centrifuge[3], "minecraft:gravel", 1);
        }

        String[][] crystals = {
                {"ntmspace:ore_lithium", "SULFURIC_ACID", "hbm:crystal_lithium"},
                {"ntmspace:ore_zinc", "NITRIC_ACID", "ntmspace:crystal_zinc"},
                {"ntmspace:ore_niobium", "SULFURIC_ACID", "ntmspace:crystal_niobium"},
                {"ntmspace:ore_mineral", "PEROXIDE", "ntmspace:crystal_mineral"},
        };
        for (String[] row : crystals) {
            com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe recipe =
                    com.hbm.inventory.recipes.CrystallizerRecipes.getOutput(stack(row[0], 1), f(row[1]));
            if (recipe == null) {
                errors.add("crystallizer " + row[0] + " + " + row[1] + ": no recipe");
                continue;
            }
            expectStack(errors, "crystallizer " + row[0], recipe.output, row[2], 1);
            if (recipe.acidAmount != 500 || recipe.duration != 600 || recipe.productivity != 0.05F) {
                errors.add("crystallizer " + row[0] + ": " + recipe.acidAmount + " mB, " + recipe.duration + " t, " + recipe.productivity);
            }
        }

        com.hbm.inventory.recipes.MixerRecipes.MixerRecipe[] mixer = com.hbm.inventory.recipes.MixerRecipes.getOutput(SpaceFluids.MINSOL);
        if (mixer == null || mixer.length != 1) {
            errors.add("mixer MINSOL: " + (mixer == null ? "no recipe" : mixer.length + " recipes"));
        } else {
            com.hbm.inventory.recipes.MixerRecipes.MixerRecipe m = mixer[0];
            if (m.output != 500 || m.processTime != 50 || m.input1 == null || m.input1.type != Fluids.NITRIC_ACID || m.input1.fill != 500
                    || m.input2 != null || m.solidInput == null || !m.solidInput.matchesRecipe(stack("ntmspace:ore_mineral_duna", 1), true)) {
                errors.add("mixer MINSOL differs from 1.7 (500 mB from nitric acid 500 + mineral ore, 50 t)");
            }
        }

        expectStack(errors, "smelting zinc ore", dev.golem.core.world.Recipes.smeltingResult(helper.getLevel(), stack("ntmspace:ore_zinc", 1))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY), "ntmspace:ingot_zinc", 1);
        expectStack(errors, "smelting nickel ore", dev.golem.core.world.Recipes.smeltingResult(helper.getLevel(), stack("ntmspace:ore_nickel", 1))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY), "ntmspace:chunk_ore_pentlandite", 1);
        finish(helper, errors);
    }

    // ---------------------------------------------------------------------------------------------- helpers

    static void expectTank(List<String> errors, String what, int actual, int expected) {
        if (actual != expected) errors.add(what + ": " + actual + ", 1.7 " + expected);
    }

    static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", String.join("; ", errors));
            helper.fail(String.join("; ", errors));
        }
    }
}
