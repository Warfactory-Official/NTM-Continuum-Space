package com.ntmspace.recipes.data;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.world.RemovalSource;
import java.util.ArrayList;
import java.util.List;

/**
 * The HBM recipes NTM: Space takes out that are not one table source's: HBM's crafting and smelting rows hbm-space changes
 * or drops, and the whole 1.7 bedrock ore chain ({@code recipe_removals/bedrock_ore.json}). hbm-space runs its
 * {@code ItemBedrockOreNew} grade loops over {@code CelestialBedrockOre.getAllTypes()} instead of 1.7's
 * {@code BedrockOreType.values()}; the addon's {@code BedrockOre*Table} sources add the per-body rows and this list removes
 * the rows 1.7's loops over HBM's six types made, in six tables. It is one list rather than six {@code remove(...)} blocks
 * because the crystallizer and shredder tables each have a second NTM: Space source that also removes rows, and one table's
 * removals can be stated by one source only (each writes {@code recipe_removals/<table>.json}).
 */
public final class SpaceRemovals extends RemovalSource {

    @Override
    protected void registerDefaults() {
        // crafting rows hbm-space changes or drops
        register("crafting", List.of("hbm:coffee", "hbm:laser_crystal_dnt", "hbm:powder_fertilizer", "hbm:powder_fertilizer_1",
                "hbm:refueler", "hbm:rtg_fuel_gold", "hbm:rtg_fuel_lead"));
        // furnace rows
        register("smelting", List.of("hbm:chunk_ore_cryolite_1"));
        register("bedrock_ore", bedrockOre());
    }

    /** The ids of the rows 1.7's bedrock ore loops make for HBM's six types, table by table, in 1.7's grade order. */
    private static List<String> bedrockOre() {
        List<String> ids = new ArrayList<>();
        for (BedrockOreType type : BedrockOreType.VALUES) {
            // 1.7 CombinationRecipes: roasting
            for (Grade grade : new Grade[] {Grade.BASE, Grade.PRIMARY, Grade.SULFURIC_BYPRODUCT, Grade.SOLVENT_BYPRODUCT, Grade.RAD_BYPRODUCT}) {
                ids.add(row("combination", grade, type));
            }

            // 1.7 CrystallizerRecipes: washing, the three acids, hydrogen and chlorine weighting, slop recycling
            crystallizer(ids, type, "water", Grade.BASE, Grade.BASE_ROASTED);
            crystallizer(ids, type, "sulfuric_acid", Grade.PRIMARY, Grade.PRIMARY_ROASTED);
            crystallizer(ids, type, "solvent", Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_NOSULFURIC);
            crystallizer(ids, type, "radiosolvent", Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_NOSULFURIC, Grade.PRIMARY_NOSOLVENT);
            crystallizer(ids, type, "water", Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED, Grade.SULFURIC_ARC,
                    Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ROASTED, Grade.SOLVENT_ARC, Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED, Grade.RAD_ARC);
            Grade[] weighted = {Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_SULFURIC, Grade.PRIMARY_NOSULFURIC,
                    Grade.PRIMARY_SOLVENT, Grade.PRIMARY_NOSOLVENT, Grade.PRIMARY_RAD, Grade.PRIMARY_NORAD};
            crystallizer(ids, type, "hydrogen", weighted);
            crystallizer(ids, type, "chlorine", weighted);
            crystallizer(ids, type, "slop", Grade.CRUMBS);

            // 1.7 CentrifugeRecipes
            for (Grade grade : new Grade[] {Grade.BASE, Grade.BASE_ROASTED, Grade.BASE_WASHED, Grade.PRIMARY, Grade.PRIMARY_ROASTED,
                    Grade.PRIMARY_SULFURIC, Grade.PRIMARY_NOSULFURIC, Grade.PRIMARY_SOLVENT, Grade.PRIMARY_NOSOLVENT, Grade.PRIMARY_RAD,
                    Grade.PRIMARY_NORAD, Grade.PRIMARY_FIRST, Grade.PRIMARY_SECOND, Grade.SULFURIC_WASHED, Grade.SOLVENT_WASHED,
                    Grade.RAD_WASHED}) {
                ids.add(row("centrifuge", grade, type));
            }

            // 1.7 ArcFurnaceRecipes: a melting grade whose outputs cannot melt registered no row
            for (Grade grade : new Grade[] {Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED, Grade.SOLVENT_BYPRODUCT,
                    Grade.SOLVENT_ROASTED, Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED}) {
                ids.add(row("arc_furnace", grade, type));
            }
            if (melts(type.primary1, type.primary2)) {
                ids.add(row("arc_furnace", Grade.PRIMARY_FIRST, type));
                ids.add(row("arc_furnace", Grade.PRIMARY_SECOND, type));
                ids.add(row("arc_furnace", Grade.CRUMBS, type));
            }
            if (melts(type.byproductAcid1, type.byproductAcid2, type.byproductAcid3)) ids.add(row("arc_furnace", Grade.SULFURIC_WASHED, type));
            if (melts(type.byproductSolvent1, type.byproductSolvent2, type.byproductSolvent3)) ids.add(row("arc_furnace", Grade.SOLVENT_WASHED, type));
            if (melts(type.byproductRad1, type.byproductRad2, type.byproductRad3)) ids.add(row("arc_furnace", Grade.RAD_WASHED, type));

            // 1.7 ElectrolyserMetalRecipes
            for (Grade grade : new Grade[] {Grade.PRIMARY_FIRST, Grade.PRIMARY_SECOND, Grade.CRUMBS}) {
                ids.add(row("electrolyser_metal", grade, type));
            }

            // 1.7 ShredderRecipes: every grade back to gravel
            for (Grade grade : Grade.values()) {
                ids.add(row("shredder", grade, type));
            }
        }
        return ids;
    }

    private static void crystallizer(List<String> ids, BedrockOreType type, String acid, Grade... grades) {
        for (Grade grade : grades) ids.add(row("crystallizer", grade, type) + "_" + acid);
    }

    /** HBM's id of a bedrock ore row: the input item's path ({@code hbm:<table>/bedrock_ore_<grade>_<type>}). */
    private static String row(String table, Grade grade, BedrockOreType type) {
        return "hbm:" + table + "/" + ModBedrockOreItems.id(grade, type);
    }

    /** 1.7 {@code ItemBedrockOreNew.toFluid} gave a molten material for at least one of the outputs. */
    private static boolean melts(BedrockOreOutput... outputs) {
        for (BedrockOreOutput o : outputs) {
            if (o != null && o.material() != null && o.material().smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) return true;
        }
        return false;
    }
}
