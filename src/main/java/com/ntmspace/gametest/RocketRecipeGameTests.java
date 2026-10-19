package com.ntmspace.gametest;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.ntmspace.NtmSpace;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * The rocket family's machine recipes, authored under {@code data/ntmspace/recipe/{assembly_machine,soldering}/} from
 * hbm-space's {@code AssemblyMachineRecipes} and {@code SolderingRecipes}. Expected values are 1.7's: name, duration,
 * power, output id and count, input count and the 528 blueprint pool. Outputs are compared by registry id so the test
 * does not depend on which class registers the item.
 */
public final class RocketRecipeGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** The assembly-machine rows load from the addon's datapack with hbm-space's values. */
    public static final GameTests.Function ASSEMBLY = TESTS.function("rocket_recipes_assembly", 20,
            () -> RocketRecipeGameTests::assembly);

    /** {@code hard_drive} solders from 2 chips, 16 PCBs and 4 fine red-copper wires, 200 ticks at 250 HE/t. */
    public static final GameTests.Function SOLDERING = TESTS.function("rocket_recipes_hard_drive", 20,
            () -> RocketRecipeGameTests::soldering);

    private RocketRecipeGameTests() {}

    public static void init() {}

    private static void assembly(GameTestHelper helper) {
        AssemblyMachineRecipes table = AssemblyMachineRecipes.INSTANCE;
        table.registerRecipes();
        List<String> errors = new ArrayList<>();
        // name, output id, output count, duration, power, item inputs, expensive inputs (-1: none), 528 pool (null: none)
        check(table, errors, "ass.rocketassembly", "ntmspace:machine_rocket_assembly", 1, 200, 100, 6, -1, "528.aluminium");
        check(table, errors, "ass.launchpadrocket", "ntmspace:launch_pad_rocket", 1, 400, 100, 7, -1, "528.aluminium");
        check(table, errors, "ass.driveprocessor", "ntmspace:machine_drive_processor", 1, 400, 100, 5, 4, "528.aluminium");
        check(table, errors, "ass.mp_thruster_20_methalox", "ntmspace:mp_thruster_20_methalox", 1, 400, 100, 5, -1, null);
        check(table, errors, "ass.mp_thruster_20_hydrogen_triple", "ntmspace:mp_thruster_20_hydrogen_triple", 1, 400, 100, 5, -1, null);
        check(table, errors, "ass.rp_fuselage_20_12", "ntmspace:rp_f_20_12", 1, 400, 100, 4, -1, null);
        check(table, errors, "ass.rp_fuselage_20_12_hydrazine", "ntmspace:mp_fuselage_20_hydrazine", 1, 400, 100, 4, -1, null);
        check(table, errors, "ass.rp_capsule_20", "ntmspace:rp_c_20", 1, 200, 100, 6, -1, null);
        check(table, errors, "ass.rp_legs_20", "ntmspace:rp_l_20", 1, 100, 100, 3, -1, null);

        GenericRecipe triple = table.getRecipe("ass.mp_thruster_20_hydrogen_triple");
        if (triple != null && triple.inputItem.length == 5) {
            // 1.7: seg_20 x1, steel pipe x1, tungsten ingot x16, steel plate x12, saturnite ingot x6
            int[] counts = Arrays.stream(triple.inputItem).mapToInt(stack -> stack.stacksize).toArray();
            if (!Arrays.equals(counts, new int[] {1, 1, 16, 12, 6})) {
                errors.add("ass.mp_thruster_20_hydrogen_triple input counts " + Arrays.toString(counts));
            }
        }
        if (!errors.isEmpty()) {
            fail(helper, String.join("; ", errors));
            return;
        }
        helper.succeed();
    }

    private static void check(AssemblyMachineRecipes table, List<String> errors, String name, String output, int count,
                              int duration, long power, int inputs, int expensive, String pool) {
        GenericRecipe recipe = table.getRecipe(name);
        if (recipe == null) {
            errors.add(name + " missing");
            return;
        }
        ItemStack out = recipe.outputItem == null || recipe.outputItem.length == 0 ? ItemStack.EMPTY : recipe.outputItem[0];
        String id = out.isEmpty() ? "empty" : String.valueOf(BuiltInRegistries.ITEM.getKey(out.getItem()));
        AStack[] ex = recipe.inputItemEx;
        boolean poolOk = pool == null ? recipe.blueprintPools528 == null || recipe.blueprintPools528.length == 0
                : recipe.blueprintPools528 != null && Arrays.asList(recipe.blueprintPools528).contains(pool);
        if (!id.equals(output) || out.getCount() != count || recipe.duration != duration || recipe.power != power
                || recipe.inputItem == null || recipe.inputItem.length != inputs
                || (expensive < 0 ? ex != null && ex.length > 0 : ex == null || ex.length != expensive) || !poolOk) {
            errors.add(name + " wrong: " + GenericRecipeCodec.describe(recipe));
        }
    }

    private static void soldering(GameTestHelper helper) {
        SolderingRecipes.SolderingRecipe drive = SolderingRecipes.recipes.stream()
                .filter(r -> "ntmspace:hard_drive".equals(String.valueOf(BuiltInRegistries.ITEM.getKey(r.output.getItem()))))
                .findFirst().orElse(null);
        if (drive == null) {
            fail(helper, "no soldering recipe makes ntmspace:hard_drive");
            return;
        }
        if (drive.duration != 200 || drive.consumption != 250 || drive.fluid != null
                || drive.toppings.length != 1 || drive.toppings[0].stacksize != 2
                || drive.pcb.length != 1 || drive.pcb[0].stacksize != 16
                || drive.solder.length != 1 || drive.solder[0].stacksize != 4) {
            fail(helper, "hard_drive soldering recipe is not 1.7's: " + drive.duration + " t, " + drive.consumption + " HE/t");
            return;
        }
        helper.succeed();
    }

    private static void fail(GameTestHelper helper, String message) {
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
