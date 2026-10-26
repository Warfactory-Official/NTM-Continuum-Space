package com.ntmspace.recipe;


import static dev.golem.core.dejson.Crafting.anyOf;
import static dev.golem.core.dejson.Crafting.blast;
import static dev.golem.core.dejson.Crafting.componentShaped;
import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.fluid;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.key;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.smelt;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * hbm-space {@code MineralRecipes}, {@code PowderRecipes}, {@code ConsumableRecipes}, {@code ArmorRecipes},
 * {@code ToolRecipes}, {@code RodRecipes}, {@code SmeltingRecipes} and {@code main/CraftingManager}: the crafting and
 * smelting recipes NTM: Space adds to base HBM, declared through Golem Core's {@code dejson} vocabulary.
 *
 * <p>Ids are the addon's ({@code ntmspace:crafting/...}, {@code ntmspace:smelting/...} and a few top-level ones); a
 * row that replaces an HBM recipe is an ordinary row here, and the replaced HBM id is in the authored
 * {@code data/ntmspace/recipe_removals/{crafting,smelting}.json}. Rows where hbm-space only widened an ingredient to an
 * ore-dictionary key (stone, sand, cheese, cobblestone, coal dust to any coal/coke dust) are additions next to HBM's own
 * row, so what a player can craft is hbm-space's.
 *
 * <p>Declarations only: this class runs in a plain JVM (Dejson), so it names everything by string id.
 */
public final class SpaceCraftingRecipes {

    private static final String NS = "ntmspace";

    private static boolean declared;

    private SpaceCraftingRecipes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        mineralRecipes();
        powderRecipes();
        consumableRecipes();
        armorRecipes();
        toolRecipes();
        rodRecipes();
        craftingManager();
        smeltingRecipes();
    }

    // ------------------------------------------------------------------------------------------------ MineralRecipes

    /** hbm-space {@code MineralRecipes}: the new 1-to-9 pairs, billets, billet mixes and depleted-pellet recycling. */
    private static void mineralRecipes() {
        String[][] pairs = {
                {"powder_gallium", "powder_gallium_tiny"}, {"ingot_nickel", "nugget_nickel"},
                {"ingot_hafnium", "nugget_hafnium"}, {"ingot_bk247", "nugget_bk247"},
                {"ingot_cm242", "nugget_cm242"}, {"ingot_cm243", "nugget_cm243"}, {"ingot_cm244", "nugget_cm244"},
                {"ingot_cm245", "nugget_cm245"}, {"ingot_cm246", "nugget_cm246"}, {"ingot_cm247", "nugget_cm247"},
                {"ingot_cf251", "nugget_cf251"}, {"ingot_cf252", "nugget_cf252"}, {"ingot_es253", "nugget_es253"},
                {"ingot_gaas", "nugget_gaas"}, {"ingot_zinc", "nugget_zinc"}, {"ingot_gallium", "nugget_gallium"},
                {"ingot_cm_fuel", "nugget_cm_fuel"}, {"ingot_cm_mix", "nugget_cm_mix"},
                {"ingot_menthol", "nugget_menthol"},
        };
        for (String[] pair : pairs) {
            add1To9Pair(ns(pair[0]), ns(pair[1]), pair[0], pair[1]);
        }
        add1To9Pair(ns("block_osmiridium"), "hbm:ingot_osmiridium", "block_osmiridium", "ingot_osmiridium");
        // addMineralSet(nugget_lanthanium, ingot_lanthanium, block_lanthanium): the block pair is HBM's already
        add1To9Pair("hbm:ingot_lanthanium", ns("nugget_lanthanium"), "ingot_lanthanium", "nugget_lanthanium");
        // block_nickel <-> ingot_nickel, two GameRegistry.addRecipe lines
        shaped(NS, "crafting/block_nickel", ns("block_nickel"), rows("###", "###", "###"), "#", ns("ingot_nickel"));
        shaped(NS, "crafting/ingot_nickel_from_block_nickel", ns("ingot_nickel"), 9, rows("#"),
                "#", ns("block_nickel"));
        for (String g : new String[] {"greater", "lesser"}) {
            add1To9Pair(ns("ingot_australium_" + g), "hbm:nugget_australium_" + g, "ingot_australium_" + g,
                    "nugget_australium_" + g);
        }

        // 1.7 waste nuggets: add1To9PairSameMeta(nuclear_waste_short(_depleted), ..._tiny, i) for hbm-space's new
        // ItemWasteShort.WasteClass entries
        String[][] waste = {{"7", "americium241"}, {"9", "berkelium247"}, {"10", "curium244"}, {"11", "curium245"}};
        for (String[] w : waste) {
            for (String kind : new String[] {"nuclear_waste_short", "nuclear_waste_short_depleted"}) {
                shapeless(NS, "crafting/" + kind + "_to_" + kind + "_tiny_" + w[0],
                        ns(kind + "_tiny_" + w[1]), 9, ns(kind + "_" + w[1]));
                shaped(NS, "crafting/" + kind + "_tiny_to_" + kind + "_" + w[0], ns(kind + "_" + w[1]),
                        rows("###", "###", "###"), "#", ns(kind + "_tiny_" + w[1]));
            }
        }

        addBillet(ns("billet_gaas"), ns("ingot_gaas"), ns("nugget_gaas"), "billet_gaas", null);
        for (String iso : new String[] {"bk247", "cf251", "cf252", "es253", "cm242", "cm243", "cm244", "cm245",
                "cm246", "cm247"}) {
            // addBillet(..., ISO.all(MaterialShapes.NUGGET)): the ore-name rows; the tag holds the item too
            addBillet(ns("billet_" + iso), ns("ingot_" + iso), ns("nugget_" + iso), "billet_" + iso,
                    "#c:nuggets/" + iso);
        }
        addBillet(ns("billet_cm_mix"), ns("ingot_cm_mix"), ns("nugget_cm_mix"), "billet_cm_mix", null);
        addBillet(ns("billet_cm_fuel"), ns("ingot_cm_fuel"), ns("nugget_cm_fuel"), "billet_cm_fuel", null);
        addBillet(ns("billet_menthol"), ns("ingot_menthol"), ns("nugget_menthol"), "billet_menthol", null);
        // addBillet(billet_australium_greater/lesser, ingot_..., nugget_...): HBM has the nugget rows
        for (String g : new String[] {"greater", "lesser"}) {
            addBilletToIngot("hbm:billet_australium_" + g, ns("ingot_australium_" + g), "billet_australium_" + g);
        }
        addBilletToIngot(ns("billet_red_copper"), "hbm:ingot_red_copper", "billet_red_copper");

        // ShapelessOreRecipe(billet_bk247, 6x "nuggetBerkelium247")
        shapeless(NS, "crafting/billet_bk247_from_nuggets_shapeless", ns("billet_bk247"),
                "#c:nuggets/bk247", "#c:nuggets/bk247", "#c:nuggets/bk247",
                "#c:nuggets/bk247", "#c:nuggets/bk247", "#c:nuggets/bk247");
        shapeless(NS, "crafting/billet_cm_fuel_from_billets", ns("billet_cm_fuel"), 3,
                "hbm:billet_u238", "hbm:billet_u238", ns("billet_cm_mix"));
        // 2 nugget_cm_mix + 4 "nuggetUranium238" (the "tinyU238" row is the same tag here)
        shapeless(NS, "crafting/billet_cm_fuel_from_nuggets_shapeless", ns("billet_cm_fuel"),
                ns("nugget_cm_mix"), ns("nugget_cm_mix"),
                "#c:nuggets/u238", "#c:nuggets/u238", "#c:nuggets/u238", "#c:nuggets/u238");
        shapeless(NS, "crafting/billet_cm_mix_from_billets", ns("billet_cm_mix"), 3,
                ns("billet_cm244"), ns("billet_cm245"), ns("billet_cm245"));
        // 2 "nuggetCm244" + 4 "nuggetCm245" (the "tinyCm244/245" rows are the same tags here)
        shapeless(NS, "crafting/billet_cm_mix_from_nuggets_shapeless", ns("billet_cm_mix"),
                "#c:nuggets/cm244", "#c:nuggets/cm244",
                "#c:nuggets/cm245", "#c:nuggets/cm245", "#c:nuggets/cm245", "#c:nuggets/cm245");

        // pellet_rtg_depleted recycling (DepletedRTGMaterial.NICKEL, AMERICIUM)
        // hbm-space registers the nickel row unconditionally (MineralRecipes:301; it has its own ingot_nickel)
        shapeless(NS, "crafting/ingot_nickel_from_pellet_rtg_depleted_nickel", ns("ingot_nickel"), 2,
                "hbm:pellet_rtg_depleted_nickel");
        shapeless(NS, "crafting/billet_am_mix_from_pellet_rtg_depleted_americium", "hbm:billet_am_mix", 3,
                ns("pellet_rtg_depleted_americium"));

        // RTG pellets: berkelium/cf251/cf252 new; lead/gold switch IRON.plate to NI.plate (replacements)
        for (String[] p : new String[][] {{"pellet_rtg_berkelium", "billet_bk247"}, {"pellet_rtg_cf251", "billet_cf251"},
                {"pellet_rtg_cf252", "billet_cf252"}}) {
            shapeless(NS, "crafting/" + p[0], ns(p[0]), ns(p[1]), ns(p[1]), ns(p[1]), "#c:plates/nickel");
        }
        shapeless(NS, "crafting/pellet_rtg_lead", "hbm:pellet_rtg_lead",
                "hbm:billet_pb209", "hbm:billet_pb209", "hbm:billet_pb209", "#c:plates/nickel");
        shapeless(NS, "crafting/pellet_rtg_gold", "hbm:pellet_rtg_gold",
                "hbm:billet_au198", "hbm:billet_au198", "hbm:billet_au198", "#c:plates/nickel");

        // add1To9Pair(ModBlocks.bf_log, ModItems.woodemium_briquette)
        shapeless(NS, "woodemium_briquette", ns("woodemium_briquette"), 9, ns("bf_log"));
        shaped(NS, "bf_log", ns("bf_log"), rows("###", "###", "###"), "#", ns("woodemium_briquette"));
    }

    /** 1.7 {@code add1To9Pair(one, nine)}: one to nine ({@code "#"}) and nine to one. */
    private static void add1To9Pair(String one, String nine, String nameOne, String nameNine) {
        shaped(NS, "crafting/" + nameNine + "_from_" + nameOne, nine, 9, rows("#"), "#", one);
        shaped(NS, "crafting/" + nameOne + "_from_" + nameNine, one, rows("###", "###", "###"), "#", nine);
    }

    /**
     * 1.7 {@code addBillet(billet, ingot, nugget[, ore])}: six nuggets to a billet (by the ore name when given), a
     * billet back to six nuggets, and {@code addBilletToIngot}.
     */
    private static void addBillet(String billet, String ingot, String nugget, String name, String nuggetTag) {
        shaped(NS, "crafting/" + name + "_from_nuggets", billet, rows("###", "###"), "#",
                nuggetTag != null ? nuggetTag : nugget);
        shapeless(NS, "crafting/" + name.replace("billet_", "nugget_") + "_from_" + name, nugget, 6, billet);
        addBilletToIngot(billet, ingot, name);
    }

    /** 1.7 {@code addBilletToIngot}: three billets to two ingots, and two ingots to three billets. */
    private static void addBilletToIngot(String billet, String ingot, String name) {
        String ingotName = ingot.substring(ingot.indexOf(':') + 1);
        shapeless(NS, "crafting/" + ingotName + "_from_" + name, ingot, 2, billet, billet, billet);
        shaped(NS, "crafting/" + name + "_from_" + ingotName, billet, 3, rows("##"), "#", ingot);
    }

    // ------------------------------------------------------------------------------------------------ PowderRecipes

    /** hbm-space {@code PowderRecipes}. */
    private static void powderRecipes() {
        // ball_dynamite: Blocks.sand to KEY_SAND (widened; HBM's row with the chemistry set kept as is)
        componentShapeless(NS, "crafting/ball_dynamite", stack("hbm:ball_dynamite", 2),
                anyOf("#c:dusts/niter"), anyOf("minecraft:sugar"), anyOf("#c:sands"),
                anyOf("hbm:chemistry_set", "hbm:chemistry_set_boron").degrade());
        // Alite cement: 4 powder_cement from CA.dust(), KEY_SAND, clay_ball, clay_ball
        shapeless(NS, "crafting/powder_cement_alite", "hbm:powder_cement", 4,
                "#c:dusts/calcium", "#c:sands", "minecraft:clay_ball", "minecraft:clay_ball");
        // 8 bone meal (Items.dye meta 15) from ammonium_nitrate, CA.dust()
        shapeless(NS, "crafting/bone_meal_from_ammonium_nitrate", "minecraft:bone_meal", 8,
                ns("ammonium_nitrate"), "#c:dusts/calcium");
        // powder_desh_ready from desh mix, 2 mercury, ANY_COKE.dust() (the coal-dust row stays)
        shapeless(NS, "crafting/powder_desh_ready_coke", "hbm:powder_desh_ready",
                "hbm:powder_desh_mix", "hbm:nugget_mercury", "hbm:nugget_mercury", "#c:dusts/any_coke");
        // fertilizer: both rows replaced (sulfur dropped, ammonium nitrate added)
        shapeless(NS, "crafting/powder_fertilizer", "hbm:powder_fertilizer", 4,
                "#c:dusts/calcium", "#c:dusts/red_phosphorus", ns("ammonium_nitrate"));
        shapeless(NS, "crafting/powder_fertilizer_1", "hbm:powder_fertilizer", 4,
                "#c:dusts/ash", "#c:dusts/red_phosphorus", "#c:dusts/niter", ns("ammonium_nitrate"));
    }

    // -------------------------------------------------------------------------------------------- ConsumableRecipes

    /** hbm-space {@code ConsumableRecipes}. */
    private static void consumableRecipes() {
        shapeless(NS, "crafting/flesh_burger", ns("flesh_burger"), "minecraft:bread", ns("grilled_flesh"));
        // quesadilla: cheese to KEY_CHEESE (widened)
        shapeless(NS, "crafting/cheese_quesadilla", "hbm:cheese_quesadilla", 3,
                "#c:foods/cheese", "#c:foods/cheese", "minecraft:bread");
        // coffee: COAL.dust() to powder_coffee (replacement); HBM's row is a component recipe with a water bottle
        componentShapeless(NS, "crafting/coffee", stack("hbm:coffee"),
                item(ns("powder_coffee")), item("minecraft:milk_bucket"),
                item("minecraft:potion").remainder("minecraft:air").potion("minecraft:water"),
                item("minecraft:sugar"));
        // cladding_rubber: COAL.dust() to ANY_COAL_COKE.dust() (coal dust still matches: widened, an addition)
        shaped(NS, "crafting/cladding_rubber", "hbm:cladding_rubber", rows("RCR", "CDC", "RCR"),
                "R", "#c:ingots/any_rubber", "C", "#c:dusts/any_coal_coke", "D", "hbm:ducttape");
    }

    // ------------------------------------------------------------------------------------------------- ArmorRecipes

    /** hbm-space {@code ArmorRecipes}. */
    private static void armorRecipes() {
        shaped(NS, "oxy_plss", ns("oxy_plss"), rows("AA", "TC", "RR"),
                "A", "#c:plates/aluminium", "T", "hbm:tank_steel", "C", "hbm:circuit_basic", "R", "#c:ingots/rubber");
        shaped(NS, "crafting/flippers", ns("flippers"), rows("R R", "R R"), "R", "#c:ingots/rubber");
        shaped(NS, "crafting/heavy_boots", ns("heavy_boots"), rows("L L", "S S"),
                "L", "minecraft:leather", "S", "#c:ingots/steel");
    }

    // -------------------------------------------------------------------------------------------------- ToolRecipes

    /** hbm-space {@code ToolRecipes}. */
    private static void toolRecipes() {
        shaped(NS, "transporter_linker", ns("transporter_linker"), rows("S", "C", "P"),
                "S", "hbm:crt_display", "C", "hbm:circuit_basic", "P", "#c:plates/aluminium");
        shaped(NS, "crafting/atmosphere_scanner", ns("atmosphere_scanner"), rows("QCQ", "WBW", "SSS"),
                "Q", "hbm:glass_quartz", "C", "hbm:circuit_basic", "W", "#c:wires/gold",
                "B", "hbm:circuit_controller_chassis", "S", "#c:plates/stainless");
    }

    // --------------------------------------------------------------------------------------------------- RodRecipes

    /** hbm-space {@code RodRecipes}: the new RBMK rods and Watz pellets. */
    private static void rodRecipes() {
        addRBMKRod("#c:billets/bk247", ns("rbmk_fuel_bk247"));
        addRBMKRod(ns("billet_cm_fuel"), ns("rbmk_fuel_lecm"));
        addRBMKRod("#c:billets/cm_mix", ns("rbmk_fuel_mecm"));
        addRBMKRod("#c:billets/cm245", ns("rbmk_fuel_hecm"));

        addPellet("#c:ingots/pu241", "pu241");
        addPellet("#c:ingots/americium_fuel", "amf");
        addPellet("#c:ingots/am_mix", "amrg");
        addPellet("#c:ingots/cm_mix", "cmrg");
        addPellet("#c:ingots/cm_fuel", "cmf");
        addPellet("#c:ingots/bk247", "bk247");
        addPellet("#c:ingots/cf252", "cf252");
        addPellet("#c:ingots/cf251", "cf251");
        addPellet("#c:ingots/es253", "es253");
    }

    /** Fill an RBMK rod with 8 billets. */
    private static void addRBMKRod(String billet, String out) {
        shapeless(NS, "crafting/" + out.substring(out.indexOf(':') + 1), out, "hbm:rbmk_fuel_empty",
                billet, billet, billet, billet, billet, billet, billet, billet);
    }

    /** Watz pellet crafting ({@code EnumWatzType} is one item per type here). */
    private static void addPellet(String ingot, String type) {
        shaped(NS, "crafting/watz_pellet_" + type, ns("watz_pellet_" + type), rows(" I ", "IGI", " I "),
                "I", ingot, "G", "#c:ingots/graphite");
    }

    // ---------------------------------------------------------------------------------------------- CraftingManager

    /** hbm-space {@code main/CraftingManager}. */
    private static void craftingManager() {
        // stick_vinyl / stick_pvc / planks / paper (CraftingManager:208-221)
        shaped(NS, "stick_vinyl", ns("stick_vinyl"), 3, rows("L", "L"), "L", ns("vinyl_planks"));
        shaped(NS, "stick_pvc", ns("stick_pvc"), 3, rows("L", "L"), "L", ns("pvc_planks"));
        shapeless(NS, "vinyl_planks", ns("vinyl_planks"), 4, ns("vinyl_log"));
        shapeless(NS, "pvc_planks", ns("pvc_planks"), 4, ns("pvc_log"));
        shaped(NS, "paper_from_vinyl_planks", "minecraft:paper", 3, rows("LL"), "L", ns("vinyl_planks"));

        shaped(NS, "crafting/stonebrick", "minecraft:stone_bricks", 4, rows("SS", "SS"), "S", "#c:stones");
        for (String wire : new String[] {"aluminium", "copper"}) {
            shaped(NS, "crafting/circuit_capacitor_lanthanium_" + wire, ns("circuit_capacitor_lanthanium"),
                    rows("I", "N", "W"),
                    "I", "hbm:plate_polymer", "N", "#c:nuggets/lanthanium", "W", "#c:wires/" + wire);
        }
        shaped(NS, "crafting/turbine_syngas", ns("turbine_syngas"), rows("BBB", "BSB", "BBB"),
                "B", ns("blade_syngas"), "S", "#c:ingots/stainless");
        shapeless(NS, "crafting/cmug_empty", ns("cmug_empty"), "minecraft:clay_ball");
        shaped(NS, "crafting/glass_empty", ns("glass_empty"), rows("G G", "GGG", " G "), "G", "minecraft:glass");
        shapeless(NS, "crafting/teacup_empty", ns("teacup_empty"), "minecraft:clay_ball", "hbm:powder_calcium");
        // Blocks.stone to KEY_STONE / Blocks.cobblestone to KEY_COBBLESTONE (widened)
        shaped(NS, "crafting/machine_ammo_press", "hbm:machine_ammo_press", rows("IPI", "C C", "SSS"),
                "I", "#c:ingots/iron", "P", "minecraft:piston", "C", "#c:ingots/copper", "S", "#c:stones");
        shaped(NS, "crafting/machine_furnace_brick_off", "hbm:machine_furnace_brick_off", rows("III", "I I", "BBB"),
                "I", "minecraft:brick", "B", "#c:stones");
        shaped(NS, "crafting/reinforced_stone", "hbm:reinforced_stone", 4, rows("FBF", "BFB", "FBF"),
                "F", "#c:cobblestones", "B", "#c:stones");
        shapeless(NS, "crafting/lightstone", "hbm:lightstone_unrefined", 4,
                "#c:stones", "#c:stones", "#c:stones", "hbm:powder_limestone");
        componentShaped(NS, "crafting/press_preheater", stack("hbm:press_preheater"), rows("CCC", "SLS", "TST"),
                key("C", item("hbm:plate_copper")), key("S", anyOf("#c:stones")),
                key("L", fluid("LAVA", 1000)), key("T", item("hbm:ingot_tungsten")));
        shaped(NS, "crafting/laser_crystal_iron", ns("laser_crystal_iron"), rows("QGQ", "CSC", "QGQ"),
                "Q", "hbm:glass_quartz", "G", "#c:ingots/gaas", "C", "hbm:crystal_iron",
                "S", "hbm:egg_balefire_shard");
        // laser_crystal_dnt: one balefire-side powder_spark_mix replaced by zinc powder (replacement)
        shaped(NS, "crafting/laser_crystal_dnt", "hbm:laser_crystal_dnt", rows("QDQ", "SBZ", "QDQ"),
                "Q", "hbm:glass_quartz", "D", "#c:ingots/dineutronium", "B", "hbm:egg_balefire",
                "S", "hbm:powder_spark_mix", "Z", "#c:dusts/zinc");
        shaped(NS, "crafting/beryllium_mirror", ns("beryllium_mirror"), rows("BBN", "BNB", "NBB"),
                "B", "#c:billets/beryllium", "N", "#c:wires_dense/neodymium");
        // rad absorbers: COAL.dust() to ANY_COAL_COKE.dust() (widened, an addition)
        shaped(NS, "crafting/rad_absorber", "hbm:rad_absorber", rows("ICI", "CPC", "ICI"),
                "I", "#c:ingots/copper", "C", "#c:dusts/any_coal_coke", "P", "#c:dusts/lead");
        shaped(NS, "crafting/rad_absorber_red", "hbm:rad_absorber_red", rows("ICI", "CPC", "ICI"),
                "I", "#c:ingots/titanium", "C", "#c:dusts/any_coal_coke", "P", "hbm:rad_absorber");
        shaped(NS, "crafting/powder_wd2004", ns("powder_wd2004"), rows("PPP", "PCP", "PPP"),
                "P", ns("powder_wd2004_tiny"), "C", "hbm:powder_dineutronium");
        // refueler: TI.plate() to STAINLESS.plate() (replacement)
        shaped(NS, "crafting/refueler", "hbm:refueler", rows("SS", "HC", "SS"),
                "S", "#c:plates/stainless", "H", "hbm:part_generic_piston_hydraulic", "C", "hbm:circuit_basic");
        // crane casings: a fourth casing, 8 stainless ingots (1.7 craneCasing array)
        String[][] belts = {{"hbm:conveyor_wand_regular", ""}, {"hbm:conveyor", "_block"}};
        for (String[] belt : belts) {
            shaped(NS, "crafting/crane_inserter_stainless" + belt[1], "hbm:crane_inserter", 8,
                    rows("CCC", "C C", "CBC"), "C", "#c:ingots/stainless", "B", belt[0]);
        }
        for (String[] belt : belts) {
            shaped(NS, "crafting/crane_extractor_stainless" + belt[1], "hbm:crane_extractor", 8,
                    rows("CCC", "CPC", "CBC"), "C", "#c:ingots/stainless", "B", belt[0],
                    "P", "hbm:part_generic_piston_pneumatic");
        }
        for (String[] belt : belts) {
            shaped(NS, "crafting/crane_grabber_stainless" + belt[1], "hbm:crane_grabber", 8,
                    rows("C C", "P P", "CBC"), "C", "#c:ingots/stainless", "B", belt[0],
                    "P", "hbm:part_generic_piston_pneumatic");
        }

        shaped(NS, "crafting/det_salt", ns("det_salt"), rows("PIP", "DCD", "PIP"),
                "P", "hbm:ingot_cobalt", "I", "#c:plates_cast/tcalloy", "D", ns("billet_gaas"), "C", "hbm:det_nuke");
        componentShaped(NS, "lox_tank", stack(ns("lox_tank")), rows(" S ", "BKB", " S "),
                key("S", anyOf("#c:plates/steel")), key("B", anyOf("#c:bolts/steel")),
                key("K", fluid("OXYGEN", 1000)));
        shaped(NS, "air_vent", ns("air_vent"), rows("IGI", "ICI", "IDI"),
                "I", "#c:plates/iron", "G", "minecraft:iron_bars", "C", "hbm:tank_steel", "D", "minecraft:dispenser");
        // 1.7 Space CraftingManager:765: the angel spawner (meteorite/divine-shard/balefire-shard cross)
        shaped(NS, "spawn_angel", ns("spawn_angel"), rows("MSM", "SBS", "MSM"),
                "M", "hbm:ingot_meteorite", "S", ns("divine_shard"), "B", "hbm:egg_balefire_shard");
    }

    // --------------------------------------------------------------------------------------------- SmeltingRecipes

    /** hbm-space {@code SmeltingRecipes}. */
    private static void smeltingRecipes() {
        // ores: each ingredient is the tag of that ore's block and its planet variants
        oreSmelt("ore_quartz", "minecraft:quartz", 0.2);
        oreSmelt("ore_thorium", "hbm:ingot_th232", 3.0);
        oreSmelt("ore_uranium", "hbm:ingot_uranium", 6.0);
        oreSmelt("ore_titanium", "hbm:ingot_titanium", 3.0);
        oreSmelt("ore_copper", "minecraft:copper_ingot", 2.5);
        oreSmelt("ore_zinc", ns("ingot_zinc"), 2.5);
        oreSmelt("ore_tungsten", "hbm:ingot_tungsten", 6.0);
        oreSmelt("ore_aluminium", "hbm:chunk_ore_cryolite", 2.5);
        oreSmelt("ore_nickel", ns("chunk_ore_pentlandite"), 2.5);
        oreSmelt("ore_lead", "hbm:ingot_lead", 3.0);
        oreSmelt("ore_beryllium", "hbm:ingot_beryllium", 2.0);
        oreSmelt("ore_schrabidium", "hbm:ingot_schrabidium", 128.0);
        oreSmelt("ore_cobalt", "hbm:ingot_cobalt", 2.0);
        oreSmelt("ore_lanthanium", "hbm:ingot_lanthanium", 4.0);
        oreSmelt("ore_niobium", "hbm:ingot_niobium", 4.0);
        oreSmelt("ore_australium", "hbm:nugget_australium", 2.5);
        // raw australium smelts to its nugget (SpaceRawOreRecipes.cooking)
        smelt(NS, "nugget_australium_from_raw_australium", ns("raw_australium"), "hbm:nugget_australium", 2.5);
        blast(NS, "nugget_australium_from_raw_australium_blasting", ns("raw_australium"), "hbm:nugget_australium", 2.5);
        // meteor aluminium ore: 16 cryolite chunks became 16 aluminium ingots (replacement)
        smelt(NS, "smelting/ingot_aluminium_from_ore_meteor_aluminium", "hbm:ore_meteor_aluminium",
                "hbm:ingot_aluminium", 10.0, 16);

        smelt(NS, "smelting/bread_from_flour", ns("flour"), "minecraft:bread", 1.0);
        for (String block : new String[] {"duna_sands", "eve_silt", "laythe_silt"}) {
            smelt(NS, "smelting/glass_from_" + block, ns(block), "minecraft:glass", 0.1);
        }
        smelt(NS, "smelting/glass_from_moon_turf", "hbm:moon_turf", "minecraft:glass", 0.1);
        smelt(NS, "smelting/duna_rock_from_duna_cobble", ns("duna_cobble"), ns("duna_rock"), 0.1);
        smelt(NS, "smelting/stone_from_dres_rock", ns("dres_rock"), "minecraft:stone", 0.1);
        smelt(NS, "smelting/ike_stone_from_ike_regolith", ns("ike_regolith"), ns("ike_stone"), 0.1);
        smelt(NS, "smelting/stone_from_eve_rock", ns("eve_rock"), "minecraft:stone", 0.1);
        smelt(NS, "smelting/moho_stone_from_moho_regolith", ns("moho_regolith"), ns("moho_stone"), 0.1);
        smelt(NS, "smelting/stone_from_moon_rock", ns("moon_rock"), "minecraft:stone", 0.1);
        for (String p : new String[] {"nickel", "zinc", "gallium"}) {
            smelt(NS, "smelting/ingot_" + p + "_from_powder", ns("powder_" + p), ns("ingot_" + p), 1.0);
        }
        smelt(NS, "smelting/ingot_rubber_from_powder_rubber", ns("powder_rubber"), "hbm:ingot_rubber", 1.0);
        smelt(NS, "smelting/grilled_flesh", ns("flesh_wafer"), ns("grilled_flesh"), 1.0);
        smelt(NS, "smelting/bean_roast", ns("bean_raw"), ns("bean_roast"), 1.0);
        smelt(NS, "smelting/ingot_nickel_from_crystal", ns("crystal_nickel"), ns("ingot_nickel"), 2.0, 2);
        smelt(NS, "smelting/ingot_niobium_from_crystal", ns("crystal_niobium"), "hbm:ingot_niobium", 2.0, 2);
        smelt(NS, "smelting/ingot_zinc_from_crystal", ns("crystal_zinc"), ns("ingot_zinc"), 2.0, 2);
    }

    private static void oreSmelt(String ore, String result, double experience) {
        smelt(NS, "smelting/" + ore, "#ntmspace:ores/" + ore, result, experience);
    }

    private static String ns(String path) {
        return NS + ":" + path;
    }
}
