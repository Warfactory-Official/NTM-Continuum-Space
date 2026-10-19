package com.ntmspace.recipes.data;

import com.hbm.api.data.HazardDataMaps;
import com.hbm.hazard.HazardSpec;
import com.hbm.hazard.HazardSpec.Entry;
import com.hbm.hazard.modifier.HazardModifierRBMKHot;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.hazard.modifier.IHazardModifier;
import dev.golem.core.data.DataMapSource;
import java.util.ArrayList;
import java.util.List;

/**
 * hbm-space {@code HazardRegistry.registerItems} and {@code OreDictManager}'s {@code DictFrame.rad / hot}: the hazards of
 * the items and materials NTM: Space adds, as a patch of HBM's {@code hbm:hazards}. Levels are 1.7's float arithmetic
 * ({@code HazardEntry} held a float), widened to double as the map stores them.
 */
public final class SpaceHazards extends DataMapSource<HazardSpec> {

    private static final String RADIATION = "hbm:radiation";
    private static final String HOT = "hbm:hot";

    // hbm-space HazardRegistry constants
    private static final float cm242 = 9.3F;
    private static final float cm243 = 5.6F;
    private static final float cm244 = 2.0F;
    private static final float cm245 = 0.8F;
    private static final float cm246 = 2.5F;
    private static final float cm247 = 0.2F;
    private static final float cmrg = 6.0F;
    private static final float cmf = 2.2F;
    private static final float bk247 = 10.5F;
    private static final float cf251 = 14.3F;
    private static final float cf252 = 15.3F;
    private static final float es253 = 18.3F;
    private static final float es255 = 19.3F;
    private static final float amrg = 9.0F;
    private static final float pu241 = 25.0F;
    private static final float cn989 = 89.0F;
    private static final float wst = 15.0F;

    private static final float nugget = 0.1F;
    private static final float ingot = 1.0F;
    private static final float plate = ingot;
    private static final float powder = ingot * 3.0F;
    private static final float billet = 0.5F;
    private static final float rtg = billet * 3;
    private static final float rod = 0.5F;
    private static final float rod_rbmk = rod * 8;

    public SpaceHazards() {
        super(HazardDataMaps.HAZARDS, true);
    }

    @Override
    protected void values() {
        registerRBMKRod("ntmspace:rbmk_fuel_bk247", bk247 * rod_rbmk, wst * rod_rbmk * 38F);
        registerRBMKRod("ntmspace:rbmk_fuel_lecm", cmrg * 2.2F * rod_rbmk / 5F, wst * rod_rbmk);
        registerRBMKRod("ntmspace:rbmk_fuel_mecm", cmrg * 2.2F * rod_rbmk / 3F, wst * rod_rbmk * 3F);
        registerRBMKRod("ntmspace:rbmk_fuel_hecm", cmrg * 2.2F * rod_rbmk / 1.5F, wst * rod_rbmk * 5F);

        registerRBMKPellet("ntmspace:rbmk_pellet_bk247", bk247 * billet, wst * billet * 37F);
        registerRBMKPellet("ntmspace:rbmk_pellet_lecm", cmrg * billet, wst * cf252 * 2.5F);
        registerRBMKPellet("ntmspace:rbmk_pellet_mecm", cmrg * billet, wst * cf252 * 2.6F);
        registerRBMKPellet("ntmspace:rbmk_pellet_hecm", cmrg * billet, wst * cf252 * 2.7F);

        // new ItemStack(pellet_rtg_depleted, 1, DepletedRTGMaterial.AMERICIUM.ordinal())
        put("ntmspace:pellet_rtg_depleted_americium", data(entry(RADIATION, amrg * rtg)));

        // DictFrame.fromOne(ModItems.watz_pellet, EnumWatzType.X): the types hbm-space adds
        put("ntmspace:watz_pellet_pu241", data(entry(RADIATION, pu241 * ingot * 4)));
        put("ntmspace:watz_pellet_amrg", data(entry(RADIATION, amrg * ingot * 4)));
        put("ntmspace:watz_pellet_cmrg", data(entry(RADIATION, cmrg * ingot * 4)));
        put("ntmspace:watz_pellet_cmf", data(entry(RADIATION, cmf * ingot * 4)));
        put("ntmspace:watz_pellet_bk247", data(entry(RADIATION, bk247 * ingot * 4)));
        put("ntmspace:watz_pellet_cf252", data(entry(RADIATION, cf252 * ingot * 4)));
        put("ntmspace:watz_pellet_es253", data(entry(RADIATION, es253 * ingot * 4)));

        // OreDictManager: X.rad(...).hot(...).nugget(...).billet(...).ingot(...), each shape a mutex-1 tag entry
        // port: hbm-space's .neutron(...) entries (CM243..CM246, CMRG, CMF, CF252, ES253) are not stated: the port has no
        // neutron hazard type
        material("cm242", cm242, 0F, "nuggets", "billets", "ingots");
        material("cm243", cm243, 0F, "nuggets", "billets", "ingots");
        material("cm244", cm244, 0F, "nuggets", "billets", "ingots");
        material("cm245", cm245, 0F, "nuggets", "billets", "ingots");
        material("cm246", cm246, 0F, "nuggets", "billets", "ingots");
        material("cm247", cm247, 0F, "nuggets", "billets", "ingots");
        material("cm_mix", cmrg, 0F, "nuggets", "billets", "ingots");
        material("cm_fuel", cmf, 0F, "nuggets", "billets", "ingots");
        material("bk247", bk247, 0F, "nuggets", "billets", "ingots");
        material("cf251", cf251, 2F, "nuggets", "billets", "ingots");
        material("cf252", cf252, 3F, "nuggets", "billets", "ingots");
        material("es253", es253, 4F, "nuggets", "billets", "ingots");
        material("es255", es255, 0F, "ingots");
        material("cn989", cn989, 4F, "nuggets", "billets", "ingots", "dusts", "plates");

        put("ntmspace:ammonium_nitrate", data(entry("hbm:explosive", 4F)));
        put("ntmspace:powder_wd2004", data(entry("hbm:digamma", 1F)));
        put("ntmspace:powder_wd2004_tiny", data(entry("hbm:digamma", 0.05F)));

        // nuclear_waste_short / _tiny / _depleted / _depleted_tiny: the waste classes hbm-space adds
        for (String waste : new String[] {"americium241", "berkelium247", "curium244", "curium245"}) {
            put("ntmspace:nuclear_waste_short_" + waste, data(entry(RADIATION, 30F), entry(HOT, 5F)));
            put("ntmspace:nuclear_waste_short_tiny_" + waste, data(entry(RADIATION, 3F), entry(HOT, 5F)));
            put("ntmspace:nuclear_waste_short_depleted_" + waste, data(entry(RADIATION, 3F)));
            // port: 1.7's 0.3F is 0.30000001192092896 as a double; the map states 0.3
            put("ntmspace:nuclear_waste_short_depleted_tiny_" + waste, data(new Entry(RADIATION, 0.3D, List.of())));
        }

        // new ItemStack(bobblehead, 1, 22) (ASBESTOS) and meta 25 (GLITCH)
        put("ntmspace:bobblehead_juice", data(entry("hbm:asbestos", 1F)));
        put("hbm:bobblehead_microwave", data(entry("ntmspace:glitch", 1F)));
        put("hbm:sat_gerald", data(entry("ntmspace:autism", 1F)));
    }

    /** 1.7 {@code DictFrame.registerHazards}: each shape's tag gets the frame's hazards times the shape's multiplier, mutex 1. */
    private void material(String mat, float rad, float hot, String... shapes) {
        for (String shape : shapes) {
            float mult = switch (shape) {
                case "nuggets" -> nugget;
                case "billets" -> billet;
                case "ingots" -> ingot;
                case "dusts" -> powder;
                case "plates" -> plate;
                default -> throw new IllegalArgumentException(shape);
            };
            List<Entry> entries = new ArrayList<>();
            entries.add(entry(RADIATION, rad * mult));
            if (hot > 0F) entries.add(entry(HOT, hot * mult));
            put("#c:" + shape + "/" + mat, new HazardSpec(false, 1, false, entries));
        }
    }

    /** 1.7 {@code registerRBMKRod(rod, base, dep)}: radiation with the RBMK modifier, and the RBMK heat. */
    private void registerRBMKRod(String rod, float base, float dep) {
        put(rod, data(new Entry(RADIATION, base, List.<IHazardModifier>of(new HazardModifierRBMKRadiation(dep, false))),
                new Entry(HOT, 0D, List.<IHazardModifier>of(new HazardModifierRBMKHot()))));
    }

    /** 1.7 {@code registerRBMKPellet(pellet, base, dep)}. */
    private void registerRBMKPellet(String pellet, float base, float dep) {
        put(pellet, data(new Entry(RADIATION, base, List.<IHazardModifier>of(new HazardModifierRBMKRadiation(dep, false)))));
    }

    private static Entry entry(String type, float level) {
        return new Entry(type, level, List.of());
    }

    /** 1.7 {@code makeData().addEntry(...)}: no mutex, no override. */
    private static HazardSpec data(Entry... entries) {
        return new HazardSpec(false, 0, false, List.of(entries));
    }
}
