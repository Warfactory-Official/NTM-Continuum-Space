package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's replacements of base-HBM crafting and smelting rows ({@code SpaceCraftingRecipes}, removals in {@code recipe_removals}):
 * Golem's {@code recipe_removals} lists take the HBM recipe out of the recipe manager (any recipe type, not only machine
 * tables), and the addon's row crafts what hbm-space's source makes. Each replaced row is checked both ways: the
 * hbm-space inputs craft, the 1.7 inputs no longer do. A recipe that is not listed stays.
 */
public final class RecipeChangeReplacementGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function REMOVED = TESTS.function("recipe_change_vanilla_removed", 40,
            () -> RecipeChangeReplacementGameTests::removed);

    public static final GameTests.Function REPLACED = TESTS.function("recipe_change_vanilla_replaced", 40,
            () -> RecipeChangeReplacementGameTests::replaced);

    private RecipeChangeReplacementGameTests() {}

    public static void init() {}

    /** HBM recipe ids listed in data/ntmspace/recipe_removals/{crafting,smelting}.json. */
    private static final String[] REMOVED_IDS = {"hbm:coffee", "hbm:laser_crystal_dnt", "hbm:powder_fertilizer",
            "hbm:powder_fertilizer_1", "hbm:refueler", "hbm:rtg_fuel_gold", "hbm:rtg_fuel_lead", "hbm:chunk_ore_cryolite_1"};

    /** HBM crafting/smelting recipes next to the removed ones that nobody lists: they must stay. */
    private static final String[] KEPT_IDS = {"hbm:rtg_fuel_polonium", "hbm:chunk_ore_cryolite"};

    /** The addon's replacement rows. */
    private static final String[] ADDED_IDS = {"crafting/coffee", "crafting/laser_crystal_dnt", "crafting/powder_fertilizer",
            "crafting/powder_fertilizer_1", "crafting/refueler", "crafting/pellet_rtg_gold", "crafting/pellet_rtg_lead",
            "smelting/ingot_aluminium_from_ore_meteor_aluminium"};

    private static void removed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        for (String id : REMOVED_IDS) {
            String[] parts = id.split(":", 2);
            if (Recipes.hasRecipe(level, parts[0], parts[1])) errors.add(id + " is still in the recipe manager");
        }
        for (String id : KEPT_IDS) {
            String[] parts = id.split(":", 2);
            if (!Recipes.hasRecipe(level, parts[0], parts[1])) errors.add(id + " (not listed) is missing");
        }
        for (String path : ADDED_IDS) {
            if (!Recipes.hasRecipe(level, NtmSpace.MOD_ID, path)) errors.add("ntmspace:" + path + " not loaded");
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void replaced(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        // MineralRecipes: pellet_rtg_lead / _gold <- 3 billets + NI.plate() (1.7: IRON.plate())
        craft(level, errors, "pellet_rtg_gold (nickel)", 2, 2, List.of(stack("hbm:billet_au198"), stack("hbm:billet_au198"),
                stack("hbm:billet_au198"), stack("ntmspace:plate_nickel")), "hbm:pellet_rtg_gold", 1);
        none(level, errors, "pellet_rtg_gold (iron)", 2, 2, List.of(stack("hbm:billet_au198"), stack("hbm:billet_au198"),
                stack("hbm:billet_au198"), stack("hbm:plate_iron")), "hbm:pellet_rtg_gold");
        craft(level, errors, "pellet_rtg_lead (nickel)", 2, 2, List.of(stack("hbm:billet_pb209"), stack("hbm:billet_pb209"),
                stack("hbm:billet_pb209"), stack("ntmspace:plate_nickel")), "hbm:pellet_rtg_lead", 1);
        none(level, errors, "pellet_rtg_lead (iron)", 2, 2, List.of(stack("hbm:billet_pb209"), stack("hbm:billet_pb209"),
                stack("hbm:billet_pb209"), stack("hbm:plate_iron")), "hbm:pellet_rtg_lead");
        // PowderRecipes: 4 fertilizer <- CA.dust(), P_RED.dust(), ammonium_nitrate (1.7: CA, P_RED, KNO, S)
        craft(level, errors, "powder_fertilizer", 3, 1, List.of(stack("hbm:powder_calcium"), stack("hbm:powder_fire"),
                stack("ntmspace:ammonium_nitrate")), "hbm:powder_fertilizer", 4);
        none(level, errors, "powder_fertilizer (1.7 sulfur row)", 2, 2, List.of(stack("hbm:powder_calcium"),
                stack("hbm:powder_fire"), stack("hbm:niter"), stack("hbm:sulfur")), "hbm:powder_fertilizer");
        // CraftingManager: refueler SS/HC/SS with STAINLESS.plate() (1.7: TI.plate())
        craft(level, errors, "refueler (stainless)", 2, 3, List.of(stack("ntmspace:plate_stainless"),
                stack("ntmspace:plate_stainless"), stack("hbm:part_generic_piston_hydraulic"), stack("hbm:circuit_basic"),
                stack("ntmspace:plate_stainless"), stack("ntmspace:plate_stainless")), "hbm:refueler", 1);
        none(level, errors, "refueler (titanium)", 2, 3, List.of(stack("hbm:plate_titanium"), stack("hbm:plate_titanium"),
                stack("hbm:part_generic_piston_hydraulic"), stack("hbm:circuit_basic"), stack("hbm:plate_titanium"),
                stack("hbm:plate_titanium")), "hbm:refueler");
        // CraftingManager: laser_crystal_dnt QDQ/SBZ/QDQ, Z = powder_zinc (1.7: SBS)
        ItemStack q = stack("hbm:glass_quartz");
        ItemStack d = stack("hbm:ingot_dineutronium");
        craft(level, errors, "laser_crystal_dnt (zinc)", 3, 3, List.of(q, d, q, stack("hbm:powder_spark_mix"),
                stack("hbm:egg_balefire"), stack("ntmspace:powder_zinc"), q, d, q), "hbm:laser_crystal_dnt", 1);
        none(level, errors, "laser_crystal_dnt (1.7 SBS)", 3, 3, List.of(q, d, q, stack("hbm:powder_spark_mix"),
                stack("hbm:egg_balefire"), stack("hbm:powder_spark_mix"), q, d, q), "hbm:laser_crystal_dnt");
        // SmeltingRecipes: ore_meteor_aluminium -> 16 aluminium ingots (1.7: 16 cryolite chunks)
        Optional<ItemStack> smelted = Recipes.smeltingResult(level, stack("hbm:ore_meteor_aluminium"));
        if (smelted.isEmpty() || !id(smelted.get()).equals("hbm:ingot_aluminium") || smelted.get().getCount() != 16) {
            errors.add("smelting ore_meteor_aluminium -> " + smelted.map(s -> id(s) + " x" + s.getCount()).orElse("nothing"));
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String id(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean missing(List<String> errors, String what, List<ItemStack> grid) {
        if (grid.stream().anyMatch(ItemStack::isEmpty)) {
            errors.add(what + ": an input is not registered");
            return true;
        }
        return false;
    }

    private static void craft(ServerLevel level, List<String> errors, String what, int w, int h, List<ItemStack> grid,
                              String out, int count) {
        if (missing(errors, what, grid)) return;
        Optional<ItemStack> result = Recipes.craftingResult(level, w, h, grid);
        if (result.isEmpty()) errors.add(what + ": no recipe");
        else if (!id(result.get()).equals(out) || result.get().getCount() != count) {
            errors.add(what + ": got " + id(result.get()) + " x" + result.get().getCount());
        }
    }

    /** The 1.7 inputs no longer make {@code out} (another recipe may use the same grid). */
    private static void none(ServerLevel level, List<String> errors, String what, int w, int h, List<ItemStack> grid,
                             String out) {
        if (missing(errors, what, grid)) return;
        Recipes.craftingResult(level, w, h, grid).filter(r -> id(r).equals(out))
                .ifPresent(r -> errors.add(what + ": still crafts " + id(r) + " x" + r.getCount()));
    }
}
