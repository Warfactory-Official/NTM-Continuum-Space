package com.ntmspace.weather;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.world.CelestialBodies;
import dev.golem.core.world.TimeOfDay;
import java.util.Locale;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.trait.CBT_Weather;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.Weathers;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A celestial body's weather applied to its level, hbm-space's {@code WorldProviderCelestial.updateWeather}: each tick
 * the level's rain and thunder strengths are the body's {@link CBT_Weather} ({@code worldObj.rainingStrength =
 * weather.rainStrength}, the thunder likewise), and a body without a weather cycle ({@link CBT_Weather#supportsWeather}:
 * air above 0.5 atm and a water table - Eve, Laythe and Tekto by default) has none at all, whatever the overworld's
 * weather. The orbit has none either ({@code WorldProviderOrbit.updateWeather}). Applied through Golem Core's
 * {@link Weathers}, so the client's rain, the sky darkening, lightning (vanilla's, where a biome rains: 1.7
 * {@code canDoLightning} = the weather cycle) and every other reader of the level's weather follow it.
 *
 * <p>Kerbin owns the overworld's cycle too. The regular {@code /weather} command changes the body's trait, and
 * sleeping through clears that cycle immediately. An orbital command controls the station's orbited body while
 * the orbit stays dry; other non-celestial levels keep vanilla's weather and commands. Thatmo reads Kerbin's cycle,
 * as in 1.7 ({@link SpaceSkies#body}).
 */
public final class BodyWeather {

    private BodyWeather() {}

    private static final Random COMMAND_RANDOM = new Random();

    public static void init() {
        Weathers.register(BodyWeather::weather);
        Weathers.SLEPT_THROUGH.register(BodyWeather::sleptThrough);
        Weathers.COMMANDS.register(BodyWeather::command);
        CompatCommands.server(BodyWeather::registerCommand);
    }

    /** The weather of {@code level}, or null where no celestial body supplies it. */
    public static @Nullable Weathers.State weather(ServerLevel level) {
        if (Orbit.inOrbit(level)) return new Weathers.State(0.0F, 0.0F);
        SolarSystem.Body body = SpaceSkies.body(level);
        if (body == null) return null;
        return weatherOfBody(body.name);
    }

    /** The weather a body's level has: its {@link CBT_Weather}'s strengths, none without a weather cycle. */
    public static Weathers.State weatherOfBody(String body) {
        if (!CBT_Weather.supportsWeather(body, false)) return new Weathers.State(0.0F, 0.0F);
        CBT_Weather weather = CelestialTraits.getTrait(body, CBT_Weather.class, false);
        if (weather == null) return new Weathers.State(0.0F, 0.0F);
        return new Weathers.State(weather.rainStrength, weather.thunderStrength);
    }

    /** 1.7 resetRainAndThunder: clear the body, then reset only its own advancing clock to local morning. */
    private static void sleptThrough(ServerLevel level) {
        var body = SpaceSkies.body(level);
        if (body == null) return;
        var weather = CBT_Weather.ensureTrait(body.name);
        if (weather != null) {
            weather.forceClear(COMMAND_RANDOM, COMMAND_RANDOM.nextInt(168000) + 12000);
            CelestialTraits.markChanged();
        }
        if (TimeOfDay.hasOwnClock(level) && TimeOfDay.advancing(level)) {
            TimeOfDay.set(level, 0);
        }
        Weathers.advance(level);
    }

    /** The regular /weather command uses ticks; the namespaced command below continues accepting seconds. */
    public static @Nullable Integer command(CommandSourceStack source, Weathers.Mode mode, int duration)
            throws CommandSyntaxException {
        var level = source.getLevel();
        var body = commandBody(source);
        if (body == null) return null;
        var weather = CBT_Weather.ensureTrait(body.name);
        if (weather == null || !CBT_Weather.supportsWeather(body.name, false)) {
            throw new SimpleCommandExceptionType(
                    Component.translatable("commands.hbmspace.weather.no_cycle")).create();
        }
        var ticks = duration < 0 ? (300 + level.getRandom().nextInt(600)) * 20 : duration;
        var name = mode.name().toLowerCase(Locale.ROOT);
        apply(weather, name, ticks);
        source.sendSuccess(() -> Component.translatable("commands.weather.set." + name), true);
        return ticks;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // /ntmspace weather (1.7's /weather on a planet)

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("weather")
                        .executes(context -> report(context.getSource()))
                        .then(mode("clear")).then(mode("rain")).then(mode("thunder")))
                .then(Commands.literal("skytime")
                        .then(phase("noon", 0.0F)).then(phase("sunset", 0.245F)).then(phase("night", 0.5F))));
    }

    /**
     * {@code /ntmspace skytime noon|sunset|night} (test and harness): sets the body's own clock to the first time of its
     * solar day at which the sun stands at that angle (0 noon, 0.245 just above the western horizon, 0.5 midnight).
     * Logs {@code space_skytime dim=<id> time=<ticks> angle=<a>}.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> phase(String name, float target) {
        return Commands.literal(name).executes(context -> {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();
            SolarSystem.Body body = SpaceSkies.body(level);
            if (body == null || SpaceSkies.isKerbin(level) || !dev.golem.core.world.TimeOfDay.hasOwnClock(level)) {
                source.sendFailure(Component.literal("Not on a body with its own clock (use /time set on Kerbin)"));
                return 0;
            }
            long time = timeAt(body, target);
            dev.golem.core.world.TimeOfDay.set(level, time);
            String message = "space_skytime dim=" + dev.golem.core.Ids.keyId(level.dimension()) + " time=" + time
                    + " angle=" + SolarSystem.solarAngle(body, time, 0.0F);
            NtmSpace.LOG.info(message);
            source.sendSuccess(() -> Component.literal(message), false);
            return 1;
        });
    }

    /** The first tick of the body's first solar day at which its solar angle is nearest {@code target}. */
    public static long timeAt(SolarSystem.Body body, float target) {
        long day = (long) Math.ceil(SolarSystem.dayLength(body));
        long best = 0;
        double bestError = Double.MAX_VALUE;
        long step = Math.max(1, day / 4096);
        for (long t = 0; t < day; t += step) {
            double error = angleError(SolarSystem.solarAngle(body, t, 0.0F), target);
            if (error < bestError) {
                bestError = error;
                best = t;
            }
        }
        for (long t = Math.max(0, best - step); t <= best + step; t++) {
            double error = angleError(SolarSystem.solarAngle(body, t, 0.0F), target);
            if (error < bestError) {
                bestError = error;
                best = t;
            }
        }
        return best;
    }

    private static double angleError(float angle, float target) {
        double d = Math.abs(angle - target) % 1.0;
        return Math.min(d, 1.0 - d);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> mode(String mode) {
        return Commands.literal(mode)
                .executes(context -> set(context.getSource(), mode, (300 + new Random().nextInt(600)) * 20))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1_000_000))
                        .executes(context -> set(context.getSource(), mode,
                                IntegerArgumentType.getInteger(context, "seconds") * 20)));
    }

    /** {@code handlePlanetaryWeatherCommand}: force the body's weather for {@code duration} ticks. */
    private static int set(CommandSourceStack source, String mode, int duration) {
        var body = commandBody(source);
        String name = body == null ? null : body.name;
        CBT_Weather weather = name == null ? null : CBT_Weather.ensureTrait(name);
        if (weather == null || !CBT_Weather.supportsWeather(name, false)) {
            source.sendFailure(Component.literal("This celestial body has no weather cycle."));
            return 0;
        }
        apply(weather, mode, duration);
        source.sendSuccess(() -> Component.translatable("commands.weather.set." + mode), true);
        return report(source);
    }

    private static @Nullable SolarSystem.Body commandBody(CommandSourceStack source) {
        var pos = source.getPosition();
        return commandBody(source.getServer(), source.getLevel().dimension(), pos.x, pos.z);
    }

    /** A station's command reaches its actual orbited body; the orbit level's own visual weather remains dry. */
    public static @Nullable SolarSystem.Body commandBody(MinecraftServer server,
            ResourceKey<Level> dimension, double x, double z) {
        if (dimension.equals(Orbit.ORBIT)) {
            return OrbitalStation.getStationFromPosition(server,
                    Mth.floor(x), Mth.floor(z)).orbiting;
        }
        var body = CelestialBodies.of(dimension);
        return body == null ? null : SolarSystem.body(body.name());
    }

    private static void apply(CBT_Weather weather, String mode, int duration) {
        switch (mode) {
            case "clear" -> weather.forceClear(COMMAND_RANDOM, duration);
            case "rain" -> weather.forceRain(COMMAND_RANDOM, duration);
            default -> weather.forceThunder(duration);
        }
        CelestialTraits.markChanged();
    }

    private static int report(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Weathers.State state = weather(level);
        String message = "space_weather dim=" + dev.golem.core.Ids.keyId(level.dimension())
                + " provided=" + (state != null)
                + " rain=" + level.getRainLevel(1.0F) + " thunder=" + level.getThunderLevel(1.0F)
                + " raining=" + level.isRaining() + " thundering=" + level.isThundering();
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
