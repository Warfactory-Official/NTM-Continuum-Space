package com.ntmspace.gametest;

import com.google.gson.JsonElement;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.data.DataFiles;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * hbm-space's remaining items ({@code registry/SpaceItems}) and its crafting/smelting changes to base HBM
 * ({@code SpaceCraftingRecipes}): the items exist with their 1.7 rarity and ore-dictionary tags, the
 * fork's materials exist with its ids, and the recipes craft what hbm-space's {@code MineralRecipes},
 * {@code PowderRecipes}, {@code ConsumableRecipes}, {@code SmeltingRecipes} and {@code CraftingManager} rows make,
 * looked up through the vanilla recipe manager with the 1.7 inputs (expected values from the hbm-space source).
 */
public final class RecipeChangeCraftingGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function ITEMS = TESTS.function("recipe_change_crafting_items", 20,
            () -> RecipeChangeCraftingGameTests::items);

    public static final GameTests.Function FILES = TESTS.function("recipe_change_crafting_files_load", 40,
            () -> RecipeChangeCraftingGameTests::filesLoad);

    public static final GameTests.Function ROWS = TESTS.function("recipe_change_crafting_rows", 40,
            () -> RecipeChangeCraftingGameTests::rows);

    private RecipeChangeCraftingGameTests() {}

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(null);
    }

    private static ItemStack stack(String id) {
        Item item = item(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static boolean tagged(String id, String tag) {
        Item item = item(id);
        return item != null && new ItemStack(item).is(TagKey.create(Registries.ITEM, Ids.parse(tag)));
    }

    private static void items(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        String[] ids = {"ingot_nickel", "nugget_nickel", "powder_nickel", "plate_nickel", "crystal_nickel", "block_nickel",
                "block_osmiridium", "ingot_stainless", "plate_stainless", "plate_cast_stainless", "plate_welded_stainless",
                "plate_cast_iridium", "plate_welded_iridium", "wire_fine_zinc", "ingot_gaas", "billet_gaas", "ingot_cm_mix",
                "billet_cf252", "ingot_es255", "plate_cn989", "item_expensive_stainless_beam", "item_expensive_avionics",
                "item_expensive_astro", "powder_coke_coal", "powder_coke_lignite", "powder_coke_petroleum",
                "mineral_fragment_peroxide", "mineral_fragment_schrabidic", "flesh_burger", "glass_smilk", "teacup",
                "bottle_honey", "s_cream", "turbine_syngas", "ammonium_nitrate"};
        for (String id : ids) {
            if (item("ntmspace:" + id) == null) errors.add("ntmspace:" + id + " not registered");
        }
        // 1.7 rarities (EnumRarity on ItemCustomLore)
        expectRarity(errors, "ntmspace:ingot_cn989", Rarity.EPIC);
        expectRarity(errors, "ntmspace:ingot_gaas", Rarity.RARE);
        expectRarity(errors, "ntmspace:ingot_tt", Rarity.UNCOMMON);
        // ore dictionary -> c: tags
        String[][] tags = {{"ntmspace:ingot_nickel", "c:ingots/nickel_pure"}, {"ntmspace:plate_stainless", "c:plates/stainless"},
                {"ntmspace:ingot_stainless", "c:ingots/stainless"}, {"ntmspace:nugget_cm244", "c:nuggets/cm244"},
                {"ntmspace:billet_bk247", "c:billets/bk247"}, {"ntmspace:powder_gallium_tiny", "c:dusts_tiny/gallium"},
                {"ntmspace:ingot_zinc", "c:ingots/zinc"}, {"ntmspace:powder_coke_coal", "c:dusts/any_coal_coke"},
                {"ntmspace:duna_rock", "c:stones"}, {"ntmspace:moon_rock", "c:cobblestones"}, {"ntmspace:duna_sands", "c:sands"},
                {"hbm:cheese", "c:foods/cheese"}, {"ntmspace:plate_welded_stainless", "c:plates_welded/stainless"},
                {"ntmspace:plate_cast_stainless", "c:plates_cast/stainless"}, {"ntmspace:nugget_lanthanium", "c:nuggets/lanthanium"}};
        for (String[] t : tags) if (!tagged(t[0], t[1])) errors.add(t[0] + " not in #" + t[1]);
        // hbm-space Mats: ids and the autogen items found through HBM's table
        expectMat(errors, "stainless", 24_001);
        expectMat(errors, "gaas", 24_000);
        expectMat(errors, "iridium", 7_700);
        expectMat(errors, "conglomerate", 2_993);
        NTMMaterial stainless = Mats.matByName.get("stainless");
        if (stainless != null && ModAutogenItems.get(MaterialShapes.WELDEDPLATE, stainless) == null) {
            errors.add("no welded stainless plate in HBM's autogen table");
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void expectRarity(List<String> errors, String id, Rarity rarity) {
        ItemStack stack = stack(id);
        if (stack.isEmpty() || stack.getRarity() != rarity) errors.add(id + " rarity " + (stack.isEmpty() ? "-" : stack.getRarity()));
    }

    private static void expectMat(List<String> errors, String name, int id) {
        NTMMaterial mat = Mats.matByName.get(name);
        if (mat == null || mat.id != id) errors.add("material " + name + " " + (mat == null ? "missing" : "id " + mat.id));
    }

    /** Every crafting/smelting file of this family is a loaded recipe (a bad id would drop it with only a log line). */
    private static void filesLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        // another installed mod may remove one of these on purpose (its recipe_removals list; the dev probe does)
        java.util.Set<String> removed = new java.util.HashSet<>();
        for (JsonElement list : DataFiles.read(dev.golem.core.world.RecipeTables.REMOVALS, level.getServer().getResourceManager()).values()) {
            if (list.isJsonObject() && list.getAsJsonObject().has("recipes")) {
                for (JsonElement id : list.getAsJsonObject().getAsJsonArray("recipes")) removed.add(id.getAsString());
            }
        }
        for (Map.Entry<String, JsonElement> file : DataFiles.read(DataFiles.recipeFolder(), level.getServer().getResourceManager()).entrySet()) {
            String id = file.getKey();
            if (!id.startsWith(NtmSpace.MOD_ID + ":crafting/") && !id.startsWith(NtmSpace.MOD_ID + ":smelting/")) continue;
            if (removed.contains(id)) continue;
            checked++;
            if (!Recipes.hasRecipe(level, NtmSpace.MOD_ID, id.substring(id.indexOf(':') + 1))) errors.add(id + " not loaded");
        }
        if (checked < 150) errors.add("only " + checked + " crafting/smelting files found");
        NtmSpace.LOG.info("recipe_change_crafting_files_load: {} files, {} not loaded", checked, errors.size());
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void rows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        // MineralRecipes.add1To9Pair(ingot_nickel, nugget_nickel)
        craft(level, errors, "nugget_nickel x9 -> ingot_nickel", 3, 3, Collections.nCopies(9, stack("ntmspace:nugget_nickel")), "ntmspace:ingot_nickel", 1);
        craft(level, errors, "ingot_nickel -> 9 nugget_nickel", 1, 1, List.of(stack("ntmspace:ingot_nickel")), "ntmspace:nugget_nickel", 9);
        // addBillet(billet_cm244, ..., CM244.all(NUGGET)): 6 nuggets -> billet; addBilletToIngot: 3 billets -> 2 ingots
        craft(level, errors, "6 nugget_cm244 -> billet", 3, 2, Collections.nCopies(6, stack("ntmspace:nugget_cm244")), "ntmspace:billet_cm244", 1);
        craft(level, errors, "3 billet_cm244 -> 2 ingots", 3, 1, Collections.nCopies(3, stack("ntmspace:billet_cm244")), "ntmspace:ingot_cm244", 2);
        // billet_cm_mix x3 <- billet_cm244, billet_cm245, billet_cm245
        craft(level, errors, "cm_mix from billets", 3, 1, List.of(stack("ntmspace:billet_cm244"), stack("ntmspace:billet_cm245"),
                stack("ntmspace:billet_cm245")), "ntmspace:billet_cm_mix", 3);
        // pellet_rtg_cf252 <- 3 billet_cf252 + NI.plate()
        craft(level, errors, "pellet_rtg_cf252", 2, 2, List.of(stack("ntmspace:billet_cf252"), stack("ntmspace:billet_cf252"),
                stack("ntmspace:billet_cf252"), stack("ntmspace:plate_nickel")), "ntmspace:pellet_rtg_cf252", 1);
        // ConsumableRecipes: flesh_burger <- bread + grilled_flesh
        craft(level, errors, "flesh_burger", 2, 1, List.of(new ItemStack(Items.BREAD), stack("ntmspace:grilled_flesh")), "ntmspace:flesh_burger", 1);
        // PowderRecipes: 8 bone meal <- ammonium nitrate + calcium dust
        craft(level, errors, "bone meal", 2, 1, List.of(stack("ntmspace:ammonium_nitrate"), stack("hbm:powder_calcium")), "minecraft:bone_meal", 8);
        // CraftingManager: 4 stone bricks <- 4 KEY_STONE (Duna rock is "stone" in hbm-space)
        craft(level, errors, "stonebrick from duna_rock", 2, 2, Collections.nCopies(4, stack("ntmspace:duna_rock")), "minecraft:stone_bricks", 4);
        // CraftingManager: turbine_syngas <- 8 blades around a stainless ingot
        List<ItemStack> turbine = new ArrayList<>(Collections.nCopies(9, stack("ntmspace:blade_syngas")));
        turbine.set(4, stack("ntmspace:ingot_stainless"));
        craft(level, errors, "turbine_syngas", 3, 3, turbine, "ntmspace:turbine_syngas", 1);
        // craneCasing STAINLESS.ingot(), 8: CCC / C C / CBC with the regular conveyor wand -> 8 inserters
        ItemStack c = stack("ntmspace:ingot_stainless");
        craft(level, errors, "crane_inserter stainless", 3, 3, List.of(c, c, c, c, ItemStack.EMPTY, c, c,
                stack("hbm:conveyor_wand_regular"), c), "hbm:crane_inserter", 8);
        // SmeltingRecipes
        smelt(level, errors, "ntmspace:crystal_nickel", "ntmspace:ingot_nickel", 2);
        smelt(level, errors, "ntmspace:flour", "minecraft:bread", 1);
        smelt(level, errors, "ntmspace:duna_cobble", "ntmspace:duna_rock", 1);
        smelt(level, errors, "ntmspace:flesh_wafer", "ntmspace:grilled_flesh", 1);
        smelt(level, errors, "hbm:moon_turf", "minecraft:glass", 1);
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static void craft(ServerLevel level, List<String> errors, String what, int w, int h, List<ItemStack> grid,
                              String out, int count) {
        for (ItemStack s : grid) {
            if (s == null) {
                errors.add(what + ": an input is not registered");
                return;
            }
        }
        Optional<ItemStack> result = Recipes.craftingResult(level, w, h, grid);
        if (result.isEmpty()) {
            errors.add(what + ": no recipe");
            return;
        }
        ItemStack r = result.get();
        if (!String.valueOf(BuiltInRegistries.ITEM.getKey(r.getItem())).equals(out) || r.getCount() != count) {
            errors.add(what + ": got " + BuiltInRegistries.ITEM.getKey(r.getItem()) + " x" + r.getCount());
        }
    }

    private static void smelt(ServerLevel level, List<String> errors, String in, String out, int count) {
        Optional<ItemStack> result = Recipes.smeltingResult(level, stack(in));
        if (result.isEmpty() || !String.valueOf(BuiltInRegistries.ITEM.getKey(result.get().getItem())).equals(out)
                || result.get().getCount() != count) {
            errors.add("smelting " + in + " -> " + result.map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()) + " x" + s.getCount()).orElse("nothing"));
        }
    }
}
