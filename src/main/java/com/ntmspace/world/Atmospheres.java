package com.ntmspace.world;

import java.util.List;
import java.util.Map;

/**
 * The air of each celestial body, as hbm-space's {@code SolarSystem} gave it in {@code CBT_Atmosphere}: its fluids and
 * their partial pressures in atmospheres. The sky ({@link SpaceSkies}) reads it the way hbm-space's
 * {@code WorldProviderCelestial} read {@code CBT_Atmosphere.fluids} and {@code getPressure()}.
 *
 * <p>Fluids are named by registry name; their colours are hbm-space's {@code Fluids} constants, kept here because the
 * sky formulas use them before (and whether or not) the fluids port registers those fluids.
 */
public final class Atmospheres {

    /** One fluid of a body's air: its registry name and partial pressure in atmospheres ({@code CBT_Atmosphere.FluidEntry}). */
    public record Entry(String fluid, double pressure) {}

    public static final String EARTHAIR = "ntmspace:earthair";
    public static final String EVEAIR = "ntmspace:eveair";
    public static final String DUNAAIR = "ntmspace:dunaair";
    public static final String TEKTOAIR = "ntmspace:tektoair";
    public static final String XENON = "hbm:xenon";
    public static final String NITROGEN = "ntmspace:nitrogen";
    public static final String CARBONDIOXIDE = "hbm:carbondioxide";
    public static final String OXYGEN = "hbm:oxygen";
    public static final String JOOLGAS = "ntmspace:joolgas";
    public static final String CHLORINE = "hbm:chlorine";
    public static final String CHLOROMETHANE = "hbm:chloromethane";
    public static final String RADIOSOLVENT = "hbm:radiosolvent";
    public static final String CCL = "ntmspace:ccl";

    /** hbm-space's {@code SolarSystem} atmospheres, by body name; a body without air has none. */
    private static final Map<String, List<Entry>> AIR = Map.of(
            "kerbin", List.of(new Entry(EARTHAIR, 1.0D)),
            "eve", List.of(new Entry(EVEAIR, 5.0D)),
            "duna", List.of(new Entry(DUNAAIR, 0.1D)),
            "laythe", List.of(new Entry(EARTHAIR, 0.45D), new Entry(XENON, 0.15D)),
            "tekto", List.of(new Entry(TEKTOAIR, 1.5D)),
            "thatmo", List.of(new Entry(NITROGEN, 0.005D)));

    /** hbm-space's {@code Fluids} colours ({@code FluidType.getColor()}) of the fluids the sky formulas can meet. */
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry(EARTHAIR, 0xD1CEEE),
            Map.entry(EVEAIR, 0xDCABF8),
            Map.entry(DUNAAIR, 0xD4704E),
            Map.entry(TEKTOAIR, 0x245F46),
            Map.entry(XENON, 0xBA45E8),
            Map.entry(NITROGEN, 0xB3C6D2),
            Map.entry(CARBONDIOXIDE, 0x404040),
            Map.entry(JOOLGAS, 0x96B596),
            Map.entry(CHLORINE, 0xBAB572));

    private Atmospheres() {}

    /**
     * The air of {@code body} (a {@link CelestialBodies.Body} name), empty without an atmosphere: on a client, the body's
     * runtime air as the server last synced it (hbm-space's sky read {@code getTrait} on the client's synced traits), else
     * the defaults below.
     */
    public static List<Entry> of(String body) {
        List<Entry> runtime = com.ntmspace.trait.CelestialTraits.clientAtmosphere(body);
        if (runtime != null) return runtime;
        return AIR.getOrDefault(body, List.of());
    }

    /** Whether {@code body} has an atmosphere ({@code getTrait(CBT_Atmosphere.class) != null}). */
    public static boolean has(String body) {
        return !of(body).isEmpty();
    }

    /** {@code CBT_Atmosphere.getPressure()}: the sum of the partial pressures, 0 without air. */
    public static double pressure(String body) {
        double total = 0;
        for (Entry entry : of(body)) total += entry.pressure();
        return total;
    }

    /** {@code CBT_Atmosphere.hasFluid(fluid)}. */
    public static boolean hasFluid(String body, String fluid) {
        for (Entry entry : of(body)) if (entry.fluid().equals(fluid)) return true;
        return false;
    }

    /** The fluid's colour, {@code 0xRRGGBB} (white for a fluid hbm-space's sky formulas never meet). */
    public static int color(String fluid) {
        return COLORS.getOrDefault(fluid, 0xFFFFFF);
    }
}
