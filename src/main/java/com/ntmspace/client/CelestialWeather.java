package com.ntmspace.client;

import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.trait.CBT_Water;
import com.ntmspace.trait.CBT_Weather;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SkyMath;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.gen.noise.Noise17Perlin;
import dev.golem.core.client.Precipitation;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code WeatherProviderCelestial}, the weather renderer of every celestial body's world (Kerbin's
 * overworld included, {@code WorldProviderEarth}): 1.7's rain and snow columns, drawn through Golem
 * {@link Precipitation} with the RGB of the rain replaced by the body's liquid ({@code CBT_Water}, desaturated to 35 %,
 * {@code getWeatherColor}) and of the snow by the same unless the liquid is water ({@code getSnowColor}); nothing on a body
 * without a weather cycle.
 *
 * <p>Ported as 1.7 drew it: the {@code (fancy ? 10 : 5)} column radius (Golem's {@link Precipitation.Frame#radius}), the
 * 32 x 32 column-direction table, the per-column seed, the rain's scroll by the player's age and the snow's sway, the
 * alpha falling off with the distance, light from the sky and block light at the column (snow brightened by 1.7's
 * {@code (light * 3 + 15728880) / 4}), and rain or snow by 1.7's {@code getFloatTemperature} at the column's lowest
 * point (the biome's temperature lowered above y 64 by 1.7's temperature noise, seed 1234) against 0.15 at the
 * precipitation height. A biome without precipitation has none, as 1.7's {@code canSpawnLightningBolt || getEnableSnow}.
 */
public final class CelestialWeather {

    private CelestialWeather() {}

    private static final String RAIN_TEXTURE = "minecraft:textures/environment/rain.png";
    private static final String SNOW_TEXTURE = "minecraft:textures/environment/snow.png";

    /** 1.7 {@code BiomeGenBase.temperatureNoise}. */
    private static final Noise17Perlin TEMPERATURE_NOISE = new Noise17Perlin(new Random(1234L), 1);
    private static final float[] RAIN_X = new float[1024];
    private static final float[] RAIN_Y = new float[1024];

    static {
        // initRainCoords
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float coordX = j - 16;
                float coordY = i - 16;
                float coordLength = Mth.sqrt(coordX * coordX + coordY * coordY);
                RAIN_X[i << 5 | j] = -coordY / coordLength;
                RAIN_Y[i << 5 | j] = coordX / coordLength;
            }
        }
    }

    private static final Random RANDOM = new Random();

    public static void init() {
        Precipitation.Renderer renderer = CelestialWeather::render;
        Precipitation.register(Level.OVERWORLD, renderer);
        Precipitation.register(SpaceDimensions.MUN, renderer);
        Precipitation.register(SpaceDimensions.MINMUS, renderer);
        Precipitation.register(SpaceDimensions.DUNA, renderer);
        Precipitation.register(SpaceDimensions.IKE, renderer);
        Precipitation.register(SpaceDimensions.DRES, renderer);
        Precipitation.register(SpaceDimensions.EVE, renderer);
        Precipitation.register(SpaceDimensions.MOHO, renderer);
        Precipitation.register(SpaceDimensions.LAYTHE, renderer);
        Precipitation.register(SpaceDimensions.TEKTO, renderer);
        Precipitation.register(SpaceDimensions.THATMO, renderer);
    }

    /** {@code getWeatherColor}: the body's liquid, desaturated to 35 % around its luminance ({@link SkyMath#weatherColor}). */
    public static float[] rainColor(int fluidColor) {
        return SkyMath.weatherColor(fluidColor);
    }

    private static float[] rainColor(String body) {
        CBT_Water water = CelestialTraits.getTrait(body, CBT_Water.class, true);
        if (water == null || water.fluid == null) return new float[] {1.0F, 1.0F, 1.0F};
        return rainColor(water.fluid.getColor());
    }

    private static float[] snowColor(String body) {
        CBT_Water water = CelestialTraits.getTrait(body, CBT_Water.class, true);
        if (water == null || water.fluid == null || water.fluid == Fluids.WATER) return new float[] {1.0F, 1.0F, 1.0F};
        return rainColor(water.fluid.getColor());
    }

    /** 1.7 {@code BiomeGenBase.getFloatTemperature(x, y, z)}. */
    static float temperature(float base, int x, int y, int z) {
        if (y > 64) {
            float f = (float) TEMPERATURE_NOISE.value((double) x * 1.0D / 8.0D, (double) z * 1.0D / 8.0D) * 4.0F;
            return base - (f + (float) y - 64.0F) * 0.05F / 30.0F;
        }
        return base;
    }

    private static void render(Precipitation.Frame frame) {
        ClientLevel world = frame.level();
        float partialTicks = frame.partialTick();
        float intensity = world.getRainLevel(partialTicks);
        if (intensity <= 0.0F) return;
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null) return;
        String body = CelestialTraits.bodyOf(world);
        if (!CBT_Weather.supportsWeather(body, true)) return;

        int timer = mc.player != null ? mc.player.tickCount : (int) (world.getGameTime() & Integer.MAX_VALUE);
        int playerX = Mth.floor(camera.getX());
        int playerY = Mth.floor(camera.getY());
        int playerZ = Mth.floor(camera.getZ());
        Vec3 interp = frame.camera();
        int playerHeight = Mth.floor(interp.y);
        int renderLayerCount = Math.min(15, frame.radius());
        float[] rainColor = rainColor(body);
        float[] snowColor = snowColor(body);

        // one batch per run of one texture, as 1.7 drew whenever the layer switched
        java.util.List<Float> run = new java.util.ArrayList<>();
        int layer = -1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int layerZ = playerZ - renderLayerCount; layerZ <= playerZ + renderLayerCount; ++layerZ) {
            for (int layerX = playerX - renderLayerCount; layerX <= playerX + renderLayerCount; ++layerX) {
                int rainCoord = (layerZ - playerZ + 16) * 32 + layerX - playerX + 16;
                float rainCoordX = RAIN_X[rainCoord] * 0.5F;
                float rainCoordY = RAIN_Y[rainCoord] * 0.5F;
                Biome biome = world.getBiome(pos.set(layerX, playerY, layerZ)).value();
                if (!biome.hasPrecipitation()) continue;

                int precipitationHeight = world.getHeight(Heightmap.Types.MOTION_BLOCKING, layerX, layerZ);
                int minHeight = playerY - renderLayerCount;
                int maxHeight = playerY + renderLayerCount;
                if (minHeight < precipitationHeight) minHeight = precipitationHeight;
                if (maxHeight < precipitationHeight) maxHeight = precipitationHeight;
                int layerY = precipitationHeight;
                if (precipitationHeight < playerHeight) layerY = playerHeight;
                if (minHeight == maxHeight) continue;

                RANDOM.setSeed(layerX * layerX * 3121 + layerX * 45238971 ^ layerZ * layerZ * 418711 + layerZ * 13761);
                float temperature = temperature(biome.getBaseTemperature(), layerX, minHeight, layerZ);
                boolean renderRain = temperature >= 0.15F; // getTemperatureAtHeight is the identity in 1.7

                pos.set(layerX, layerY, layerZ);
                int light = world.getBrightness(LightLayer.SKY, pos) << 20 | world.getBrightness(LightLayer.BLOCK, pos) << 4;
                double distX = layerX + 0.5D - camera.getX();
                double distZ = layerZ + 0.5D - camera.getZ();
                float intensityMod = (float) Math.sqrt(distX * distX + distZ * distZ) / renderLayerCount;

                if (renderRain) {
                    if (layer != 0) {
                        flush(frame, layer, run);
                        layer = 0;
                    }
                    int rainSeed = layerX * layerX * 3121 + layerX * 45238971 + layerZ * layerZ * 418711 + layerZ * 13761;
                    float rainOffset = ((timer + (rainSeed & 31)) + partialTicks) / 32.0F * (3.0F + RANDOM.nextFloat());
                    float alpha = ((1.0F - intensityMod * intensityMod) * 0.5F + 0.5F) * intensity;
                    column(run, interp, layerX, layerZ, rainCoordX, rainCoordY, minHeight, maxHeight, 0.0F, rainOffset, rainColor,
                            alpha, light);
                } else {
                    if (layer != 1) {
                        flush(frame, layer, run);
                        layer = 1;
                    }
                    float swayLoop = ((timer & 511) + partialTicks) / 512.0F;
                    float fallVariation = RANDOM.nextFloat() + timer * 0.01F * (float) RANDOM.nextGaussian();
                    float swayVariation = RANDOM.nextFloat() + timer * (float) RANDOM.nextGaussian() * 0.001F;
                    float alpha = ((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F) * intensity;
                    int snowLight = (light * 3 + 15728880) / 4;
                    column(run, interp, layerX, layerZ, rainCoordX, rainCoordY, minHeight, maxHeight, fallVariation,
                            swayLoop + swayVariation, snowColor, alpha, snowLight);
                }
            }
        }
        flush(frame, layer, run);
    }

    /**
     * One column's quad (1.7's four {@code addVertexWithUV} calls under {@code setTranslation(-interpX, -interpY, -interpZ)}),
     * 10 floats a vertex as {@link Precipitation.Frame#quads}, relative to the camera.
     */
    private static void column(java.util.List<Float> out, Vec3 cam, int x, int z, double cx, double cy, int minHeight,
                               int maxHeight, float u, float v, float[] color, float alpha, int light) {
        vertex(out, cam, x - cx + 0.5D, minHeight, z - cy + 0.5D, 0.0F + u, minHeight / 4.0F + v, color, alpha, light);
        vertex(out, cam, x + cx + 0.5D, minHeight, z + cy + 0.5D, 1.0F + u, minHeight / 4.0F + v, color, alpha, light);
        vertex(out, cam, x + cx + 0.5D, maxHeight, z + cy + 0.5D, 1.0F + u, maxHeight / 4.0F + v, color, alpha, light);
        vertex(out, cam, x - cx + 0.5D, maxHeight, z - cy + 0.5D, 0.0F + u, maxHeight / 4.0F + v, color, alpha, light);
    }

    private static void vertex(java.util.List<Float> out, Vec3 cam, double x, double y, double z, float u, float v,
                               float[] color, float alpha, int light) {
        out.add((float) (x - cam.x));
        out.add((float) (y - cam.y));
        out.add((float) (z - cam.z));
        out.add(u);
        out.add(v);
        out.add(color[0]);
        out.add(color[1]);
        out.add(color[2]);
        out.add(alpha);
        out.add((float) light);
    }

    private static void flush(Precipitation.Frame frame, int layer, java.util.List<Float> run) {
        if (layer < 0 || run.isEmpty()) {
            run.clear();
            return;
        }
        float[] v = new float[run.size()];
        for (int i = 0; i < v.length; i++) v[i] = run.get(i);
        frame.quads(layer == 0 ? RAIN_TEXTURE : SNOW_TEXTURE, v);
        run.clear();
    }
}
