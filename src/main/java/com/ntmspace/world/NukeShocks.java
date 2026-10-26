package com.ntmspace.world;

import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.platform.Lifecycle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * hbm-space's {@code CelestialNukeShockHandler}: every nuclear blast on a body leaves a shock on that body as seen from
 * space - a flash, an afterglow and a ring on its day side, clearing its clouds ({@code nuke.frag} and the nuke uniforms of
 * the atmosphere shaders). At most four per body, each kept 220 ticks, at a centre hashed from the blast's position and
 * the time and snapped to the 16 x 16 flash grid, with a strength by the blast radius (140 blocks = 1, at least 0.35).
 *
 * <p>Fired by HBM's {@code Environment.NUCLEAR_BLAST} (1.7: an MK5 blast entity joining the world, and
 * {@code ExplosionNukeSmall} with {@code miniNuke}). 1.7 sent the list in its every-tick perma-sync; the port sends the
 * whole list to every player when it changes and to a player who joins, and the client drops shocks older than 220 ticks
 * itself (what the next 1.7 sync would have done). Bodies are keyed by name, not 1.7's dimension id.
 */
public final class NukeShocks {

    private NukeShocks() {}

    static final long MAX_AGE = 220L;
    public static final int MAX_ACTIVE_SHOCKS = 4;
    static final float FLASH_GRID = 16.0F;

    /** {@code ShockStatus}: when (total world time), where on the body's disc (0..1) and how strong (0.35..1). */
    public record Shock(long time, float centerX, float centerY, float strength) {}

    private static final Map<String, List<Shock>> SERVER = new HashMap<>();
    private static final Map<String, List<Shock>> CLIENT = new HashMap<>();

    public record Payload(Map<String, List<Shock>> shocks) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.of(Payload.class, Ids.of(NtmSpace.MOD_ID, "nuke_shocks"),
            (payload, buf) -> {
                buf.writeVarInt(payload.shocks().size());
                for (Map.Entry<String, List<Shock>> entry : payload.shocks().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeVarInt(entry.getValue().size());
                    for (Shock shock : entry.getValue()) {
                        buf.writeLong(shock.time());
                        buf.writeFloat(shock.centerX());
                        buf.writeFloat(shock.centerY());
                        buf.writeFloat(shock.strength());
                    }
                }
            },
            buf -> {
                Map<String, List<Shock>> shocks = new HashMap<>();
                int bodies = buf.readVarInt();
                for (int i = 0; i < bodies; i++) {
                    String body = buf.readUtf();
                    int count = buf.readVarInt();
                    List<Shock> list = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) list.add(new Shock(buf.readLong(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
                    shocks.put(body, list);
                }
                return new Payload(shocks);
            });

    public static void init() {
        GolemNetwork.declarePlayToClient(CODEC);
        com.hbm.api.environment.Environment.NUCLEAR_BLAST.register(NukeShocks::trigger);
        Lifecycle.onPlayerJoin((player, first) -> send(player));
        Lifecycle.onServerStopping(server -> SERVER.clear());
    }

    public static void initClient() {
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> {
            CLIENT.clear();
            CLIENT.putAll(payload.shocks());
        });
    }

    /** {@code trigger}: a blast of {@code blastRadius} at x / z on {@code level}'s body. */
    public static void trigger(ServerLevel level, double x, double z, float blastRadius) {
        trigger(CelestialTraits.bodyOf(level), level.getServer(), level.getGameTime(), x, z, blastRadius);
    }

    /** {@link #trigger} on {@code body} at the server's time {@code now} (also the {@code /ntmspace skytrait nuke} test). */
    public static void trigger(String body, MinecraftServer server, long now, double x, double z, float blastRadius) {
        if (body == null) return;
        Shock shock = shock(x, z, now, blastRadius);
        List<Shock> list = SERVER.computeIfAbsent(body, k -> new ArrayList<>());
        prune(list, now);
        list.add(shock);
        while (list.size() > MAX_ACTIVE_SHOCKS) list.remove(0);
        NtmSpace.LOG.info("space_nuke_shock body={} cx={} cy={} strength={}", body, shock.centerX(), shock.centerY(),
                shock.strength());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
    }

    /** The shock a blast makes: 1.7's hashed, grid-snapped centre and its strength (pure, for tests). */
    public static Shock shock(double x, double z, long totalWorldTime, float blastRadius) {
        float rawX = 0.18F + hashToUnit(x * 0.03125D + z * 0.0125D + totalWorldTime * 0.017D) * 0.64F;
        float rawY = 0.22F + hashToUnit(x * -0.015D + z * 0.0275D + totalWorldTime * 0.011D + 19.0D) * 0.52F;
        float strength = Mth.clamp(blastRadius / 140.0F, 0.35F, 1.0F);
        return new Shock(totalWorldTime, quantizeCenter(rawX), quantizeCenter(rawY), strength);
    }

    private static void send(ServerPlayer player) {
        if (!GolemNetwork.canSendToPlayer(player, CODEC)) return;
        long now = player.level().getGameTime();
        Map<String, List<Shock>> copy = new HashMap<>();
        for (Map.Entry<String, List<Shock>> entry : SERVER.entrySet()) {
            prune(entry.getValue(), now);
            if (!entry.getValue().isEmpty()) copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        GolemNetwork.sendToPlayer(player, CODEC, new Payload(copy));
    }

    /** The body's live server list; trigger and sync own its expiry cadence. */
    public static List<Shock> serverShocks(String body) {
        var list = SERVER.get(body);
        return list == null ? Collections.emptyList() : list;
    }

    /** {@code getClientShocks}: the body's shocks on this client, without those 1.7's next sync would have dropped. */
    public static List<Shock> clientShocks(String body, long now) {
        List<Shock> list = CLIENT.get(body);
        if (list == null) return Collections.emptyList();
        prune(list, now);
        return list;
    }

    private static void prune(List<Shock> list, long now) {
        Iterator<Shock> iterator = list.iterator();
        while (iterator.hasNext()) if (now - iterator.next().time() > MAX_AGE) iterator.remove();
    }

    private static float hashToUnit(double value) {
        return (float) (value - Math.floor(value));
    }

    private static float quantizeCenter(float value) {
        float clamped = Mth.clamp(value, 0.05F, 0.95F);
        return (Mth.floor(clamped * FLASH_GRID) + 0.5F) / FLASH_GRID;
    }
}
