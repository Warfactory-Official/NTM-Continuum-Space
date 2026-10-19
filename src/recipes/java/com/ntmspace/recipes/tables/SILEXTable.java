package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.Row;
import com.hbm.inventory.recipes.SILEXRecipes.VariantStack;
import com.hbm.inventory.recipes.SILEXRecipes.WeightedOut;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.ntmspace.registry.SpaceSpecialItems;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code SILEXRecipes.register}: the rows hbm-space adds to or changes in HBM's table. The changed rows (the
 * LEP, MEP, HEP-239 and HEP-241 pellet stages that gained a curium output) replace HBM's under the same id and take HBM's
 * {@code order} (HBM's rows are removed by {@code recipe_removals/silex.json}); the added rows take the order of the row
 * they follow in hbm-space, both looked up from HBM's loaded table. hbm-space's {@code new SILEXRecipe(produced, consumed, int)} is
 * {@code EnumWavelengths.values()[int]} with {@code XRAY} inserted between {@code UV} and {@code GAMMA} (see
 * {@link #WAVELENGTHS}). Outputs of weight 0 can never be rolled by 1.7's {@code WeightedRandom} and HBM's table rejects
 * them, so they are left out; a row whose only change is such an output (stage 0 and 5, every PU238BE stage) stays HBM's.
 * The rows are hbm-space's waste classes as items: HBM's {@code nuclear_waste_short*_<class>}, NTM: Space's for the
 * classes it added ({@link #SPACE_WASTE}). Replaces the former {@code gen_recipe_changes_nuclear.py}'s {@code gen_silex}.
 */
public final class SILEXTable extends RecipeSource<Row> {

    /** hbm-space's {@code EnumWavelengths.values()}: NULL, IR, VISIBLE, UV, XRAY, GAMMA, DRX. */
    private static final EnumWavelengths[] WAVELENGTHS = {EnumWavelengths.NULL, EnumWavelengths.IR, EnumWavelengths.VISIBLE,
            EnumWavelengths.UV, SpaceSpecialItems.XRAY, EnumWavelengths.GAMMA, EnumWavelengths.DRX};

    /** hbm-space's {@code ItemWasteShort.WasteClass} members that are NTM: Space's items (SpaceWasteItems). */
    private static final Set<String> SPACE_WASTE = Set.of("americium241", "berkelium247", "curium244", "curium245");

    public SILEXTable() {
        super(SILEXRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // hbm-space puts ingot_cm_mix twice with the same key and values; the second wins, identical. After ingot_am_mix.
        put("ingot_cm_mix", comp("ntmspace:ingot_cm_mix"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm244"), 3)
                .addOut(stack("ntmspace:nugget_cm245"), 6), after("hbm:silex/ingot_am_mix"));

        for (int i = 0; i < 5; i++) {

            // the stage-0 and stage-5 rows below give curium weight 0 (2 / 50 + 0): unchanged, HBM's rows stay
            if (i > 0) {
                // LEP //
                put(pellet("hbm:rbmk_pellet_lep", i), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:nugget_plutonium_fuel"), 84 - i * 14)
                        .addOut(i < 1 ? stack("hbm:nugget_pu239") : stack("hbm:nugget_pu_mix"), 6 + i * 2)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 2)
                        .addOut(shortTiny("plutonium239"), 7 + 8 * i)
                        .addOut(shortTiny("plutonium240"), 3 + 4 * i));

                put(pellet("hbm:rbmk_pellet_lep", i + 5), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:powder_xe135_tiny"), 1)
                        .addOut(stack("hbm:nugget_plutonium_fuel"), 83 - i * 14)
                        .addOut(i < 1 ? stack("hbm:nugget_pu239") : stack("hbm:nugget_pu_mix"), 6 + i * 2)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 2)
                        .addOut(shortTiny("plutonium239"), 7 + 8 * i)
                        .addOut(shortTiny("plutonium240"), 3 + 4 * i));

                // MEP //
                put(pellet("hbm:rbmk_pellet_mep", i), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:nugget_pu_mix"), 85 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 3)
                        .addOut(shortTiny("plutonium239"), 10 + 10 * i)
                        .addOut(shortTiny("plutonium240"), 5 + 5 * i));

                put(pellet("hbm:rbmk_pellet_mep", i + 5), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:powder_xe135_tiny"), 1)
                        .addOut(stack("hbm:nugget_pu_mix"), 84 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 3)
                        .addOut(shortTiny("plutonium239"), 10 + 10 * i)
                        .addOut(shortTiny("plutonium240"), 5 + 5 * i));

                // HEP239 // (HBM's rbmk_pellet_hep)
                put(pellet("hbm:rbmk_pellet_hep", i), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:nugget_pu239"), 85 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 4)
                        .addOut(shortTiny("plutonium239"), 15 + 20 * i));

                put(pellet("hbm:rbmk_pellet_hep", i + 5), new SILEXRecipe(600, 100, 1)
                        .addOut(stack("hbm:powder_xe135_tiny"), 1)
                        .addOut(stack("hbm:nugget_pu239"), 84 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 4)
                        .addOut(shortTiny("plutonium239"), 15 + 20 * i));

                // HEP241 //
                put(pellet("hbm:rbmk_pellet_hep241", i), new SILEXRecipe(600, 100, 2)
                        .addOut(stack("hbm:nugget_pu241"), 85 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 4 + 1 / 2)
                        .addOut(shortTiny("plutonium241"), 15 + 20 * i));

                put(pellet("hbm:rbmk_pellet_hep241", i + 5), new SILEXRecipe(600, 100, 2)
                        .addOut(stack("hbm:powder_xe135_tiny"), 1)
                        .addOut(stack("hbm:nugget_pu241"), 84 - i * 20)
                        .addOut(stack("ntmspace:nugget_cm_mix"), 2 / 50 + i * 4 + 1 / 2)
                        .addOut(shortTiny("plutonium241"), 15 + 20 * i));
            }

            // PU238BE: `2 / 50 * i * 2` is always 0, unchanged. DRX: hbm-space's tier 5 is 1.7's tier 4, unchanged.

            // the added families, after ZFB RG-AM (HBM's rbmk_pellet_zfb_am_mix_<i + 5>) in the same iteration
            int order = after("hbm:silex/rbmk_pellet_zfb_am_mix_" + (i + 5));

            //LEA//
            put(pellet("hbm:rbmk_pellet_lea", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_americium_fuel"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 90)
                    .addOut(shortTiny("americium242"), 1 + 3 * (i / 2))
                    .addOut(shortTiny("americium241"), 1 + (2 + 1 / 2) * (i / 2)), order);

            put(pellet("hbm:rbmk_pellet_lea", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_americium_fuel"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 90)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("americium242"), 1 + 3 * (i / 2))
                    .addOut(shortTiny("americium241"), 1 + (2 + 1 / 2) * (i / 2)), order);
            //MEA//
            put(pellet("hbm:rbmk_pellet_mea", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am_mix"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 70)
                    .addOut(shortTiny("americium242"), 2 + 3 * i)
                    .addOut(shortTiny("americium241"), 2 + 2 * i), order);

            put(pellet("hbm:rbmk_pellet_mea", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am_mix"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 70)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("americium242"), 2 + 3 * i)
                    .addOut(shortTiny("americium241"), 2 + 2 * i), order);
            //HEA242//
            put(pellet("hbm:rbmk_pellet_hea242", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am242"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 50)
                    .addOut(shortTiny("americium242"), 4 + 6 * i), order);

            put(pellet("hbm:rbmk_pellet_hea242", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am242"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 50)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("americium242"), 4 + 6 * i), order);
            //HEA241//
            put(pellet("hbm:rbmk_pellet_hea241", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am241"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 50)
                    .addOut(shortTiny("americium241"), 4 + 6 * i), order);

            put(pellet("hbm:rbmk_pellet_hea241", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("hbm:nugget_am241"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_bk247"), 1 + 10 * i / 50)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("americium241"), 4 + 6 * i), order);
            //BK247//
            put(pellet("ntmspace:rbmk_pellet_bk247", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_bk247"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cf251"), 1 + 10 * i / 40)
                    .addOut(shortTiny("berkelium247"), 4 + 6 * i), order);

            put(pellet("ntmspace:rbmk_pellet_bk247", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_bk247"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cf251"), 1 + 10 * i / 40)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("berkelium247"), 4 + 6 * i), order);
            //LEC//
            put(pellet("ntmspace:rbmk_pellet_lecm", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm_fuel"), 100 - i * 20)
                    .addOut(stack("hbm:nugget_pu_mix"), 50 - i * 10)
                    .addOut(stack("ntmspace:nugget_cm247"), 1 + 2 * i / 50)
                    .addOut(shortTiny("curium244"), 2 + 1 * i)
                    .addOut(shortTiny("curium245"), 2 + 1 * i), order);

            put(pellet("ntmspace:rbmk_pellet_lecm", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm_fuel"), 100 - i * 20)
                    .addOut(stack("hbm:nugget_pu_mix"), 50 - i * 10)
                    .addOut(stack("ntmspace:nugget_cm247"), 1 + 2 * i / 50)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("curium244"), 2 + 1 * i)
                    .addOut(shortTiny("curium245"), 2 + 1 * i), order);
            //MEC//
            put(pellet("ntmspace:rbmk_pellet_mecm", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm_mix"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cm247"), 2 + 4 * i / 30)
                    .addOut(shortTiny("curium244"), 4 + 4 * i)
                    .addOut(shortTiny("curium245"), 5 + 4 * i), order);

            put(pellet("ntmspace:rbmk_pellet_mecm", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm_mix"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cm247"), 2 + 4 * i / 30)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("curium244"), 4 + 4 * i)
                    .addOut(shortTiny("curium245"), 5 + 4 * i), order);
            //HEC//
            put(pellet("ntmspace:rbmk_pellet_hecm", i), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm245"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cm247"), 3 + 5 * i / 30)
                    .addOut(shortTiny("curium244"), 6 + 7 * i)
                    .addOut(shortTiny("curium245"), 7 + 8 * i), order);

            put(pellet("ntmspace:rbmk_pellet_hecm", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(stack("ntmspace:nugget_cm245"), 100 - i * 20)
                    .addOut(stack("ntmspace:nugget_cm247"), 3 + 5 * i / 30)
                    .addOut(stack("hbm:powder_xe135_tiny"), 3)
                    .addOut(shortTiny("curium244"), 6 + 7 * i)
                    .addOut(shortTiny("curium245"), 7 + 8 * i), order);
        }

        // nuclear waste reprocessing, after HBM's nuclear_waste_short_depleted_schrabidium
        int order = after("hbm:silex/nuclear_waste_short_depleted_schrabidium");
        put(shortWaste("americium241"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 40)
                .addOut(stack("hbm:nugget_pu239"), 10)
                .addOut(stack("hbm:powder_cs137_tiny"), 5)
                .addOut(stack("hbm:powder_i131_tiny"), 5)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10)
                .addOut(stack("hbm:nugget_am242"), 30), order);
        put(shortDepleted("americium241"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 50)
                .addOut(stack("hbm:nugget_pu239"), 20)
                .addOut(stack("hbm:nuclear_waste_tiny"), 20)
                .addOut(stack("ntmspace:nugget_cm242"), 10), order);
        put(shortWaste("americium242"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 70)
                .addOut(stack("hbm:nugget_pu239"), 10)
                .addOut(stack("hbm:powder_cs137_tiny"), 5)
                .addOut(stack("hbm:powder_i131_tiny"), 5)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10), order);
        put(shortDepleted("americium242"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 50)
                .addOut(stack("hbm:nugget_pu239"), 20)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10)
                .addOut(stack("ntmspace:nugget_cm242"), 20), order);
        put(shortWaste("berkelium247"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cf251"), 40)
                .addOut(stack("hbm:nugget_am_mix"), 10)
                .addOut(stack("hbm:powder_cs137_tiny"), 5)
                .addOut(stack("hbm:powder_sr90_tiny"), 5)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10), order);

        put(shortDepleted("berkelium247"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 50)
                .addOut(stack("ntmspace:nugget_cf251"), 20)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10)
                .addOut(stack("hbm:nugget_am_mix"), 20), order);

        put(shortWaste("curium244"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm245"), 30)
                .addOut(stack("ntmspace:nugget_cm246"), 15)
                .addOut(stack("ntmspace:nugget_cm247"), 10)
                .addOut(stack("ntmspace:nugget_cf251"), 25)
                .addOut(stack("ntmspace:nugget_cf252"), 20)
                .addOut(stack("ntmspace:nugget_es253"), 10), order);
        put(shortDepleted("curium244"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 40) //from short-lived californium isotope decay
                .addOut(stack("ntmspace:nugget_cm246"), 5)
                .addOut(stack("ntmspace:nugget_cm247"), 15)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10)
                .addOut(stack("ntmspace:nugget_cf251"), 20)
                .addOut(stack("ntmspace:nugget_cf252"), 10), order);
        put(shortWaste("curium245"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm246"), 15)
                .addOut(stack("ntmspace:nugget_cm247"), 10)
                .addOut(stack("ntmspace:nugget_cf251"), 35)
                .addOut(stack("ntmspace:nugget_cf252"), 30)
                .addOut(stack("ntmspace:nugget_es253"), 10), order);
        put(shortDepleted("curium245"), new SILEXRecipe(900, 100, 2)
                .addOut(stack("ntmspace:nugget_cm_mix"), 10) //from short-lived californium isotope decay
                .addOut(stack("ntmspace:nugget_cm246"), 5)
                .addOut(stack("ntmspace:nugget_cm247"), 15)
                .addOut(stack("hbm:nuclear_waste_tiny"), 10)
                .addOut(stack("ntmspace:nugget_cf252"), 15)
                .addOut(stack("ntmspace:nugget_cf251"), 25), order);
    }

    /** hbm-space {@code recipes.put(input, recipe)} of a changed row: it replaces HBM's row of the same id at its order. */
    private void put(AStack input, SILEXRecipe recipe) {
        remove("hbm:silex/" + id(input));
        put(input, recipe, after("hbm:silex/" + id(input)));
    }

    /** hbm-space {@code recipes.put(input, recipe)}; the id is the input item, {@code <pellet>_<stage>} for a pellet. */
    private void put(AStack input, SILEXRecipe recipe, int order) {
        put(id(input), input, recipe, order);
    }

    private static String id(AStack input) {
        return input instanceof VariantStack pellet ? path(pellet.item) + "_" + pellet.variant : path(((ComparableStack) input).item);
    }

    /** The order of the HBM row a row follows (or replaces): the stable sort places it right after (ties sort by id). */
    private int after(String recipeId) {
        return loaded(recipeId).order();
    }

    private void put(String id, AStack input, SILEXRecipe recipe, int order) {
        register(id, new Row(input, recipe.fluidProduced, recipe.fluidConsumed, recipe.laserStrength, List.copyOf(recipe.outputs), order));
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static ComparableStack comp(String id) {
        return new ComparableStack(item(id));
    }

    private static Item stack(String id) {
        return item(id);
    }

    /** hbm-space {@code new ComparableStack(ModItems.rbmk_pellet_<name>, 1, meta)}. */
    private static VariantStack pellet(String id, int meta) {
        return new VariantStack(item(id), meta);
    }

    private static String waste(String family, String wasteClass) {
        return (SPACE_WASTE.contains(wasteClass) ? "ntmspace:" : "hbm:") + family + "_" + wasteClass;
    }

    /** hbm-space {@code new ItemStack(ModItems.nuclear_waste_short_tiny, 1, WasteClass.X.ordinal())}. */
    private static Item shortTiny(String wasteClass) {
        return item(waste("nuclear_waste_short_tiny", wasteClass));
    }

    /** hbm-space {@code new ComparableStack(ModItems.nuclear_waste_short, 1, WasteClass.X.ordinal())}. */
    private static ComparableStack shortWaste(String wasteClass) {
        return new ComparableStack(item(waste("nuclear_waste_short", wasteClass)));
    }

    /** hbm-space {@code new ComparableStack(ModItems.nuclear_waste_short_depleted, 1, WasteClass.X.ordinal())}. */
    private static ComparableStack shortDepleted(String wasteClass) {
        return new ComparableStack(item(waste("nuclear_waste_short_depleted", wasteClass)));
    }

    /** hbm-space {@code SILEXRecipe}: the values of a row before it has an input. */
    private static final class SILEXRecipe {

        final int fluidProduced;
        final int fluidConsumed;
        final EnumWavelengths laserStrength;
        final List<WeightedOut> outputs = new ArrayList<>();

        SILEXRecipe(int fluidProduced, int fluidConsumed, int laserStrength) {
            this.fluidProduced = fluidProduced;
            this.fluidConsumed = fluidConsumed;
            this.laserStrength = WAVELENGTHS[laserStrength];
        }

        /** {@code addOut(new ItemStack(item), weight)}; weight 0 is never rolled, and HBM's table rejects it. */
        SILEXRecipe addOut(Item item, int weight) {
            if (weight > 0) outputs.add(new WeightedOut(new ItemOut(item, 1), weight));
            return this;
        }
    }
}
