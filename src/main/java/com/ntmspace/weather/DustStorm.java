package com.ntmspace.weather;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.Ids;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.CompatSavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Duna's dust storms, hbm-space's {@code WorldProviderDuna.updateWeather}: a storm of a random strength (0.25 .. 1)
 * blows for 12,000 .. 24,000 ticks, then the air stays clear for 12,000 .. 180,000 ticks, and so on. The server keeps
 * the countdown and the strength ({@code dustStormTimer}, {@code dustStormIntensity}, saved on Duna's level as 1.7 saved
 * them in its per-dimension data) and sends the strength to the players on Duna; the client eases towards it
 * ({@code dustStormSmoothed}, 0.8 % a tick) and from that draws the storm: blowing dust around the camera and a fog that
 * thickens with the square of the strength ({@link com.ntmspace.client.SkyWeatherClient},
 * {@code WorldProviderDuna.fogDensity}).
 *
 * <p>Differences: 1.7 advanced the countdown only while Duna's dimension was loaded (players there or forced chunks);
 * the port advances it while players are on Duna, as modern levels never unload (a chunk loader no longer keeps the
 * storm going). A completed night clears the storm and starts a new clear countdown, through the same completed-sleep
 * hook that resets the body's weather and local morning. Daytime queries answer false during a
 * storm of strength 0.2 or more through {@link com.ntmspace.world.SpaceSkies}, independently of cached sky brightness.
 * A fresh save starts with strength 1 and an expired countdown, which the
 * first tick turns into a long clear spell, as in 1.7.
 */
public final class DustStorm extends CompatSavedData<DustStorm> {

    static final String ID = NtmSpace.MOD_ID + ":dust_storm";

    /** {@code dustStormTimer}: ticks until the weather changes. */
    public int timer = 0;
    /** {@code dustStormIntensity}: 0 = clear, 0.25 .. 1 = a storm. */
    public float intensity = 1;

    /** The strength last delivered to the client, before its visual easing. */
    public static float clientIntensity;

    public DustStorm() {}

    private DustStorm(int timer, float intensity) {
        this.timer = timer;
        this.intensity = intensity;
    }

    public static DustStorm get(ServerLevel duna) {
        return CompatSavedData.get(duna, ID, DustStorm::new, CODEC);
    }

    public static boolean obscuresDaylight(Level level) {
        return level.dimension().equals(SpaceDimensions.DUNA)
                && (level instanceof ServerLevel server ? get(server).intensity : clientIntensity) >= 0.2F;
    }

    public static void acceptSync(float intensity) {
        clientIntensity = intensity;
    }

    /** Delivers the current strength when a player arrives in Duna's level. */
    public static void sync(ServerPlayer player, ServerLevel duna) {
        if (duna.dimension().equals(SpaceDimensions.DUNA)) {
            GolemNetwork.sendToPlayer(player, CODEC_PAYLOAD, new Payload(get(duna).intensity));
        }
    }

    /** The 1.7 server half of {@code updateWeather}, one tick; {@code rand} is the level's random. */
    public void tick(RandomSource rand) {
        if (timer <= 0) {
            if (intensity >= 0.05F) {
                intensity = 0;
                timer = rand.nextInt(168000) + 12000;
            } else {
                intensity = rand.nextFloat() * 0.75F + 0.25F;
                timer = rand.nextInt(12000) + 12000;
            }
        }
        timer--;
        setDirty();
    }

    @Override
    protected Codec<DustStorm> codec() {
        return CODEC;
    }

    public static final Codec<DustStorm> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("dustStormTimer", 0).forGetter(s -> s.timer),
            Codec.FLOAT.optionalFieldOf("dustStormIntensity", 1F).forGetter(s -> s.intensity)
    ).apply(i, DustStorm::new));

    // ---------------------------------------------------------------------------------------------------------------
    // ticking and sync

    /** The strength sent to a client (1.7 {@code serialize}: the server's {@code dustStormIntensity}). */
    public record Payload(float intensity) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC_PAYLOAD.id();
        }
    }

    public static final GolemCodec<Payload> CODEC_PAYLOAD = GolemCodec.of(Payload.class, Ids.of(NtmSpace.MOD_ID, "dust_storm"),
            (payload, buf) -> buf.writeFloat(payload.intensity()), buf -> new Payload(buf.readFloat()));

    private static final Map<UUID, Float> SENT = new HashMap<>();

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC_PAYLOAD);
        Lifecycle.onServerTick(DustStorm::serverTick);
        Lifecycle.onServerStopping(server -> SENT.clear());
        CompatCommands.server(DustStorm::registerCommand);
    }

    private static void serverTick(MinecraftServer server) {
        ServerLevel duna = server.getLevel(SpaceDimensions.DUNA);
        if (duna == null) return;
        // 1.7 ticked a loaded dimension only, which a dimension without players soon stopped being
        boolean loaded = !duna.players().isEmpty();
        DustStorm storm = get(duna);
        if (loaded) storm.tick(duna.getRandom());
        for (ServerPlayer player : duna.players()) {
            Float sent = SENT.get(player.getUUID());
            if (sent != null && sent == storm.intensity) continue;
            if (!GolemNetwork.canSendToPlayer(player, CODEC_PAYLOAD)) continue;
            SENT.put(player.getUUID(), storm.intensity);
            GolemNetwork.sendToPlayer(player, CODEC_PAYLOAD, new Payload(storm.intensity));
        }
        // a player who left Duna is sent the strength again when back (it may have changed meanwhile)
        SENT.keySet().removeIf(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            return player == null || player.level() != duna;
        });
    }

    /** WorldProviderDuna.resetRainAndThunder: a slept-through night starts a fresh clear interval. */
    public static void sleptThrough(ServerLevel level) {
        if (!level.dimension().equals(SpaceDimensions.DUNA)) return;
        var storm = get(level);
        storm.intensity = 0F;
        storm.timer = level.getRandom().nextInt(168000) + 12000;
        storm.setDirty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // /ntmspace duststorm [strength [ticks]] (test and harness)

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("duststorm")
                        .executes(context -> report(context.getSource()))
                        .then(Commands.argument("strength", FloatArgumentType.floatArg(0, 1))
                                .executes(context -> set(context.getSource(), FloatArgumentType.getFloat(context, "strength"), 12000))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                        .executes(context -> set(context.getSource(), FloatArgumentType.getFloat(context, "strength"),
                                                IntegerArgumentType.getInteger(context, "ticks")))))));
    }

    private static int set(CommandSourceStack source, float strength, int ticks) {
        ServerLevel duna = duna(source);
        if (duna == null) return 0;
        DustStorm storm = get(duna);
        storm.intensity = strength;
        storm.timer = ticks;
        storm.setDirty();
        return report(source);
    }

    private static int report(CommandSourceStack source) {
        ServerLevel duna = duna(source);
        if (duna == null) return 0;
        DustStorm storm = get(duna);
        String message = "space_duststorm intensity=" + storm.intensity + " timer=" + storm.timer;
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static @Nullable ServerLevel duna(CommandSourceStack source) {
        ServerLevel duna = source.getServer().getLevel(SpaceDimensions.DUNA);
        if (duna == null) source.sendFailure(Component.literal("Duna is not loaded"));
        return duna;
    }
}
