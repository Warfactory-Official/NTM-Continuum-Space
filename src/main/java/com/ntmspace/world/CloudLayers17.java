package com.ntmspace.world;

import dev.golem.core.Ids;
import dev.golem.core.client.Clouds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The clouds of a celestial body, ported from hbm-space's {@code CloudProviderCelestial} and
 * {@code WorldProviderCelestial.getCloudLayerCount / getCloudHeight / getAtmosphereFluidColor} (drawn by Golem Core's
 * {@link Clouds}, registered in {@code client/SpaceClouds}):
 *
 * <ul>
 *   <li>Layers by the body's air pressure: none below 0.5 atm (and none without air), 1 from 0.5, 2 from 2.5, 3 from
 *       5 atm. With the default air that is Eve 3, Kerbin's air (Thatmo's level) 1, Laythe 1, Tekto 1, and none on Duna
 *       (0.1 atm), the Mun, Minmus, Ike, Dres and Moho.</li>
 *   <li>Heights 1.7's default cloud height 128 plus 0, 12 and 24; alphas 0.8 times 1, 0.84 and 0.76; drift 0.03 blocks
 *       a tick along x times 1, 0.84 and 0.7 of the player's age ({@code ticksExisted} + partial tick); each layer moved
 *       by up to 8,192 blocks in x and z by a hash of the world seed, the dimension and the layer (on a 1.7 client the
 *       seed is 0; the dimension is hbm-space's default dimension id of the body).</li>
 *   <li>Colour: 1.7's {@code World.drawCloudsBody} (white, darkened by the body's own time of day, rain and thunder),
 *       tinted towards the non-neutral fluids of the air by their share of the pressure, at most 65 %: earth air,
 *       oxygen, nitrogen, Duna air and CO2 are neutral; Eve air is (53, 32, 74), others their fluid colour.</li>
 * </ul>
 * Kerbin (the overworld) keeps the native first layer's geometry, height, alpha and drift. Its atmosphere tints or
 * hides that layer, and higher pressure adds layers relative to the native one. Other bodies use the legacy layer
 * geometry below. The orbit level has none (1.7 {@code WorldProviderOrbit}).
 *
 * <p>Differences, each from the renderer being the node's vanilla one: the "fast" setting draws vanilla's flat 12-block
 * cells (1.7's fast clouds were 8 blocks a texel), and the cloud distance and fog are vanilla's.
 */
public final class CloudLayers17 {

    private CloudLayers17() {}

    /** {@code CLOUD_BASE_ALPHA}. */
    public static final float BASE_ALPHA = 0.8F;
    /** {@code CLOUD_SCROLL_SPEED}. */
    public static final float SCROLL_SPEED = 0.03F;
    /** 1.7 {@code WorldProvider.getCloudHeight()}, which {@code WorldProviderCelestial} kept for bodies with clouds. */
    public static final float HEIGHT = 128.0F;
    public static final float[] HEIGHT_OFFSETS = {0.0F, 12.0F, 24.0F};
    public static final float[] ALPHA_MULTIPLIERS = {1.0F, 0.84F, 0.76F};
    public static final double[] SCROLL_MULTIPLIERS = {1.0D, 0.84D, 0.7D};
    private static final double LAYER_POSITION_RANGE = 16384.0D;
    private static final long SALT_X = 0x4F1BBCDCBFA54001L;
    private static final long SALT_Z = 0x2C1B3C6D5E7F8103L;

    /** hbm-space {@code SpaceConfig}'s default dimension ids, by body level ({@code world.provider.dimensionId}). */
    private static final Map<String, Long> DIMENSION_IDS = Map.ofEntries(
            Map.entry("mun", 413_015L), Map.entry("duna", 413_016L), Map.entry("ike", 413_017L),
            Map.entry("eve", 413_018L), Map.entry("dres", 413_019L), Map.entry("moho", 413_020L),
            Map.entry("minmus", 413_021L), Map.entry("laythe", 413_022L), Map.entry("tekto", 413_024L),
            Map.entry("thatmo", 413_025L));

    /** {@code getCloudLayerCount(CBT_Atmosphere)}: by total pressure in atm, 0 without air. */
    public static int layerCount(List<Atmospheres.Entry> air) {
        if (air == null || air.isEmpty()) return 0;
        double pressure = 0;
        for (Atmospheres.Entry entry : air) pressure += entry.pressure();
        // 1.7 compared getPressure() (a double) with float literals
        if (pressure < 0.5F) return 0;
        if (pressure >= 5.0F) return 3;
        if (pressure >= 2.5F) return 2;
        return 1;
    }

    /** {@code isNeutralCloudFluid}. */
    public static boolean isNeutral(String fluid) {
        return fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)
                || fluid.equals(Atmospheres.DUNAAIR) || fluid.equals(Atmospheres.CARBONDIOXIDE);
    }

    /** {@code WorldProviderCelestial.getAtmosphereFluidColor}, RGB 0..1. */
    public static double[] fluidColor(String fluid) {
        if (fluid.equals(Atmospheres.EVEAIR)) return new double[] {53F / 255F, 32F / 255F, 74F / 255F};
        // Slightly redder "red sand" tint for Duna-like atmospheres.
        if (fluid.equals(Atmospheres.DUNAAIR)) return new double[] {198F / 255F, 96F / 255F, 64F / 255F};
        // Neutral/desaturated CO2 tint.
        if (fluid.equals(Atmospheres.CARBONDIOXIDE)) return new double[] {188F / 255F, 192F / 255F, 198F / 255F};
        if (fluid.equals(Atmospheres.EARTHAIR) || fluid.equals(Atmospheres.OXYGEN) || fluid.equals(Atmospheres.NITROGEN)) {
            return new double[] {0.7529412F, 0.84705883F, 1.0F};
        }
        int hex = color(fluid);
        return new double[] {((hex >> 16) & 0xFF) / 255.0F, ((hex >> 8) & 0xFF) / 255.0F, (hex & 0xFF) / 255.0F};
    }

    /** The fluid's colour: {@link Atmospheres#color} for the sky's fluids, else HBM's {@code FluidType.getColor()}. */
    private static int color(String fluid) {
        int known = Atmospheres.color(fluid);
        if (known != 0xFFFFFF) return known;
        com.hbm.inventory.fluid.FluidType type = com.hbm.inventory.fluid.Fluids.byRegistryName(fluid);
        return type == com.hbm.inventory.fluid.Fluids.NONE ? 0xFFFFFF : type.getColor();
    }

    /**
     * {@code getCloudTintData}: {strength, r, g, b} with the colour normalised to its brightest channel, or null when the
     * air has nothing that tints.
     */
    public static double[] tintData(List<Atmospheres.Entry> air) {
        if (air == null || air.isEmpty()) return null;
        double totalPressure = 0.0D, tintPressure = 0.0D, tintR = 0.0D, tintG = 0.0D, tintB = 0.0D;
        for (Atmospheres.Entry entry : air) {
            if (entry == null || entry.fluid() == null || entry.pressure() <= 0.0D) continue;
            totalPressure += entry.pressure();
            if (isNeutral(entry.fluid())) continue;
            double[] c = fluidColor(entry.fluid());
            tintPressure += entry.pressure();
            tintR += c[0] * entry.pressure();
            tintG += c[1] * entry.pressure();
            tintB += c[2] * entry.pressure();
        }
        if (totalPressure <= 0.0D || tintPressure <= 0.0D) return null;
        tintR /= tintPressure;
        tintG /= tintPressure;
        tintB /= tintPressure;
        double peak = Math.max(tintR, Math.max(tintG, tintB));
        if (peak <= 0.0D) return null;
        double strength = Mth.clamp(tintPressure / totalPressure, 0.0D, 0.65D);
        return new double[] {strength, Mth.clamp(tintR / peak, 0.0D, 1.0D), Mth.clamp(tintG / peak, 0.0D, 1.0D),
                Mth.clamp(tintB / peak, 0.0D, 1.0D)};
    }

    /** {@code getTintedCloudColor}: the base cloud colour pulled towards the air's tint. */
    public static double[] tinted(List<Atmospheres.Entry> air, double r, double g, double b) {
        double[] tint = tintData(air);
        if (tint == null) return new double[] {r, g, b};
        double luma = r * 0.299D + g * 0.587D + b * 0.114D;
        double brightness = Mth.clamp(luma * 0.9D, 0.0D, 1.0D);
        double strength = tint[0];
        double cr = Mth.clamp(tint[1] * brightness, 0.0D, 1.0D);
        double cg = Mth.clamp(tint[2] * brightness, 0.0D, 1.0D);
        double cb = Mth.clamp(tint[3] * brightness, 0.0D, 1.0D);
        return new double[] {
                Mth.clamp(r + (cr - r) * strength, 0.0D, 1.0D),
                Mth.clamp(g + (cg - g) * strength, 0.0D, 1.0D),
                Mth.clamp(b + (cb - b) * strength, 0.0D, 1.0D)};
    }

    /**
     * 1.7 {@code World.drawCloudsBody} with {@code cloudColour} white: {@code angle} is the body's celestial angle
     * (0 noon, 0.5 midnight), {@code rain} the rain strength and {@code thunder} the weighted thunder strength.
     */
    public static float[] baseColor(float angle, float rain, float thunder) {
        float f2 = Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.5F;
        if (f2 < 0.0F) f2 = 0.0F;
        if (f2 > 1.0F) f2 = 1.0F;
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (rain > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.6F;
            float keep = 1.0F - rain * 0.95F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        r *= f2 * 0.9F + 0.1F;
        g *= f2 * 0.9F + 0.1F;
        b *= f2 * 0.85F + 0.15F;
        if (thunder > 0.0F) {
            float grey = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.2F;
            float keep = 1.0F - thunder * 0.95F;
            r = r * keep + grey * (1.0F - keep);
            g = g * keep + grey * (1.0F - keep);
            b = b * keep + grey * (1.0F - keep);
        }
        return new float[] {r, g, b};
    }

    /** {@code getLayerPositionOffset}: the layer's fixed offset along one axis, -8192..8192 blocks. */
    public static double layerOffset(long seed, long dimension, int layer, long salt) {
        long mixed = mixSeed(seed ^ (dimension * 0x9E3779B97F4A7C15L) ^ ((long) (layer + 1) * 0xC2B2AE3D27D4EB4FL) ^ salt);
        double normalized = (double) (mixed & ((1L << 53) - 1)) / (double) (1L << 53);
        return (normalized - 0.5D) * LAYER_POSITION_RANGE;
    }

    private static long mixSeed(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    /** hbm-space's default dimension id of the body level named {@code level} (0 for an unknown one). */
    public static long dimensionId(String level) {
        return DIMENSION_IDS.getOrDefault(level, 0L);
    }

    /**
     * The layers of a body with air {@code air} on level {@code levelName}, for {@code cloudTime} ({@code ticksExisted}
     * plus the partial tick) and the base colour {@code base} ({@link #baseColor}).
     */
    public static List<Clouds.Layer> layers(List<Atmospheres.Entry> air, String levelName, double cloudTime, float[] base) {
        int count = Math.min(layerCount(air), HEIGHT_OFFSETS.length);
        if (count <= 0) return List.of();
        return coloredLayers(count, levelName, cloudTime, tinted(air, base[0], base[1], base[2]));
    }

    private static List<Clouds.Layer> coloredLayers(int count, String levelName, double cloudTime, double[] colour) {
        long dimension = dimensionId(levelName);
        List<Clouds.Layer> layers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // 1.7: cloudTime * CLOUD_SCROLL_SPEED (a float) * LAYER_SCROLL_MULTIPLIERS (a double) + the layer offset
            double shiftX = cloudTime * SCROLL_SPEED * SCROLL_MULTIPLIERS[i] + layerOffset(0L, dimension, i, SALT_X);
            double shiftZ = layerOffset(0L, dimension, i, SALT_Z);
            layers.add(new Clouds.Layer(HEIGHT + HEIGHT_OFFSETS[i], (float) colour[0], (float) colour[1], (float) colour[2],
                    BASE_ALPHA * ALPHA_MULTIPLIERS[i], shiftX, shiftZ));
        }
        return layers;
    }

    /** Upper decks above a real native first layer; its colour already includes the body's atmosphere tint. */
    public static List<Clouds.Layer> additionalLayers(Level level, Clouds.NativeLayer base) {
        var body = SpaceSkies.airBody(level);
        if (body == null) return List.of();
        var count = Math.min(layerCount(Atmospheres.of(body, level.isClientSide())), HEIGHT_OFFSETS.length);
        if (count <= 1) return List.of();
        var dimension = Ids.keyId(level.dimension()).hashCode();
        var time = base.gameTime() + base.partialTick();
        var argb = base.argb();
        var red = ((argb >>> 16) & 255) / 255.0F;
        var green = ((argb >>> 8) & 255) / 255.0F;
        var blue = (argb & 255) / 255.0F;
        var layers = new ArrayList<Clouds.Layer>(count - 1);
        for (var i = 1; i < count; i++) {
            var shiftX = (SCROLL_MULTIPLIERS[i] - 1.0D) * time * 0.03D
                    + layerOffset(0L, dimension, i, SALT_X);
            var shiftZ = layerOffset(0L, dimension, i, SALT_Z);
            var alpha = Math.round((argb >>> 24) * ALPHA_MULTIPLIERS[i]) / 255.0F;
            layers.add(new Clouds.Layer(base.height() + HEIGHT_OFFSETS[i], red, green, blue, alpha, shiftX, shiftZ));
        }
        return layers;
    }

    /** The body's live cloud colour, shared by environment queries and the rendered layer colours. */
    public static Vec3 color(Level level, float partialTicks) {
        var body = SpaceSkies.airBody(level);
        var air = body == null ? List.<Atmospheres.Entry>of() : Atmospheres.of(body, level.isClientSide());
        var colour = color(level, partialTicks, air);
        return new Vec3(colour[0], colour[1], colour[2]);
    }

    private static double[] color(Level level, float partialTicks, List<Atmospheres.Entry> air) {
        var base = baseColor(SpaceSkies.angle(level, partialTicks), level.getRainLevel(partialTicks),
                level.getThunderLevel(partialTicks));
        return tinted(air, base[0], base[1], base[2]);
    }

    /** {@link #layers} for a body level now: its air, its own time of day, rain and thunder. */
    public static List<Clouds.Layer> layers(Level level, double cloudTime, float partialTicks) {
        var air = SpaceSkies.airBody(level);
        if (air == null) return List.of();
        var entries = Atmospheres.of(air, level.isClientSide());
        var count = Math.min(layerCount(entries), HEIGHT_OFFSETS.length);
        if (count <= 0) return List.of();
        return coloredLayers(count, Ids.keyId(level.dimension()).getPath(), cloudTime,
                color(level, partialTicks, entries));
    }
}
