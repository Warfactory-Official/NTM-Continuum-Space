package com.ntmspace.gametest;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemStamp.StampType;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The metal group of the recipe-change family: hbm-space's changes to HBM's press, crucible, DiFurnace, blast furnace,
 * rotary furnace, shredder, anvil and material-distribution tables (generated from {@code src/recipes}),
 * checked row by row through each table's own lookup against the values in hbm-space's source.
 */
public final class RecipeChangeMetalGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function PRESS = TESTS.function("recipe_change_metal_press", 40,
            () -> RecipeChangeMetalGameTests::press);
    public static final GameTests.Function CRUCIBLE = TESTS.function("recipe_change_metal_crucible", 40,
            () -> RecipeChangeMetalGameTests::crucible);
    public static final GameTests.Function FURNACES = TESTS.function("recipe_change_metal_furnaces", 40,
            () -> RecipeChangeMetalGameTests::furnaces);
    public static final GameTests.Function SHREDDER = TESTS.function("recipe_change_metal_shredder", 40,
            () -> RecipeChangeMetalGameTests::shredder);
    public static final GameTests.Function ANVIL = TESTS.function("recipe_change_metal_anvil", 40,
            () -> RecipeChangeMetalGameTests::anvil);
    public static final GameTests.Function MATERIAL_DISTRIBUTION = TESTS.function("recipe_change_metal_material_distribution",
            40, () -> RecipeChangeMetalGameTests::materialDistribution);

    private RecipeChangeMetalGameTests() {}

    public static void init() {}

    // ---------------------------------------------------------------- press

    private static void press(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // makeRecipe(PLATE, NI.ingot(), plate_nickel) / (PLATE, STAINLESS.ingot(), plate_stainless); the wire loop for zinc
        pressRow(errors, StampType.PLATE, "ntmspace:ingot_nickel", "ntmspace:plate_nickel", 1);
        pressRow(errors, StampType.PLATE, "ntmspace:ingot_stainless", "ntmspace:plate_stainless", 1);
        pressRow(errors, StampType.WIRE, "ntmspace:ingot_zinc", "ntmspace:wire_fine_zinc", 8);
        // 1.7 order: titanium, nickel, aluminium / steel, stainless, lead
        List<String> outputs = new ArrayList<>();
        for (PressRecipes.PressRecipe r : PressRecipes.getRecipes()) outputs.add(id(r.output()));
        follows(errors, outputs, "hbm:plate_titanium", "ntmspace:plate_nickel");
        follows(errors, outputs, "hbm:plate_steel", "ntmspace:plate_stainless");
        follows(errors, outputs, "hbm:wire_fine_zirconium", "ntmspace:wire_fine_zinc");
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void pressRow(List<String> errors, StampType stamp, String input, String output, int count) {
        ItemStack in = stack(input, 1);
        for (PressRecipes.PressRecipe r : PressRecipes.getRecipes()) {
            if (r.type() == stamp && r.input().matchesRecipe(in, true)) {
                if (!id(r.output()).equals(output) || r.output().getCount() != count) {
                    errors.add("press " + stamp + " " + input + " gives " + r.output().getCount() + " " + id(r.output()));
                }
                return;
            }
        }
        errors.add("press " + stamp + " " + input + ": no recipe");
    }

    // ---------------------------------------------------------------- crucible

    private static void crucible(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        CrucibleRecipes.register();
        // n = nugget (8), i = ingot (72)
        crucibleRow(errors, "crucible.hsss", 12, "hbm:ingot_dura_steel",
                new Object[] {"stainless", 40, "tungsten", 24, "cobalt", 8}, new Object[] {"dura_steel", 144});
        crucibleRow(errors, "crucible.arse", 9, "ntmspace:ingot_gaas",
                new Object[] {"gallium", 48, "arsenic", 24}, new Object[] {"gaas", 72});
        crucibleRow(errors, "crucible.stainless", 2, "ntmspace:ingot_stainless",
                new Object[] {"steel", 8, "nickel", 8}, new Object[] {"stainless", 16});
        List<String> names = new ArrayList<>();
        for (GenericRecipe r : CrucibleRecipes.INSTANCE.recipeOrderedList) names.add(r.getInternalName());
        follows(errors, names, "crucible.hss", "crucible.hsss");
        follows(errors, names, "crucible.bscco", "crucible.arse");
        follows(errors, names, "crucible.arse", "crucible.stainless");
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void crucibleRow(List<String> errors, String name, int frequency, String icon, Object[] in, Object[] out) {
        GenericRecipe found = CrucibleRecipes.INSTANCE.recipeNameMap.get(name);
        if (!(found instanceof CrucibleRecipes.CrucibleRecipe r)) {
            errors.add(name + " missing");
            return;
        }
        String got = describe(r.input) + " -> " + describe(r.output);
        String want = describe(in) + " -> " + describe(out);
        if (r.frequency != frequency || !got.equals(want) || !id(r.getIcon()).equals(icon)) {
            errors.add(name + " is " + r.frequency + " " + id(r.getIcon()) + " " + got + ", want " + frequency + " " + icon + " " + want);
        }
    }

    private static String describe(Mats.MaterialStack[] stacks) {
        StringBuilder b = new StringBuilder();
        for (Mats.MaterialStack s : stacks) b.append(s.material.name).append(' ').append(s.amount).append(", ");
        return b.toString();
    }

    private static String describe(Object[] pairs) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) b.append(pairs[i]).append(' ').append(pairs[i + 1]).append(", ");
        return b.toString();
    }

    // ---------------------------------------------------------------- DiFurnace, blast furnace, rotary furnace

    private static void furnaces(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // BlastFurnaceRecipes.addRecipe(NI, STEEL, ingot_stainless x2): nickel in any shape, either slot
        for (String[] pair : new String[][] {{"ntmspace:ingot_nickel", "hbm:ingot_steel"}, {"hbm:ingot_steel", "ntmspace:powder_nickel"},
                {"ntmspace:plate_nickel", "hbm:plate_steel"}}) {
            DiFurnaceRecipes.Match m = DiFurnaceRecipes.INSTANCE.getMatch(stack(pair[0], 1), stack(pair[1], 1));
            if (m == null || !"difurnace.stainless".equals(m.recipe().getInternalName())
                    || !id(m.recipe().outputItem[0]).equals("ntmspace:ingot_stainless") || m.recipe().outputItem[0].getCount() != 2) {
                errors.add("difurnace " + pair[0] + " + " + pair[1] + " -> " + (m == null ? "none" : m.recipe().getInternalName()));
            }
        }
        // BlastFurnaceRecipesNT blast.stainless: 400 ticks, STEEL ingot + NI ingot -> 2 stainless
        GenericRecipe blast = BlastFurnaceRecipesNT.INSTANCE.getRecipe(stack("ntmspace:ingot_nickel", 1), stack("hbm:ingot_steel", 1));
        if (blast == null || !"blast.stainless".equals(blast.getInternalName()) || blast.duration != 400
                || !id(blast.outputItem[0]).equals("ntmspace:ingot_stainless") || blast.outputItem[0].getCount() != 2) {
            errors.add("blast furnace nickel + steel -> " + (blast == null ? "none" : blast.getInternalName() + " " + blast.duration));
        }
        // RotaryFurnaceRecipes gunmetal: 3 copper + 1 ZINC (was aluminium) -> 4 ingots, 200 ticks, 100 steam
        RotaryFurnaceRecipes.RotaryFurnaceRecipe zinc = RotaryFurnaceRecipes.getRecipe(Fluids.NONE, stack("hbm:ingot_copper", 3), stack("ntmspace:ingot_zinc", 1));
        if (zinc == null || !zinc.output.material.name.equals("gunmetal") || zinc.output.amount != 288 || zinc.duration != 200
                || zinc.steam != 100 || zinc.fluid != null) {
            errors.add("rotary copper + zinc -> " + (zinc == null ? "none" : RotaryFurnaceRecipes.describe(zinc)));
        }
        RotaryFurnaceRecipes.RotaryFurnaceRecipe al = RotaryFurnaceRecipes.getRecipe(Fluids.NONE, stack("hbm:ingot_copper", 3), stack("hbm:ingot_aluminium", 1));
        if (al != null) errors.add("rotary copper + aluminium still makes " + RotaryFurnaceRecipes.describe(al));
        RecipeChangeGameTests.finish(helper, errors);
    }

    // ---------------------------------------------------------------- shredder

    private static void shredder(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        shred(errors, "minecraft:wheat", "ntmspace:flour", 1);
        shred(errors, "hbm:reinforced_light", "minecraft:glowstone_dust", 4);
        shred(errors, "hbm:ore_nether_fire", "hbm:scrap", 1); // the row is gone in hbm-space
        shred(errors, "ntmspace:ore_fire", "hbm:powder_fire", 6);
        shred(errors, "ntmspace:ore_gas_empty", "minecraft:gravel", 1);
        shred(errors, "ntmspace:ore_rare_moho", "hbm:powder_desh_mix", 1);
        shred(errors, "hbm:ore_rare", "hbm:powder_desh_mix", 1);
        shred(errors, "ntmspace:ore_aluminium_duna", "hbm:chunk_ore_cryolite", 2);
        shred(errors, "ntmspace:crystal_cleaned", "ntmspace:mineral_dust", 4);
        shred(errors, "ntmspace:bean_roast", "ntmspace:powder_coffee", 1);
        shred(errors, "ntmspace:stone_resource_calcium", "hbm:powder_calcium", 6);
        shred(errors, "hbm:stone_resource_limestone", "hbm:powder_limestone", 4); // 1.7's calcium row for it is dead
        shred(errors, "minecraft:granite", "minecraft:gravel", 1);                // KEY_STONE loop
        shred(errors, "minecraft:mossy_cobblestone", "minecraft:gravel", 1);      // KEY_COBBLESTONE loop
        shred(errors, "minecraft:red_sand", "hbm:dust", 2);                       // KEY_SAND loop
        shred(errors, "ntmspace:crystal_nickel", "ntmspace:powder_nickel", 3);
        shred(errors, "ntmspace:crystal_niobium", "hbm:powder_niobium", 3);
        shred(errors, "ntmspace:crystal_zinc", "ntmspace:powder_zinc", 3);
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void shred(List<String> errors, String input, String output, int count) {
        ItemStack in = stack(input, 1);
        if (in.isEmpty()) {
            errors.add("shredder input " + input + " is not registered");
            return;
        }
        ItemStack out = ShredderRecipes.getShredderResult(in);
        if (!id(out).equals(output) || out.getCount() != count) {
            errors.add("shredder " + input + " -> " + out.getCount() + " " + id(out) + ", want " + count + " " + output);
        }
    }

    // ---------------------------------------------------------------- anvil

    private static void anvil(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // smithing: ingot_gunmetal = CU ingot + ZI ingot (was AL), tier 1
        AnvilSmithingRecipe gunmetal = null;
        for (AnvilSmithingRecipe r : AnvilRecipes.getSmithing()) {
            if (id(r.getSimpleOutput()).equals("hbm:ingot_gunmetal")) gunmetal = r;
        }
        if (gunmetal == null || gunmetal.tier != 1 || !gunmetal.matches(stack("hbm:ingot_copper", 1), stack("ntmspace:ingot_zinc", 1))
                || gunmetal.matches(stack("hbm:ingot_copper", 1), stack("hbm:ingot_aluminium", 1))) {
            errors.add("smithing ingot_gunmetal is not copper + zinc");
        }
        int smithing = 0;
        for (AnvilSmithingRecipe r : AnvilRecipes.getSmithing()) if (id(r.getSimpleOutput()).equals("hbm:ingot_gunmetal")) smithing++;
        if (smithing != 1) errors.add(smithing + " gunmetal smithing rows");

        // construction: id, tier, first input (stack that must match, count), output id, output count
        construction(errors, "plate_nickel", 3, "ntmspace:ingot_nickel", 1, 1, "ntmspace:plate_nickel", 1);
        construction(errors, "plate_stainless", 3, "ntmspace:ingot_stainless", 1, 1, "ntmspace:plate_stainless", 1);
        construction(errors, "wire_fine_zinc", 4, "ntmspace:ingot_zinc", 1, 1, "ntmspace:wire_fine_zinc", 8);
        construction(errors, "coke_coal", 3, "ntmspace:powder_coke_coal", 1, 1, "hbm:coke_coal", 1);
        construction(errors, "coke_lignite", 3, "ntmspace:powder_coke_lignite", 1, 1, "hbm:coke_lignite", 1);
        construction(errors, "coke_petroleum", 3, "ntmspace:powder_coke_petroleum", 1, 1, "hbm:coke_petroleum", 1);
        construction(errors, "deco_stainless", 1, "ntmspace:ingot_stainless", 1, 1, "ntmspace:deco_stainless", 4);
        construction(errors, "dish_control", 3, "minecraft:oak_planks", 6, 5, "ntmspace:dish_control", 1);
        construction(errors, "machine_ashpit", 2, "minecraft:granite", 8, 3, "hbm:machine_ashpit", 1);
        construction(errors, "ingot_stainless", 1, "ntmspace:deco_stainless", 4, 1, "ntmspace:ingot_stainless", 1);
        if (AnvilRecipes.constructionById("hbm:anvil_construction/machine_ashpit") != null) errors.add("HBM's stone-only ashpit row is still there");
        AnvilRecipes.AnvilConstructionRecipe dish = AnvilRecipes.constructionById("ntmspace:anvil_construction/dish_control");
        if (dish != null) {
            int[] counts = dish.input.stream().mapToInt(s -> s.stacksize).toArray();
            if (!java.util.Arrays.equals(counts, new int[] {6, 8, 1, 8, 1})) errors.add("dish_control input counts " + java.util.Arrays.toString(counts));
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void construction(List<String> errors, String file, int tier, String input, int count, int inputs,
                                     String output, int outCount) {
        String rid = "ntmspace:anvil_construction/" + file;
        AnvilRecipes.AnvilConstructionRecipe r = AnvilRecipes.constructionById(rid);
        if (r == null) {
            errors.add(rid + " missing");
            return;
        }
        AStack first = r.input.isEmpty() ? null : r.input.get(0);
        ItemStack out = r.output.isEmpty() ? ItemStack.EMPTY : r.output.get(0).stack;
        if (r.tierLower != tier || r.input.size() != inputs || first == null || first.stacksize != count
                || !first.matchesRecipe(stack(input, count), true) || !id(out).equals(output) || out.getCount() != outCount) {
            errors.add(rid + " wrong: tier " + r.tierLower + ", " + r.input.size() + " inputs, -> " + out.getCount() + " " + id(out));
        }
    }

    // ---------------------------------------------------------------- material distribution

    private static void materialDistribution(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        item(errors, "ntmspace:blade_syngas", "stainless 216, ");
        item(errors, "ntmspace:stone_resource_calcium", "flux 864, ");
        item(errors, "hbm:ring_starmetal", "starmetal 288, ");
        tag(errors, "c:ores/zinc", "zinc 144, stone 162, ");
        tag(errors, "c:ores/conglomerate", "conglomerate 288, ");
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void item(List<String> errors, String item, String want) {
        List<Mats.MaterialStack> got = MaterialDistribution.items().get(BuiltInRegistries.ITEM.getValue(Ids.parse(item)));
        String desc = got == null ? "none" : describe(got.toArray(Mats.MaterialStack[]::new));
        if (!desc.equals(want)) errors.add("material of " + item + " is " + desc + ", want " + want);
    }

    private static void tag(List<String> errors, String tag, String want) {
        List<Mats.MaterialStack> got = MaterialDistribution.tags().get(tag);
        String desc = got == null ? "none" : describe(got.toArray(Mats.MaterialStack[]::new));
        if (!desc.equals(want)) errors.add("material of #" + tag + " is " + desc + ", want " + want);
    }

    // ---------------------------------------------------------------- helpers

    private static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static String id(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "empty" : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** {@code after} sits right behind {@code before} in {@code list} (1.7 registration order). */
    private static void follows(List<String> errors, List<String> list, String before, String after) {
        int i = list.indexOf(before);
        if (i < 0 || i + 1 >= list.size() || !list.get(i + 1).equals(after)) {
            errors.add(after + " does not follow " + before + " (" + (i < 0 ? "absent" : list.subList(i, Math.min(list.size(), i + 3))) + ")");
        }
    }
}
