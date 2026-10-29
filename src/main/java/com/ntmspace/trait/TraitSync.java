package com.ntmspace.trait;

import com.ntmspace.client.CelestialTimeClient;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Deaths;
import dev.golem.core.world.EntityRemovals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The traits half of hbm-space's perma-sync packet ({@code PermaSyncHandler}): every body with saved traits and those
 * traits, by id. 1.7 sent it to every player every tick; the port sends it when a player enters a level and at the end
 * of any server tick in which a trait changed. The trait bodies are written by each trait's own
 * {@link CelestialBodyTrait#writeToBytes}.
 */
public final class TraitSync {

    private TraitSync() {
    }

    public record Payload(
            Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits) implements GolemPayload {
        @Override
        public Identifier id() {
            return CODEC.id();
        }
    }

    public static final GolemCodec<Payload> CODEC = GolemCodec.ofRegistry(Payload.class,
            Ids.of(NtmSpace.MOD_ID, "celestial_traits"),
            TraitSync::write, TraitSync::read);

    private static void write(Payload payload, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(payload.traits().size());
        for (Map.Entry<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> body : payload.traits()
                                                                                                           .entrySet()) {
            buf.writeUtf(body.getKey());
            List<CelestialBodyTrait> traits = new ArrayList<>(body.getValue().values());
            buf.writeVarInt(traits.size());
            for (CelestialBodyTrait trait : traits) {
                buf.writeUtf(trait.id());
                trait.writeToBytes(buf);
            }
        }
    }

    private static Payload read(RegistryFriendlyByteBuf buf) {
        Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits = new HashMap<>();
        int bodies = buf.readVarInt();
        for (int i = 0; i < bodies; i++) {
            String body = buf.readUtf();
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> set = new HashMap<>();
            int count = buf.readVarInt();
            for (int j = 0; j < count; j++) {
                CelestialBodyTrait.Type<?> type = CelestialBodyTrait.byId(buf.readUtf());
                if (type == null) throw new IllegalStateException("unknown celestial body trait in sync packet");
                CelestialBodyTrait trait = type.create();
                trait.readFromBytes(buf);
                set.put(trait.getClass(), trait);
            }
            traits.put(body, set);
        }
        return new Payload(traits);
    }

    /**
     * Common start-up: the payload, the server tick and the join sync.
     */
    public static void init() {
        CelestialTimeSync.init();
        GolemNetwork.declarePlayToClient(CODEC);
        Lifecycle.onServerStarted(CelestialTraits::serverStarted);
        Lifecycle.onServerStopping(server -> CelestialTraits.serverStopping());
        Lifecycle.onBeforeLevelTicks(CelestialTraits::serverTick);
        Lifecycle.onServerTick(TraitSync::tick);
        // 1.7 ModEventHandler.onLivingDeath: a kill counts for the body's invasion
        Deaths.onDeath((entity, unusedSource) -> CBT_Invasion.onDeath(entity));
        EntityRemovals.AFTER.register(CBT_Invasion::onRemoval);
        Lifecycle.onEntityLoad((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                CelestialTraits.attach(level);
                send(player);
            }
        });
    }

    /**
     * Client start-up: the handler.
     */
    public static void initClient() {
        CelestialTimeClient.init();
        GolemNetwork.registerPlayToClient(CODEC, (payload, context) -> CelestialTraits.receive(payload.traits()));
        dev.golem.core.client.ClientTicks.onEndTick(CelestialTraits::clientTick);
    }

    private static void tick(MinecraftServer server) {
        CelestialTraits.serverMaintenance(server);
        if (!CelestialTraits.takeChanged()) return;
        Payload payload = snapshot();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, payload);
        }
    }

    private static void send(ServerPlayer player) {
        if (GolemNetwork.canSendToPlayer(player, CODEC)) GolemNetwork.sendToPlayer(player, CODEC, snapshot());
    }

    /**
     * The saved traits, copied so the payload never shares objects with the save (an integrated server shares a JVM).
     */
    private static Payload snapshot() {
        Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> copy = new HashMap<>();
        for (Map.Entry<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> body : CelestialTraits.savedTraits()
                                                                                                                   .entrySet()) {
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> set = new HashMap<>();
            for (CelestialBodyTrait trait : body.getValue().values()) set.put(trait.getClass(), trait.copy());
            copy.put(body.getKey(), set);
        }
        return new Payload(copy);
    }
}
