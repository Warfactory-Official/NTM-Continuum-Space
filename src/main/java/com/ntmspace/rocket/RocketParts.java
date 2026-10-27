package com.ntmspace.rocket;

import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import dev.golem.core.Ids;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MissilePart.registerAllParts} with the part stats of its {@code ModItems} make-calls, transcribed
 * row for row (147 legacy rows, plus the three science satellite payloads). Rows are keyed by item id:
 * HBM's parts and satellites are {@code hbm:}, the 18 parts hbm-space added are {@code ntmspace:}. Plain data, no
 * Minecraft registry read until a lookup by item.
 *
 * <p>Kept as hbm-space had them, odd or not:
 * <ul>
 *   <li>{@code mp_thruster_20_kerosene} is 2 high here (1.7 HBM: 3); HBM's own missile keeps 3, rockets use 2.</li>
 *   <li>{@code mp_thruster_10_hydrazine} reuses the solid thruster's model and skin; hbm-space registered it twice.</li>
 *   <li>the copied fuselage skins ({@code _camo}, {@code _desert}, ...) inherit their base part's mass and tank.</li>
 *   <li>the satellites are 2.0 m payloads under a fairing ({@code mp_w_fairing}), masses from hbm-space's
 *       {@code ItemSatellite(mass)}; hbm-space's {@code sat_mapper}.. map onto HBM's {@code satellite_*} items through
 *       HBM's {@code hbm:satellite} item value map (mapper = spy, laser = death ray, foeq = relay, gerald = horizons).
 *       {@code sat_dyson_relay} and {@code sat_war} belong to the Dyson and war families and are not listed here.</li>
 * </ul>
 */
public final class RocketParts {

    private static final Map<String, RocketPart> BY_ID = new LinkedHashMap<>();

    /** 1.7 {@code ModItems.rp_capsule_20}: the crewed capsule. */
    public static final String CAPSULE = "ntmspace:rp_c_20";
    /** 1.7 {@code ModItems.rp_station_core_20}: the station core payload. */
    public static final String STATION_CORE = "ntmspace:rp_sc_20";
    /** 1.7 {@code ModItems.rp_pod_20}: the reusable drop pod. */
    public static final String POD = "ntmspace:rp_pod_20";

    private RocketParts() {}

    /** Every part, in hbm-space's registration order. */
    public static Collection<RocketPart> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    /** The part of item id {@code id}, or null. */
    public static @Nullable RocketPart get(String id) {
        return BY_ID.get(id);
    }

    /** 1.7 {@code MissilePart.getPart(item)}: null for an item that is no rocket part. */
    public static @Nullable RocketPart get(@Nullable Item item) {
        if (item == null) return null;
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? null : BY_ID.get(key.toString());
    }

    /** 1.7 {@code MissilePart.getPart(stack)}. */
    public static @Nullable RocketPart get(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return get(stack.getItem());
    }

    private static void part(String id, PartType type, @Nullable String fuel, @Nullable String warhead, double top, double bottom,
                             int tank, int mass, int thrust, int isp, double height, double guiHeight, String mesh, String skin,
                             @Nullable String deployed, @Nullable String shroud, @Nullable String renderer) {
        BY_ID.put(id, new RocketPart(id, type, fuel, warhead, top, bottom, tank, mass, thrust, isp, height, guiHeight,
                Ids.parse(mesh), Ids.parse(skin), deployed == null ? null : Ids.parse(deployed),
                shroud == null ? null : Ids.parse(shroud), renderer, true));
    }

    /** 1.7 {@code ItemSatellite(mass)}: {@code makeWarhead(WarheadType.SATELLITE, 15F, mass, PartSize.SIZE_20)}. */
    private static void sat(String id, double height, double guiHeight, int mass, String mesh, String skin, boolean renderInventory) {
        BY_ID.put(id, new RocketPart(id, PartType.WARHEAD, null, "SATELLITE", 0, 2.0, 0, mass, 0, 0, height, guiHeight,
                Ids.parse(mesh), Ids.parse(skin), null, null, null, renderInventory));
    }

    static {
        part("hbm:mp_thruster_10_kerosene", PartType.THRUSTER, "KEROSENE", null, 1.0, 0.0, 0, 180, 16000, 308, 1, 1, "hbm:models/missile_parts/mp_t_10_kerosene.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_10_kerosene.png", null, null, null);
        part("hbm:mp_thruster_10_solid", PartType.THRUSTER, "SOLID", null, 1.0, 0.0, 0, 400, 60000, 195, 0.5, 1, "hbm:models/missile_parts/mp_t_10_solid.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_10_solid.png", null, null, null);
        part("ntmspace:mp_thruster_10_hydrazine", PartType.THRUSTER, "HYDRAZINE", null, 1.0, 0.0, 0, 120, 14000, 220, 0.5, 1, "hbm:models/missile_parts/mp_t_10_solid.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_10_solid.png", null, null, null);
        part("hbm:mp_thruster_10_xenon", PartType.THRUSTER, "XENON", null, 1.0, 0.0, 0, 200, 2000, 4200, 0.5, 1, "hbm:models/missile_parts/mp_t_10_xenon.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_10_xenon.png", null, null, null);
        part("hbm:mp_thruster_15_kerosene", PartType.THRUSTER, "KEROSENE", null, 1.5, 0.0, 0, 900, 120000, 308, 1.5, 1.5, "hbm:models/missile_parts/mp_t_15_kerosene.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene.png", null, null, null);
        part("hbm:mp_thruster_15_kerosene_dual", PartType.THRUSTER, "KEROSENE", null, 1.5, 0.0, 0, 1600, 200000, 308, 1, 1.5, "hbm:models/missile_parts/mp_t_15_kerosene_dual.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png", null, null, null);
        part("hbm:mp_thruster_15_kerosene_triple", PartType.THRUSTER, "KEROSENE", null, 1.5, 0.0, 0, 2200, 280000, 308, 1, 1.5, "hbm:models/missile_parts/mp_t_15_kerosene_triple.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png", null, null, null);
        part("hbm:mp_thruster_15_solid", PartType.THRUSTER, "SOLID", null, 1.5, 0.0, 0, 1000, 220000, 195, 0.5, 1, "hbm:models/missile_parts/mp_t_15_solid.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_solid.png", null, null, null);
        part("hbm:mp_thruster_15_solid_hexdecuple", PartType.THRUSTER, "SOLID", null, 1.5, 0.0, 0, 1200, 260000, 195, 0.5, 1, "hbm:models/missile_parts/mp_t_15_solid_hexdecuple.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_solid_hexdecuple.png", null, null, null);
        part("hbm:mp_thruster_15_hydrogen", PartType.THRUSTER, "HYDROGEN", null, 1.5, 0.0, 0, 800, 100000, 380, 1.5, 1.5, "hbm:models/missile_parts/mp_t_15_kerosene.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_hydrogen.png", null, null, null);
        part("hbm:mp_thruster_15_hydrogen_dual", PartType.THRUSTER, "HYDROGEN", null, 1.5, 0.0, 0, 1400, 200000, 380, 1, 1.5, "hbm:models/missile_parts/mp_t_15_kerosene_dual.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_hydrogen_dual.png", null, null, null);
        part("hbm:mp_thruster_15_balefire_short", PartType.THRUSTER, "BALEFIRE", null, 1.5, 0.0, 0, 1600, 800000, 666, 2, 2, "hbm:models/missile_parts/mp_t_15_balefire_short.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_short.png", null, null, null);
        part("hbm:mp_thruster_15_balefire", PartType.THRUSTER, "BALEFIRE", null, 1.5, 0.0, 0, 2000, 1000000, 666, 3, 2.5, "hbm:models/missile_parts/mp_t_15_balefire.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire.png", null, null, null);
        part("hbm:mp_thruster_15_balefire_large", PartType.THRUSTER, "BALEFIRE", null, 1.5, 0.0, 0, 2400, 1200000, 666, 3, 2.5, "hbm:models/missile_parts/mp_t_15_balefire_large.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_large.png", null, null, null);
        part("hbm:mp_thruster_15_balefire_large_rad", PartType.THRUSTER, "BALEFIRE", null, 1.5, 0.0, 0, 2400, 1200000, 666, 3, 2.5, "hbm:models/missile_parts/mp_t_15_balefire_large.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_15_balefire_large_rad.png", null, null, null);
        part("hbm:mp_thruster_20_kerosene", PartType.THRUSTER, "KEROSENE", null, 2.0, 0.0, 0, 2400, 1536000, 308, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene.png", null, null, null);
        part("hbm:mp_thruster_20_kerosene_dual", PartType.THRUSTER, "KEROSENE", null, 2.0, 0.0, 0, 3200, 1934000, 308, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_dual.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png", null, null, null);
        part("hbm:mp_thruster_20_kerosene_triple", PartType.THRUSTER, "KEROSENE", null, 2.0, 0.0, 0, 4400, 2542000, 308, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_triple.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png", null, null, null);
        part("ntmspace:mp_thruster_20_methalox", PartType.THRUSTER, "METHALOX", null, 2.0, 0.0, 0, 2400, 890000, 320, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene.obj", "ntmspace:textures/models/missile_parts/thrusters/mp_t_20_methalox.png", null, null, null);
        part("ntmspace:mp_thruster_20_methalox_dual", PartType.THRUSTER, "METHALOX", null, 2.0, 0.0, 0, 3200, 1184000, 320, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_dual.obj", "ntmspace:textures/models/missile_parts/thrusters/mp_t_20_methalox_dual.png", null, null, null);
        part("ntmspace:mp_thruster_20_methalox_triple", PartType.THRUSTER, "METHALOX", null, 2.0, 0.0, 0, 4400, 1456000, 320, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_triple.obj", "ntmspace:textures/models/missile_parts/thrusters/mp_t_20_methalox_dual.png", null, null, null);
        part("ntmspace:mp_thruster_20_hydrogen", PartType.THRUSTER, "HYDROGEN", null, 2.0, 0.0, 0, 2600, 480000, 380, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_hydrogen.png", null, null, null);
        part("ntmspace:mp_thruster_20_hydrogen_dual", PartType.THRUSTER, "HYDROGEN", null, 2.0, 0.0, 0, 3400, 640000, 380, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_dual.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_hydrogen_dual.png", null, null, null);
        part("ntmspace:mp_thruster_20_hydrogen_triple", PartType.THRUSTER, "HYDROGEN", null, 2.0, 0.0, 0, 4500, 938000, 380, 2, 2, "hbm:models/missile_parts/mp_t_20_kerosene_triple.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_hydrogen_dual.png", null, null, null);
        part("hbm:mp_thruster_20_solid", PartType.THRUSTER, "SOLID", null, 2.0, 0.0, 0, 2400, 1400000, 195, 1, 1.75, "hbm:models/missile_parts/mp_t_20_solid.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid.png", null, null, null);
        part("hbm:mp_thruster_20_solid_multi", PartType.THRUSTER, "SOLID", null, 2.0, 0.0, 0, 3200, 1830000, 195, 0.5, 1.5, "hbm:models/missile_parts/mp_t_20_solid_multi.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid_multi.png", null, null, null);
        part("hbm:mp_thruster_20_solid_multier", PartType.THRUSTER, "SOLID", null, 2.0, 0.0, 0, 4000, 2320000, 195, 0.5, 1.5, "hbm:models/missile_parts/mp_t_20_solid_multi.obj", "hbm:textures/models/missile_parts/thrusters/mp_t_20_solid_multier.png", null, null, null);
        part("ntmspace:mp_thruster_20_hydrazine", PartType.THRUSTER, "HYDRAZINE", null, 2.0, 0.0, 0, 1200, 600000, 220, 3, 2.5, "ntmspace:models/missile_parts/mp_t_20_hydrazine.obj", "ntmspace:textures/models/missile_parts/thrusters/mp_t_20_hydrazine.png", null, null, null);
        part("hbm:mp_stability_10_flat", PartType.FINS, null, null, 1.0, 1.0, 0, 0, 0, 0, 0, 2, "hbm:models/missile_parts/mp_s_10_flat.obj", "hbm:textures/models/missile_parts/stability/mp_s_10_flat.png", null, null, null);
        part("hbm:mp_stability_10_cruise", PartType.FINS, null, null, 1.0, 1.0, 0, 0, 0, 0, 0, 3, "hbm:models/missile_parts/mp_s_10_cruise.obj", "hbm:textures/models/missile_parts/stability/mp_s_10_cruise.png", null, null, null);
        part("hbm:mp_stability_10_space", PartType.FINS, null, null, 1.0, 1.0, 0, 0, 0, 0, 0, 2, "hbm:models/missile_parts/mp_s_10_space.obj", "hbm:textures/models/missile_parts/stability/mp_s_10_space.png", null, null, null);
        part("hbm:mp_stability_15_flat", PartType.FINS, null, null, 1.5, 1.5, 0, 0, 0, 0, 0, 3, "hbm:models/missile_parts/mp_s_15_flat.obj", "hbm:textures/models/missile_parts/stability/mp_s_15_flat.png", null, null, null);
        part("hbm:mp_stability_15_thin", PartType.FINS, null, null, 1.5, 1.5, 0, 0, 0, 0, 0, 3, "hbm:models/missile_parts/mp_s_15_thin.obj", "hbm:textures/models/missile_parts/stability/mp_s_15_thin.png", null, null, null);
        part("hbm:mp_stability_15_soyuz", PartType.FINS, null, null, 1.5, 1.5, 0, 0, 0, 0, 0, 3, "hbm:models/missile_parts/mp_s_15_soyuz.obj", "hbm:textures/models/missile_parts/stability/mp_s_15_soyuz.png", null, null, null);
        part("hbm:mp_s_20", PartType.FINS, null, null, 2.0, 2.0, 0, 0, 0, 0, 0, 3, "hbm:models/missile_parts/mp_s_20.obj", "hbm:textures/models/thegadget3_.png", null, null, null);
        part("ntmspace:rp_l_20", PartType.FINS, null, null, 2.0, 2.0, 0, 0, 0, 0, 2.4, 3, "ntmspace:models/missile_parts/rp_s_20_leggy.obj", "hbm:textures/models/thegadget3_.png", "ntmspace:models/missile_parts/rp_s_20_leggy_deployed.obj", null, null);
        part("hbm:mp_fuselage_10_kerosene", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_camo", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_camo.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_desert", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_desert.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_sky", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_sky.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_insulation", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_insulation.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_flames", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_flames.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_sleek", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_sleek.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_metal", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_kerosene_metal.png", null, null, null);
        part("hbm:mp_fuselage_10_kerosene_taint", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_kerosene_taint.png", null, null, null);
        part("hbm:mp_fuselage_10_solid", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid.png", null, null, null);
        part("ntmspace:mp_fuselage_10_hydrazine", PartType.FUSELAGE, "HYDRAZINE", null, 1.0, 1.0, 2000, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_10_hydrazine.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_flames", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_flames.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_insulation", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_insulation.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_sleek", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_sleek.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_soviet_glory", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_soviet_glory.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_cathedral", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_cathedral.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_moonlit", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_moonlit.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_battery", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_solid_battery.png", null, null, null);
        part("hbm:mp_fuselage_10_solid_duracell", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 2500, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_solid_duracell.png", null, null, null);
        part("hbm:mp_fuselage_10_xenon", PartType.FUSELAGE, "XENON", null, 1.0, 1.0, 5000, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_xenon.png", null, null, null);
        part("hbm:mp_fuselage_10_xenon_bhole", PartType.FUSELAGE, "XENON", null, 1.0, 1.0, 5000, 1000, 0, 0, 4, 3, "hbm:models/missile_parts/mp_f_10_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_xenon_bhole.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_camo", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_camo.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_desert", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_desert.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_sky", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sky.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_flames", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_flames.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_insulation", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_insulation.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_sleek", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sleek.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_metal", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_metal.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_dash", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_dash.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_taint", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_taint.png", null, null, null);
        part("hbm:mp_fuselage_10_long_kerosene_vap", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_vap.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_flames", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_flames.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_insulation", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_insulation.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_sleek", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_sleek.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_soviet_glory", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_long_solid_soviet_glory.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_bullet", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_bullet.png", null, null, null);
        part("hbm:mp_fuselage_10_long_solid_silvermoonlight", PartType.FUSELAGE, "SOLID", null, 1.0, 1.0, 5000, 1000, 0, 0, 7, 5, "hbm:models/missile_parts/mp_f_10_long_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_silvermoonlight.png", null, null, null);
        part("hbm:mp_fuselage_10_15_kerosene", PartType.FUSELAGE, "KEROSENE", null, 1.0, 1.5, 10000, 1000, 0, 0, 9, 5.5, "hbm:models/missile_parts/mp_f_10_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_kerosene.png", null, null, null);
        part("hbm:mp_fuselage_10_15_solid", PartType.FUSELAGE, "SOLID", null, 1.0, 1.5, 10000, 1000, 0, 0, 9, 5.5, "hbm:models/missile_parts/mp_f_10_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_solid.png", null, null, null);
        part("hbm:mp_fuselage_10_15_hydrogen", PartType.FUSELAGE, "HYDROGEN", null, 1.0, 1.5, 10000, 1000, 0, 0, 9, 5.5, "hbm:models/missile_parts/mp_f_10_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_hydrogen.png", null, null, null);
        part("hbm:mp_fuselage_10_15_balefire", PartType.FUSELAGE, "BALEFIRE", null, 1.0, 1.5, 10000, 1000, 0, 0, 9, 5.5, "hbm:models/missile_parts/mp_f_10_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_10_15_balefire.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_camo", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_camo.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_desert", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_desert.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_sky", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_sky.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_insulation", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_insulation.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_metal", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_metal.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_decorated", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_decorated.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_steampunk", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_steampunk.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_polite", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_polite.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_blackjack", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/base/mp_f_15_kerosene_blackjack.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_lambda", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_lambda.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_minuteman", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_minuteman.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_pip", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_pip.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_taint", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_taint.png", null, null, null);
        part("hbm:mp_fuselage_15_kerosene_yuck", PartType.FUSELAGE, "KEROSENE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_kerosene_yuck.png", null, null, null);
        part("hbm:mp_fuselage_15_solid", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_insulation", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_insulation.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_desh", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_desh.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_soviet_glory", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_glory.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_soviet_stank", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_stank.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_faust", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_faust.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_silvermoonlight", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_silvermoonlight.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_snowy", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_solid_snowy.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_panorama", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_panorama.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_roses", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_roses.png", null, null, null);
        part("hbm:mp_fuselage_15_solid_mimi", PartType.FUSELAGE, "SOLID", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_solid_mimi.png", null, null, null);
        part("hbm:mp_fuselage_15_hydrogen", PartType.FUSELAGE, "HYDROGEN", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_hydrogen.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_hydrogen.png", null, null, null);
        part("hbm:mp_fuselage_15_hydrogen_cathedral", PartType.FUSELAGE, "HYDROGEN", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_hydrogen.obj", "hbm:textures/models/missile_parts/fuselages/contest/mp_f_15_hydrogen_cathedral.png", null, null, null);
        part("hbm:mp_fuselage_15_balefire", PartType.FUSELAGE, "BALEFIRE", null, 1.5, 1.5, 15000, 1000, 0, 0, 10, 6, "hbm:models/missile_parts/mp_f_15_hydrogen.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_balefire.png", null, null, null);
        part("hbm:mp_fuselage_15_20_kerosene", PartType.FUSELAGE, "KEROSENE", null, 1.5, 2.0, 20000, 1000, 0, 0, 16, 10, "hbm:models/missile_parts/mp_f_15_20_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_kerosene.png", null, null, null);
        part("hbm:mp_fuselage_15_20_kerosene_magnusson", PartType.FUSELAGE, "KEROSENE", null, 1.5, 2.0, 20000, 1000, 0, 0, 16, 10, "hbm:models/missile_parts/mp_f_15_20_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_kerosene_magnusson.png", null, null, null);
        part("hbm:mp_fuselage_15_20_solid", PartType.FUSELAGE, "SOLID", null, 1.5, 2.0, 20000, 1000, 0, 0, 16, 10, "hbm:models/missile_parts/mp_f_15_20_kerosene.obj", "hbm:textures/models/missile_parts/fuselages/mp_f_15_20_solid.png", null, null, null);
        part("ntmspace:rp_f_20_12", PartType.FUSELAGE, "ANY", null, 2.0, 2.0, 64000, 4000, 0, 0, 12, 8, "ntmspace:models/missile_parts/mp_f_20_usa.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_20_kerosene_usa.png", null, null, null);
        part("ntmspace:rp_f_20_6", PartType.FUSELAGE, "ANY", null, 2.0, 2.0, 32000, 2100, 0, 0, 6, 4.5, "ntmspace:models/missile_parts/mp_f_20_6_usa.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_20_kerosene_usa.png", null, null, null);
        part("ntmspace:rp_f_20_3", PartType.FUSELAGE, "ANY", null, 2.0, 2.0, 16000, 1200, 0, 0, 3, 2.5, "ntmspace:models/missile_parts/mp_f_20_3_usa.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_20_kerosene.png", null, "ntmspace:models/missile_parts/mp_f_20_6_usa.obj", null);
        part("ntmspace:rp_f_20_1", PartType.FUSELAGE, "ANY", null, 2.0, 2.0, 6000, 500, 0, 0, 1, 1.5, "ntmspace:models/missile_parts/mp_f_20_1_usa.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_20_kerosene.png", null, "ntmspace:models/missile_parts/mp_f_20_6_usa.obj", null);
        part("ntmspace:mp_fuselage_20_hydrazine", PartType.FUSELAGE, "HYDRAZINE", null, 2.0, 2.0, 12500, 1000, 0, 0, 10, 8, "ntmspace:models/missile_parts/mp_f_20_neo.obj", "ntmspace:textures/models/missile_parts/fuselages/mp_f_20_hydrazine.png", null, null, null);
        part("hbm:mp_warhead_10_he", PartType.WARHEAD, null, "HE", 0.0, 1.0, 0, 1500, 0, 0, 2, 1.5, "hbm:models/missile_parts/mp_w_10_he.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_he.png", null, null, null);
        part("hbm:mp_warhead_10_incendiary", PartType.WARHEAD, null, "INC", 0.0, 1.0, 0, 1500, 0, 0, 2.5, 2, "hbm:models/missile_parts/mp_w_10_incendiary.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_incendiary.png", null, null, null);
        part("hbm:mp_warhead_10_buster", PartType.WARHEAD, null, "BUSTER", 0.0, 1.0, 0, 1500, 0, 0, 0.5, 1, "hbm:models/missile_parts/mp_w_10_buster.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_buster.png", null, null, null);
        part("hbm:mp_warhead_10_nuclear", PartType.WARHEAD, null, "NUCLEAR", 0.0, 1.0, 0, 1500, 0, 0, 2, 1.5, "hbm:models/missile_parts/mp_w_10_nuclear.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_nuclear.png", null, null, null);
        part("hbm:mp_warhead_10_nuclear_large", PartType.WARHEAD, null, "NUCLEAR", 0.0, 1.0, 0, 2500, 0, 0, 2.5, 1.5, "hbm:models/missile_parts/mp_w_10_nuclear_large.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_nuclear_large.png", null, null, null);
        part("hbm:mp_warhead_10_taint", PartType.WARHEAD, null, "TAINT", 0.0, 1.0, 0, 1500, 0, 0, 2.25, 1.5, "hbm:models/missile_parts/mp_w_10_taint.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_taint.png", null, null, null);
        part("hbm:mp_warhead_10_cloud", PartType.WARHEAD, null, "CLOUD", 0.0, 1.0, 0, 1500, 0, 0, 2.25, 1.5, "hbm:models/missile_parts/mp_w_10_taint.obj", "hbm:textures/models/missile_parts/warheads/mp_w_10_cloud.png", null, null, null);
        part("hbm:mp_warhead_15_he", PartType.WARHEAD, null, "HE", 0.0, 1.5, 0, 2500, 0, 0, 2, 1.5, "hbm:models/missile_parts/mp_w_15_he.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_he.png", null, null, null);
        part("hbm:mp_warhead_15_incendiary", PartType.WARHEAD, null, "INC", 0.0, 1.5, 0, 2500, 0, 0, 2, 1.5, "hbm:models/missile_parts/mp_w_15_incendiary.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_incendiary.png", null, null, null);
        part("hbm:mp_warhead_15_nuclear", PartType.WARHEAD, null, "NUCLEAR", 0.0, 1.5, 0, 5000, 0, 0, 3.5, 2, "hbm:models/missile_parts/mp_w_15_nuclear.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear.png", null, null, null);
        part("hbm:mp_warhead_15_nuclear_shark", PartType.WARHEAD, null, "NUCLEAR", 0.0, 1.5, 0, 5000, 0, 0, 3.5, 2, "hbm:models/missile_parts/mp_w_15_nuclear.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear_shark.png", null, null, null);
        part("hbm:mp_warhead_15_nuclear_mimi", PartType.WARHEAD, null, "NUCLEAR", 0.0, 1.5, 0, 5000, 0, 0, 3.5, 2, "hbm:models/missile_parts/mp_w_15_nuclear.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_nuclear_mimi.png", null, null, null);
        part("hbm:mp_warhead_15_boxcar", PartType.WARHEAD, null, "TX", 0.0, 1.5, 0, 7500, 0, 0, 2.25, 7.5, "hbm:models/missile_parts/mp_w_15_boxcar.obj", "hbm:textures/models/boxcar.png", null, null, null);
        part("hbm:mp_warhead_15_n2", PartType.WARHEAD, null, "N2", 0.0, 1.5, 0, 5000, 0, 0, 3, 2, "hbm:models/missile_parts/mp_w_15_n2.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_n2.png", null, null, null);
        part("hbm:mp_warhead_15_balefire", PartType.WARHEAD, null, "BALEFIRE", 0.0, 1.5, 0, 7500, 0, 0, 2.75, 2, "hbm:models/missile_parts/mp_w_15_balefire.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_balefire.png", null, null, null);
        part("hbm:mp_warhead_15_turbine", PartType.WARHEAD, null, "TURBINE", 0.0, 1.5, 0, 5000, 0, 0, 2.25, 2, "hbm:models/missile_parts/mp_w_15_turbine.obj", "hbm:textures/models/missile_parts/warheads/mp_w_15_turbine.png", null, null, null);
        part("ntmspace:rp_c_20", PartType.WARHEAD, null, "APOLLO", 0.0, 2.0, 0, 8000, 0, 0, 3.5, 2.25, "ntmspace:models/missile_parts/rp_landing_capsule.obj", "ntmspace:textures/models/missile_parts/warheads/rp_landing_capsule.png", null, null, null);
        part("ntmspace:rp_sc_20", PartType.WARHEAD, null, "SATELLITE", 0.0, 2.0, 0, 64000, 0, 0, 7, 5, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", null, null, null);
        part("ntmspace:rp_pod_20", PartType.WARHEAD, null, "APOLLO", 0.0, 2.0, 0, 4000, 0, 0, 3.0, 2.25, "ntmspace:models/missile_parts/rp_drop_pod.obj", "ntmspace:textures/models/missile_parts/warheads/rp_drop_pod.png", null, null, "RenderDropPod");
        sat("hbm:satellite_science", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_science_sensor", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_science_assembler", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_spy", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_scanner", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_radar", 7, 5, 16000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_death_ray", 7, 5, 48000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_relay", 7, 5, 20000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_xenium_resonator", 7, 5, 20000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_miner_astro", 7, 5, 64000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_miner_lunar", 7, 5, 64000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:sat_gerald", 7, 5, 128000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_precision_laser", 7, 5, 42000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_detector", 7, 5, 24000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
        sat("hbm:satellite_ray_scan", 7, 5, 34000, "ntmspace:models/missile_parts/mp_w_fairing.obj", "ntmspace:textures/models/missile_parts/warheads/mp_w_fairing.png", false);
    }
}
