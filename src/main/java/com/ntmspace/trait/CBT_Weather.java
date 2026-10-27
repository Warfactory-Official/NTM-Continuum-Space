package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code CBT_Weather}: a body's own rain and thunder cycle, for bodies with an atmosphere above 0.5 atm and a
 * water table ({@link #supportsWeather}). The cycle (durations, the pressure-dependent lightning severity and activity,
 * the strength ramps) is ported member for member and ticked once per server tick ({@link #updateGlobalWeather}); showing
 * it in a level (1.7's {@code WeatherProviderCelestial}) belongs to the sky/weather port.
 */
public class CBT_Weather extends CelestialBodyTrait {

    private static final Random WEATHER_RANDOM = new Random();
    private static final int SAVE_INTERVAL = 200;
    private static final float LIGHTNING_CLOUD_PRESSURE = 0.5F;
    private static final float LIGHTNING_HAZE_PRESSURE = 3.0F;
    private static final float LIGHTNING_OPAQUE_PRESSURE = 5.0F;
    private static final float CLOUD_LIGHTNING_ACTIVITY = 0.55F;
    private static final float HAZE_LIGHTNING_ACTIVITY = 0.82F;
    private static final int[][] LIGHTNING_BIOME_SAMPLES = new int[][] {
        {0, 0},
        {512, 0},
        {-512, 0},
        {0, 512},
        {0, -512}
    };

    public boolean raining;
    public boolean thundering;
    public boolean canSpawnLightning = true;
    public int rainTime;
    public int thunderTime;
    public float prevRainStrength;
    public float rainStrength;
    public float prevThunderStrength;
    public float thunderStrength;

    private long lastUpdateTick = Long.MIN_VALUE;

    public long lastUpdateTick() {
        return lastUpdateTick;
    }

    /** 1.7: no gas giant ({@code body.gas}; none of the port's bodies with weather is one), air above 0.5 atm, a water table. */
    public static boolean supportsWeather(String body, boolean client) {
        if (body == null) return false;
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(body, CBT_Atmosphere.class, client);
        CBT_Water water = CelestialTraits.getTrait(body, CBT_Water.class, client);
        return atmosphere != null && atmosphere.getPressure() > 0.5D && water != null && water.fluid != null;
    }

    /** Server: the body's weather, created on first use where the body supports weather. */
    public static @Nullable CBT_Weather ensureTrait(String body) {
        if (body == null) return null;
        CBT_Weather weather = CelestialTraits.getTrait(body, CBT_Weather.class, false);
        if (weather == null && supportsWeather(body, false)) {
            CelestialTraits.modifyTraits(body, new CBT_Weather());
            weather = CelestialTraits.getTrait(body, CBT_Weather.class, false);
        }
        return weather;
    }

    /** Once per server tick (1.7 {@code ModEventHandler.onServerTick}, phase START). */
    public static void updateGlobalWeather(long tick) {
        boolean dirty = false;
        for (String body : CelestialTraits.allBodies()) {
            CBT_Weather weather = ensureTrait(body);
            if (weather == null) continue;
            ServerLevel level = CelestialTraits.levelOf(body);
            Random random = WEATHER_RANDOM; // 1.7 used world.rand when the dimension was loaded
            float rain = weather.rainStrength, thunder = weather.thunderStrength;
            if (weather.updateForTick(tick, random, body, level)) dirty = true;
            // 1.7 synced every tick; the port syncs on change, and a ramping strength is a change for the client
            if (rain != weather.rainStrength || thunder != weather.thunderStrength) dirty = true;
        }
        if (dirty) CelestialTraits.markChanged();
    }

    private static float getAtmospherePressure(String body, boolean client) {
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(body, CBT_Atmosphere.class, client);
        return atmosphere != null ? Math.max(0.0F, (float) atmosphere.getPressure()) : 0.0F;
    }

    public static float getLightningSeverity(String body, boolean client) {
        if (!supportsWeather(body, client)) return 0.0F;
        float pressure = getAtmospherePressure(body, client);
        if (pressure > LIGHTNING_HAZE_PRESSURE) {
            float hazeMix = Mth.clamp((pressure - LIGHTNING_HAZE_PRESSURE) / (LIGHTNING_OPAQUE_PRESSURE - LIGHTNING_HAZE_PRESSURE), 0.0F, 1.0F);
            return Mth.clamp(0.65F + hazeMix * 0.35F, 0.65F, 1.0F);
        }
        return Mth.clamp((pressure - LIGHTNING_CLOUD_PRESSURE) / (LIGHTNING_HAZE_PRESSURE - LIGHTNING_CLOUD_PRESSURE) * 0.65F, 0.0F, 0.65F);
    }

    public static float getLightningActivityFactor(String body, boolean client) {
        if (!supportsWeather(body, client)) return 0.0F;
        float pressure = getAtmospherePressure(body, client);
        if (pressure > LIGHTNING_HAZE_PRESSURE) {
            float hazeMix = Mth.clamp((pressure - LIGHTNING_HAZE_PRESSURE) / (LIGHTNING_OPAQUE_PRESSURE - LIGHTNING_HAZE_PRESSURE), 0.0F, 1.0F);
            return Mth.clamp(HAZE_LIGHTNING_ACTIVITY + hazeMix * (1.0F - HAZE_LIGHTNING_ACTIVITY), HAZE_LIGHTNING_ACTIVITY, 1.0F);
        }
        float cloudMix = Mth.clamp((pressure - LIGHTNING_CLOUD_PRESSURE) / (LIGHTNING_HAZE_PRESSURE - LIGHTNING_CLOUD_PRESSURE), 0.0F, 1.0F);
        return Mth.clamp(CLOUD_LIGHTNING_ACTIVITY + cloudMix * (HAZE_LIGHTNING_ACTIVITY - CLOUD_LIGHTNING_ACTIVITY), CLOUD_LIGHTNING_ACTIVITY, HAZE_LIGHTNING_ACTIVITY);
    }

    private static int getStormDuration(Random rand, float lightningSeverity) {
        int baseDuration = rand.nextInt(12000) + 3600;
        return Math.max(1200, Mth.floor(baseDuration * (0.95F + lightningSeverity * 0.45F)));
    }

    private static int getRainDuration(Random rand) {
        return rand.nextInt(12000) + 12000;
    }

    private static int getClearDuration(Random rand) {
        return rand.nextInt(168000) + 12000;
    }

    private static int getThunderClearDuration(Random rand, float lightningSeverity) {
        int baseDuration = getClearDuration(rand);
        return Math.max(2400, Mth.floor(baseDuration * (1.05F - lightningSeverity * 0.45F)));
    }

    public void forceClear(Random rand, int duration) {
        raining = false;
        thundering = false;
        rainTime = Math.max(1, duration);
        thunderTime = getClearDuration(rand);
        prevRainStrength = 0.0F;
        rainStrength = 0.0F;
        prevThunderStrength = 0.0F;
        thunderStrength = 0.0F;
    }

    public void forceRain(Random rand, int duration) {
        raining = true;
        thundering = false;
        rainTime = Math.max(1, duration);
        thunderTime = getClearDuration(rand);
    }

    public void forceThunder(int duration) {
        raining = true;
        thundering = true;
        rainTime = Math.max(1, duration);
        thunderTime = Math.max(1, duration);
    }

    /**
     * 1.7: true if any of five sample biomes can spawn lightning ({@code BiomeGenBase.canSpawnLightningBolt}: rain and no
     * snow). Modern biomes say rain with {@code hasPrecipitation}; the snow half has no version-neutral spelling and is not
     * checked. An unloaded level answers true, as 1.7 did.
     */
    private static boolean sampleCanSpawnLightning(@Nullable ServerLevel level) {
        if (level == null) return true;
        for (int[] sample : LIGHTNING_BIOME_SAMPLES) {
            if (level.getBiome(new BlockPos(sample[0], 64, sample[1])).value().hasPrecipitation()) return true;
        }
        return false;
    }

    public boolean updateForTick(long tick, Random rand, String body, @Nullable ServerLevel level) {
        if (lastUpdateTick == tick) return false;
        lastUpdateTick = tick;

        if (!supportsWeather(body, false)) {
            boolean hadWeather = rainTime != 0 || thunderTime != 0 || canSpawnLightning || raining || thundering
                    || prevRainStrength > 0.0F || rainStrength > 0.0F || prevThunderStrength > 0.0F || thunderStrength > 0.0F;
            rainTime = 0;
            thunderTime = 0;
            canSpawnLightning = false;
            raining = false;
            thundering = false;
            prevRainStrength = 0.0F;
            rainStrength = 0.0F;
            prevThunderStrength = 0.0F;
            thunderStrength = 0.0F;
            return hadWeather;
        }

        boolean stateChanged = false;
        float lightningSeverity = getLightningSeverity(body, false);
        boolean lightningAllowed = sampleCanSpawnLightning(level);
        if (canSpawnLightning != lightningAllowed) {
            canSpawnLightning = lightningAllowed;
            stateChanged = true;
        }

        if (!canSpawnLightning && thundering) {
            thundering = false;
            thunderTime = getThunderClearDuration(rand, lightningSeverity);
            stateChanged = true;
        }

        if (thunderTime <= 0) {
            thunderTime = thundering ? getStormDuration(rand, lightningSeverity) : getThunderClearDuration(rand, lightningSeverity);
            stateChanged = true;
        } else {
            thunderTime--;
            if (thunderTime <= 0) {
                thundering = canSpawnLightning && !thundering;
                thunderTime = thundering ? getStormDuration(rand, lightningSeverity) : getThunderClearDuration(rand, lightningSeverity);
                stateChanged = true;
            }
        }

        prevThunderStrength = thunderStrength;
        thunderStrength = Mth.clamp(thunderStrength + (thundering ? 0.01F : -0.01F), 0.0F, 1.0F);

        if (rainTime <= 0) {
            rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
            stateChanged = true;
        } else {
            rainTime--;
            if (rainTime <= 0) {
                raining = !raining;
                if (!raining) {
                    thundering = false;
                    thunderTime = getThunderClearDuration(rand, lightningSeverity);
                }
                rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
                stateChanged = true;
            }
        }

        prevRainStrength = rainStrength;
        rainStrength = Mth.clamp(rainStrength + (raining ? 0.01F : -0.01F), 0.0F, 1.0F);

        // 1.7 mixed the dimension number into the periodic save; the body name's hash spreads the bodies the same way
        return stateChanged || Math.floorMod(body.hashCode() + (int) tick, SAVE_INTERVAL) == 0;
    }

    public float getRainStrength(float partialTicks) {
        return Mth.clamp(prevRainStrength + (rainStrength - prevRainStrength) * partialTicks, 0.0F, 1.0F);
    }

    public float getThunderStrength(float partialTicks) {
        return Mth.clamp(prevThunderStrength + (thunderStrength - prevThunderStrength) * partialTicks, 0.0F, 1.0F);
    }

    public float getWeightedThunderStrength(float partialTicks) {
        return getRainStrength(partialTicks) * getThunderStrength(partialTicks);
    }

    @Override
    public CBT_Weather copy() {
        CBT_Weather copy = new CBT_Weather();
        copy.raining = raining;
        copy.thundering = thundering;
        copy.canSpawnLightning = canSpawnLightning;
        copy.rainTime = rainTime;
        copy.thunderTime = thunderTime;
        copy.prevRainStrength = prevRainStrength;
        copy.rainStrength = rainStrength;
        copy.prevThunderStrength = prevThunderStrength;
        copy.thunderStrength = thunderStrength;
        return copy;
    }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(raining);
        buf.writeBoolean(thundering);
        buf.writeBoolean(canSpawnLightning);
        buf.writeInt(rainTime);
        buf.writeInt(thunderTime);
        buf.writeFloat(prevRainStrength);
        buf.writeFloat(rainStrength);
        buf.writeFloat(prevThunderStrength);
        buf.writeFloat(thunderStrength);
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        raining = buf.readBoolean();
        thundering = buf.readBoolean();
        canSpawnLightning = buf.readBoolean();
        rainTime = buf.readInt();
        thunderTime = buf.readInt();
        prevRainStrength = buf.readFloat();
        rainStrength = buf.readFloat();
        prevThunderStrength = buf.readFloat();
        thunderStrength = buf.readFloat();
    }

    private record Saved(boolean raining, boolean thundering, boolean canSpawnLightning, int rainTime, int thunderTime,
                         float prevRain, float rain, float prevThunder, float thunder) {}

    static Codec<CBT_Weather> codec() {
        return RecordCodecBuilder.<Saved>create(i -> i.group(
                Codec.BOOL.optionalFieldOf("raining", false).forGetter(Saved::raining),
                Codec.BOOL.optionalFieldOf("thundering", false).forGetter(Saved::thundering),
                Codec.BOOL.optionalFieldOf("canSpawnLightning", true).forGetter(Saved::canSpawnLightning),
                Codec.INT.optionalFieldOf("rainTime", 0).forGetter(Saved::rainTime),
                Codec.INT.optionalFieldOf("thunderTime", 0).forGetter(Saved::thunderTime),
                Codec.FLOAT.optionalFieldOf("prevRainStrength", 0F).forGetter(Saved::prevRain),
                Codec.FLOAT.optionalFieldOf("rainStrength", 0F).forGetter(Saved::rain),
                Codec.FLOAT.optionalFieldOf("prevThunderStrength", 0F).forGetter(Saved::prevThunder),
                Codec.FLOAT.optionalFieldOf("thunderStrength", 0F).forGetter(Saved::thunder)
        ).apply(i, Saved::new)).xmap(s -> {
            CBT_Weather w = new CBT_Weather();
            w.raining = s.raining();
            w.thundering = s.thundering();
            w.canSpawnLightning = s.canSpawnLightning();
            w.rainTime = s.rainTime();
            w.thunderTime = s.thunderTime();
            w.prevRainStrength = s.prevRain();
            w.rainStrength = s.rain();
            w.prevThunderStrength = s.prevThunder();
            w.thunderStrength = s.thunder();
            return w;
        }, w -> new Saved(w.raining, w.thundering, w.canSpawnLightning, w.rainTime, w.thunderTime,
                w.prevRainStrength, w.rainStrength, w.prevThunderStrength, w.thunderStrength));
    }
}
