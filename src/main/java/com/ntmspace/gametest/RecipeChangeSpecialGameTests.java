package com.ntmspace.gametest;

import com.hbm.api.data.HazardDataMaps;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.IArmorModDash;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ItemModInsert;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.util.Function.FunctionLinear;
import com.hbm.util.Function.FunctionSqrt;
import com.ntmspace.atmosphere.Sealing;
import com.ntmspace.registry.SpaceSpecialItems;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * hbm-space's special items ({@code registry/SpaceSpecialItems}) against hbm-space's {@code ModItems} values, and their
 * crafting rows ({@code ArmorRecipes} flippers/heavy_boots, {@code ToolRecipes} atmosphere_scanner, {@code RodRecipes}
 * RBMK rods and Watz pellets, {@code MineralRecipes} depleted americium) crafted through the server's recipe manager in
 * the 1.7 shapes. Expected values are written from the hbm-space source.
 */
public final class RecipeChangeSpecialGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function ITEMS = TESTS.function("recipe_change_special_items", 20,
            () -> RecipeChangeSpecialGameTests::items);
    public static final GameTests.Function RBMK = TESTS.function("recipe_change_special_rbmk", 20,
            () -> RecipeChangeSpecialGameTests::rbmk);
    public static final GameTests.Function RTG = TESTS.function("recipe_change_special_rtg", 20,
            () -> RecipeChangeSpecialGameTests::rtg);
    public static final GameTests.Function WATZ = TESTS.function("recipe_change_special_watz", 20,
            () -> RecipeChangeSpecialGameTests::watz);
    public static final GameTests.Function HAZARDS = TESTS.function("recipe_change_special_hazards", 20,
            () -> RecipeChangeSpecialGameTests::hazards);
    public static final GameTests.Function CRAFTING = TESTS.function("recipe_change_special_crafting", 20,
            () -> RecipeChangeSpecialGameTests::crafting);

    private RecipeChangeSpecialGameTests() {}

    public static void init() {}

    // ------------------------------------------------------------------------------------------------ items

    private static void items(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // armour modules: slot and applicable pieces (1.7 ItemArmorMod constructor arguments)
        module(errors, "flippers", ArmorModHandler.boots_only, false, false, false, true);
        module(errors, "heavy_boots", ArmorModHandler.boots_only, false, false, false, true);
        module(errors, "v1", ArmorModHandler.plate_only, false, true, false, false);
        module(errors, "insert_cmb", ArmorModHandler.kevlar, false, true, false, false);
        if (item("v1") instanceof IArmorModDash dash) {
            if (dash.getDashes() != 3) errors.add("v1 dashes " + dash.getDashes());
            List<ItemArmorMod.Modifier> mods = new ArrayList<>();
            ((ItemArmorMod) item("v1")).collectModifiers(ItemStack.EMPTY, ArmorSlot.CHESTPLATE, mods);
            if (!mods.equals(List.of(new ItemArmorMod.Modifier(ItemArmorMod.Modifier.MOVEMENT_SPEED, 0.5D, Attributes.Op.MULTIPLY_TOTAL)))) {
                errors.add("v1 modifiers " + mods);
            }
        } else errors.add("v1 is no IArmorModDash");
        // insert_cmb: ItemModInsert(9999, 0.7, 0.9, 0.4, 1).withCorrosionProtection()
        if (item("insert_cmb") instanceof ItemModInsert insert) {
            ItemStack stack = new ItemStack(insert);
            if (stack.getMaxDamage() != 9999) errors.add("insert_cmb durability " + stack.getMaxDamage());
            List<ItemArmorMod.Modifier> mods = new ArrayList<>();
            insert.collectModifiers(stack, ArmorSlot.CHESTPLATE, mods);
            if (!mods.isEmpty()) errors.add("insert_cmb has speed modifiers " + mods);
            if (!stack.is(Sealing.CORROSION_PROTECTION)) errors.add("insert_cmb is not corrosion protection");
        }
        // tools and misc
        expectStack(errors, "wand_time", 1);
        expectStack(errors, "atmosphere_scanner", 1);
        expectStack(errors, "pellet_rtg_berkelium", 1);
        if (!(item("oxy_pinwheel") instanceof IInfiniteFluidItem pinwheel) || pinwheel.infiniteType() != Fluids.OXYGEN
                || pinwheel.infiniteAmount() != 1 || pinwheel.infiniteChance() != 1 || pinwheel.allowPressure(1)) {
            errors.add("oxy_pinwheel is not ItemInfiniteFluid(OXYGEN, 1)");
        }
        if (!(item("laser_crystal_iron") instanceof ItemFELCrystal crystal) || crystal.wavelength != SpaceSpecialItems.XRAY) {
            errors.add("laser_crystal_iron is not an X-ray crystal");
        }
        // hbm-space EnumWavelengths ordinals: NULL 0, IR 1, VISIBLE 2, UV 3, XRAY 4, GAMMA 5, DRX 6 (1.7: GAMMA 4, DRX 5)
        ItemFELCrystal.EnumWavelengths[] order = {ItemFELCrystal.EnumWavelengths.NULL, ItemFELCrystal.EnumWavelengths.IR,
                ItemFELCrystal.EnumWavelengths.VISIBLE, ItemFELCrystal.EnumWavelengths.UV, SpaceSpecialItems.XRAY,
                ItemFELCrystal.EnumWavelengths.GAMMA, ItemFELCrystal.EnumWavelengths.DRX};
        for (int i = 0; i < order.length; i++) {
            if (order[i].strength() != i) errors.add(order[i] + " strength " + order[i].strength() + ", hbm-space ordinal " + i);
            // hbm-space TileEntityFEL: powerReq * 3^ordinal (0 for NULL)
            int power = (int) (com.hbm.tileentity.machine.TileEntityFEL.powerReq * (i == 0 ? 0 : Math.pow(3, i)));
            if (com.hbm.tileentity.machine.TileEntityFEL.requiredPower(order[i]) != power) {
                errors.add(order[i] + " FEL draw " + com.hbm.tileentity.machine.TileEntityFEL.requiredPower(order[i]) + ", hbm-space " + power);
            }
        }
        if (!(item("butter_sword").getDefaultInstance().getMaxDamage() == 59)) {
            errors.add("butter_sword durability " + item("butter_sword").getDefaultInstance().getMaxDamage() + " (1.7 WOOD: 59)");
        }
        for (String record : new String[] {"record_gs", "record_gp", "record_el"}) {
            if (item(record) == Items.AIR || item(record).getDefaultInstance().getMaxStackSize() != 1) errors.add(record + " missing");
        }
        if (dev.golem.core.world.ItemStacks.foodProperties(item("animan").getDefaultInstance()) == null) errors.add("animan is not edible");
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(Ids.of("ntmspace", "run"))) errors.add("effect ntmspace:run missing");
        finish(helper, errors);
    }

    private static void module(List<String> errors, String id, int type, boolean helmet, boolean chest, boolean legs, boolean boots) {
        if (!(item(id) instanceof ItemArmorMod mod)) {
            errors.add(id + " is no armour module");
            return;
        }
        if (mod.type != type || mod.helmet != helmet || mod.chestplate != chest || mod.leggings != legs || mod.boots != boots) {
            errors.add(id + " slot/pieces " + mod.type + " " + mod.helmet + mod.chestplate + mod.leggings + mod.boots);
        }
    }

    private static void expectStack(List<String> errors, String id, int size) {
        int actual = item(id).getDefaultInstance().getMaxStackSize();
        if (actual != size) errors.add(id + " stack size " + actual);
    }

    // ------------------------------------------------------------------------------------------------ RBMK

    private static void rbmk(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // id, yield, reactivity, selfRate, function, depletion, heat, melting point, diffusion, pellet, pellet name
        rod(errors, "bk247", 100_000_000D, 50, 0, "LINEAR", "GENTLE_SLOPE", 2D, 2993, 0.02D, "Highly Enriched Berkelium-247");
        rod(errors, "lecm", 30_000_000D, 20, 5, "SLOW_LINEAR", "CF_SLOPE", 1.25D, 1340, 0.4D, "Low Enriched Curium-245");
        rod(errors, "mecm", 27_000_000D, 30, 10, "SLOW_LINEAR", "CF_SLOPE", 1.25D, 1720, 0.3D, "Medium Enriched Curium-245");
        rod(errors, "hecm", 24_000_000D, 60, 25, "SLOW_LINEAR", "CF_SLOPE", 1.3D, 1880, 0.3D, "Highly Enriched Curium-245");

        // hbm-space ItemRBMKRod: SLOW_LINEAR = sqrt(2 * flux + 30) / 10 * reactivity / 2.5; CF_SLOPE = e + sin(e * PI) * 1.4
        double flux = 123.4D, reactivity = 30D;
        double slow = SpaceSpecialItems.SLOW_LINEAR.apply(flux, 1D, reactivity, 10D);
        if (slow != Math.sqrt(2 * flux + 30) / 10 * reactivity / 2.5) errors.add("SLOW_LINEAR " + slow);
        for (double e : new double[] {0D, 0.4D, 0.6D, 1D}) {
            double cf = SpaceSpecialItems.CF_SLOPE.apply(e);
            if (cf != e + (Math.sin(e * Math.PI)) * 1.4) errors.add("CF_SLOPE(" + e + ") " + cf);
        }
        if (Math.abs(SpaceSpecialItems.CF_SLOPE.apply(0.6D) - 1.931D) > 0.001D) errors.add("CF_SLOPE peak (1.7 comment: ~193%)");
        if (rbmkRod("lecf") != null || item("rbmk_pellet_lecf") != Items.AIR) errors.add("rbmk lecf exists (1.7 never registered it)");
        finish(helper, errors);
    }

    private static ItemRBMKRod rbmkRod(String type) {
        return item("rbmk_fuel_" + type) instanceof ItemRBMKRod rod ? rod : null;
    }

    private static void rod(List<String> errors, String type, double yield, double reactivity, double selfRate, String function,
                            String depletion, double heat, double melt, double diffusion, String name) {
        ItemRBMKRod rod = rbmkRod(type);
        if (rod == null) {
            errors.add("rbmk_fuel_" + type + " missing");
            return;
        }
        ReactorDataMaps.RBMKFuel f = rod.stats();
        if (f.yield() != yield || f.reactivity() != reactivity || f.selfRate() != selfRate || !f.function().name().equals(function)
                || !f.depletion().name().equals(depletion) || f.heat() != heat || f.meltingPoint() != melt || f.diffusion() != diffusion
                || f.xenonGen() != 0.5D || f.xenonBurn() != 50D || f.heatCoeffStart() != 0D
                || f.neutronIn() != ItemRBMKRod.NType.SLOW || f.neutronOut() != ItemRBMKRod.NType.FAST) {
            errors.add("rbmk_fuel_" + type + " stats " + f);
        }
        if (!rod.fullName.equals(name)) errors.add("rbmk_fuel_" + type + " name " + rod.fullName);
        if (new ItemStack(rod).getMaxStackSize() != 1) errors.add("rbmk_fuel_" + type + " stacks");
        Item pellet = item("rbmk_pellet_" + type);
        if (!(pellet instanceof ItemRBMKPellet p) || !p.fullName.equals(name) || !p.isXenonEnabled()) {
            errors.add("rbmk_pellet_" + type + " wrong");
        } else if (rod.pelletItem() != pellet) errors.add("rbmk_fuel_" + type + " recycles into " + rod.pelletItem());
    }

    // ------------------------------------------------------------------------------------------------ RTG

    private static void rtg(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        boolean decay = com.hbm.config.MachineConfig.rtgDecay();
        // 1.7: berkelium ItemRTGPellet(20), lifespan (long)(getLifespan(13.8F, LONG, false) * 1.5) = 9,936,000,000;
        // cf251/cf252 ItemRTGPellet(rtgDecay ? 600 : 200), lifespan (long)(getLifespan(1F, SHORT, false) * 2.5) = 120,000
        pellet(errors, "pellet_rtg_berkelium", 20, 9_936_000_000L, "ntmspace:pellet_rtg_depleted_americium");
        pellet(errors, "pellet_rtg_cf251", decay ? 600 : 200, 120_000L, "hbm:pellet_rtg_depleted_lead");
        pellet(errors, "pellet_rtg_cf252", decay ? 600 : 200, 120_000L, "hbm:pellet_rtg_depleted_lead");
        // hbm-space's (= 1.7's) model: getScaledPower = ceil(heat * life / maxLife), linear
        if (item("pellet_rtg_berkelium") instanceof ItemRTGPellet berkelium) {
            ItemStack stack = new ItemStack(berkelium);
            berkelium.setLifespan(stack, berkelium.getMaxLifespan() / 4);
            if (ItemRTGPellet.getScaledPower(berkelium, stack) != 5) {
                errors.add("berkelium at a quarter life gives " + ItemRTGPellet.getScaledPower(berkelium, stack) + ", hbm-space 5");
            }
        }
        finish(helper, errors);
    }

    private static void pellet(List<String> errors, String id, int heat, long lifespan, String decaysTo) {
        if (!(item(id) instanceof ItemRTGPellet pellet)) {
            errors.add(id + " missing");
            return;
        }
        if (pellet.getHeat() != heat) errors.add(id + " heat " + pellet.getHeat());
        if (pellet.getMaxLifespan() != lifespan) errors.add(id + " lifespan " + pellet.getMaxLifespan());
        String decay = String.valueOf(BuiltInRegistries.ITEM.getKey(pellet.getDecayItem()));
        if (!decay.equals(decaysTo)) errors.add(id + " decays to " + decay);
    }

    // ------------------------------------------------------------------------------------------------ Watz

    private static void watz(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // hbm-space EnumWatzType: light, dark, passive, heat, mud (constructor halves it), burn, heat divisor
        watz(errors, "pu241", 0x78817E, 394240, 1950, 25, 0.0025D, 1.30D, 2.66D / 18D, 24D * 24D);
        watz(errors, "amrg", 0x93767B, 0x66474D, 2888, 48, 0.0035D, 1.33D, 4.33D / 25.5D, 28D * 28D);
        watz(errors, "amf", 0x93767B, 0x66474D, 2333, 44, 0.003D, 1.33D, 4.11D / 22.2D, 27D * 27D);
        watz(errors, "cmrg", 0xD8C2C4, 0xAD9799, 2999, 50, 0.005D, 1.5D, 5.5D / 25.5D, 30D * 28D);
        watz(errors, "cmf", 0xD8C2C4, 0xAD9799, 2444, 48, 0.0045D, 1.8D, 5.0D / 20D, 26D * 24D);
        watz(errors, "bk247", 0xC2C9C7, 0x8D9592, 3000, 55, 0.012D, 1.5D, 6.0D / 23.5D, 10D * 10D);
        watz(errors, "cf251", 0x7879B4, 0x4D4E89, 1250, 60, 0.001D, 1.7D, 6.65D / 23.5D, 10D * 10D);
        watz(errors, "cf252", 0x7879B4, 0x4D4E89, 1050, 120, 0.0015D, 1.8D, 8.85D / 28.8D, 10D * 10D);
        watz(errors, "es253", 0xB9BFB2, 0x594E44, 3750, 70, 0.0001D, 1.3D, 7.0D / 27.7D, 10D * 10D);
        // hbm-space doubled every pellet's yield (1.7 5e8): with the addon, HBM's twelve carry 1e9 as well
        int hbm = 0;
        for (var entry : ReactorDataMaps.WATZ_PELLET.byId().entrySet()) {
            if (!entry.getKey().startsWith("hbm:")) continue;
            hbm++;
            if (entry.getValue().yield() != 1_000_000_000D) errors.add(entry.getKey() + " yield " + entry.getValue().yield());
        }
        if (hbm != 12) errors.add(hbm + " HBM Watz pellets, 1.7 has 12");
        if (BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "watz_pellet_schrabidium")).map(i -> ItemWatzPellet.stats(i))
                .map(st -> st.passive() != 2000D || st.heat() != 20D).orElse(true)) {
            errors.add("hbm:watz_pellet_schrabidium lost its 1.7 values in the yield patch");
        }
        finish(helper, errors);
    }

    private static void watz(List<String> errors, String type, int light, int dark, double passive, double heat, double mud,
                             double burn, double divLevel, double divOff) {
        Item fresh = item("watz_pellet_" + type);
        Item depleted = item("watz_pellet_depleted_" + type);
        ReactorDataMaps.WatzPellet stats = ItemWatzPellet.stats(fresh);
        if (stats == null || !(depleted instanceof ItemWatzPellet spent) || !spent.depleted) {
            errors.add("watz_pellet_" + type + " missing");
            return;
        }
        if (ItemWatzPellet.freshOf(depleted) != fresh) errors.add("watz_pellet_depleted_" + type + " not paired");
        if (stats.color() != light || stats.colorDark() != dark || stats.passive() != passive || stats.heat() != heat
                || stats.mud() != mud / 2D || stats.yield() != 1_000_000_000D || stats.absorb().isPresent()) {
            errors.add("watz_pellet_" + type + " values " + stats);
        }
        for (double x : new double[] {0D, 37.5D, 1000D}) {
            double expectBurn = new FunctionLinear(burn).effonix(x);
            double expectDiv = new FunctionSqrt(divLevel).withOff(divOff).effonix(x);
            if (stats.burn().map(f -> f.effonix(x)).orElse(Double.NaN) != expectBurn) errors.add("watz_pellet_" + type + " burn(" + x + ")");
            if (stats.heatDiv().map(f -> f.effonix(x)).orElse(Double.NaN) != expectDiv) errors.add("watz_pellet_" + type + " heat_div(" + x + ")");
        }
    }

    // ------------------------------------------------------------------------------------------------ hazards

    private static void hazards(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (String id : new String[] {"rbmk_fuel_bk247", "rbmk_fuel_lecm", "rbmk_fuel_mecm", "rbmk_fuel_hecm", "rbmk_pellet_bk247",
                "rbmk_pellet_lecm", "rbmk_pellet_mecm", "rbmk_pellet_hecm", "pellet_rtg_depleted_americium", "watz_pellet_pu241",
                "watz_pellet_amrg", "watz_pellet_cmrg", "watz_pellet_cmf", "watz_pellet_bk247", "watz_pellet_cf252", "watz_pellet_es253"}) {
            if (HazardDataMaps.HAZARDS.get(item(id)) == null) errors.add(id + " has no hbm:hazards entry");
        }
        // 1.7 registers none for these
        for (String id : new String[] {"watz_pellet_amf", "watz_pellet_cf251", "pellet_rtg_berkelium"}) {
            if (HazardDataMaps.HAZARDS.get(item(id)) != null) errors.add(id + " has a hazard 1.7 did not give it");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ crafting

    private static void crafting(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ItemStack rubber = stack("hbm:ingot_rubber"), steel = stack("hbm:ingot_steel"), leather = new ItemStack(Items.LEATHER);
        ItemStack e = ItemStack.EMPTY;
        // ArmorRecipes: flippers "R R" / "R R"; heavy boots "L L" / "S S"
        craft(helper, errors, 3, 2, java.util.Arrays.asList(rubber, e, rubber, rubber, e, rubber), "ntmspace:flippers", 1);
        craft(helper, errors, 3, 2, java.util.Arrays.asList(leather, e, leather, steel, e, steel), "ntmspace:heavy_boots", 1);
        // ToolRecipes: atmosphere scanner "QCQ" / "WBW" / "SSS"
        ItemStack q = stack("hbm:glass_quartz"), c = stack("hbm:circuit_basic"), w = stack("hbm:wire_fine_gold"),
                b = stack("hbm:circuit_controller_chassis"), s = stack("ntmspace:plate_stainless");
        craft(helper, errors, 3, 3, java.util.Arrays.asList(q, c, q, w, b, w, s, s, s), "ntmspace:atmosphere_scanner", 1);
        // RodRecipes.addRBMKRod: an empty rod and eight billets, shapeless
        rodRecipe(helper, errors, "ntmspace:billet_bk247", "ntmspace:rbmk_fuel_bk247");
        rodRecipe(helper, errors, "ntmspace:billet_cm_fuel", "ntmspace:rbmk_fuel_lecm");
        rodRecipe(helper, errors, "ntmspace:billet_cm_mix", "ntmspace:rbmk_fuel_mecm");
        rodRecipe(helper, errors, "ntmspace:billet_cm245", "ntmspace:rbmk_fuel_hecm");
        // RodRecipes.addPellet: " I " / "IGI" / " I ", G = graphite
        String[][] pellets = {{"hbm:ingot_pu241", "pu241"}, {"hbm:ingot_americium_fuel", "amf"}, {"hbm:ingot_am_mix", "amrg"},
                {"ntmspace:ingot_cm_mix", "cmrg"}, {"ntmspace:ingot_cm_fuel", "cmf"}, {"ntmspace:ingot_bk247", "bk247"},
                {"ntmspace:ingot_cf252", "cf252"}, {"ntmspace:ingot_cf251", "cf251"}, {"ntmspace:ingot_es253", "es253"}};
        ItemStack g = stack("hbm:ingot_graphite");
        for (String[] p : pellets) {
            ItemStack i = stack(p[0]);
            craft(helper, errors, 3, 3, java.util.Arrays.asList(e, i, e, i, g, i, e, i, e), "ntmspace:watz_pellet_" + p[1], 1);
        }
        // MineralRecipes: three reactor-grade americium billets from a decayed americium RTG pellet
        craft(helper, errors, 1, 1, java.util.Arrays.asList(stack("ntmspace:pellet_rtg_depleted_americium")), "hbm:billet_am_mix", 3);
        // 1.7 setContainerItem(plate_iron): the decayed pellet leaves its iron plate in the grid
        ItemStack depleted = stack("ntmspace:pellet_rtg_depleted_americium");
        if (depleted != null) {
            List<ItemStack> rest = Recipes.craftingRemainders(helper.getLevel(), 1, 1, List.of(depleted)).orElse(List.of());
            if (rest.isEmpty() || !rest.get(0).is(com.hbm.items.ModItems.plate_iron.get())) {
                errors.add("depleted americium pellet leaves no iron plate: " + rest);
            }
        }
        finish(helper, errors);
    }

    private static void rodRecipe(GameTestHelper helper, List<String> errors, String billet, String rod) {
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(8, stack(billet)));
        grid.add(4, stack("hbm:rbmk_fuel_empty"));
        craft(helper, errors, 3, 3, grid, rod, 1);
    }

    private static void craft(GameTestHelper helper, List<String> errors, int w, int h, List<ItemStack> grid, String output, int count) {
        if (grid.stream().anyMatch(stack -> stack == null)) {
            errors.add(output + ": an ingredient item is not registered");
            return;
        }
        Optional<ItemStack> result = Recipes.craftingResult(helper.getLevel(), w, h, grid);
        if (result.isEmpty() || !String.valueOf(BuiltInRegistries.ITEM.getKey(result.get().getItem())).equals(output)
                || result.get().getCount() != count) {
            errors.add(output + " x" + count + " not crafted (got " + result.map(ItemStack::toString).orElse("nothing") + ")");
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(Ids.of("ntmspace", path));
    }

    /** A stack of a registered item, or null when the id is unknown. */
    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        return item == Items.AIR ? null : new ItemStack(item);
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
            return;
        }
        helper.succeed();
    }
}
