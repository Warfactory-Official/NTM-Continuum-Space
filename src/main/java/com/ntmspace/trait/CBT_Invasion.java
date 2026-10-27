package com.ntmspace.trait;

import dev.golem.core.world.Compat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import com.hbm.entity.mob.EntityUFO;
import com.ntmspace.entity.missile.CombatDropPods;
import com.ntmspace.entity.missile.EntityCombatDropPod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code CBT_Invasion}: the scripted alien invasion of a body - a countdown, then waves that end with a boss.
 * The state machine (countdown, waves, kill requirements, burst timing, wave announcements, the end) is ported here; what
 * it spawns (drop pods with glyphids, siege UFOs, the boss UFO) and what a kill is worth belong to the mob port and are
 * reached through {@link #SPAWNER} and {@link #KILLS}, which do nothing until a mob family sets them.
 */
public class CBT_Invasion extends CelestialBodyTrait {

    /** What the invasion spawns; the mob port supplies it. */
    public interface Spawner {
        /** 1.7 {@code spawnCattle}: one drop pod near a random player, payload chosen by {@code wave}. */
        void dropPod(ServerLevel level, CBT_Invasion invasion);
        /** 1.7 {@code spawnAttempt}'s spawn: a siege UFO or craft near a random player. */
        @Nullable LivingEntity ufo(ServerLevel level, CBT_Invasion invasion);
        /** 1.7 {@code spawnBoss}: the boss UFO above a random player. */
        @Nullable EntityUFO boss(ServerLevel level, CBT_Invasion invasion);
    }

    /** 1.7 {@code onKill}: how many kills {@code entity} counts for (0: none), and whether it is the final boss. */
    public interface KillValue {
        int kills(LivingEntity entity);
        boolean endsInvasion(LivingEntity entity, int wave);
    }

    public static volatile @Nullable Spawner SPAWNER;
    /**
     * 1.7 {@code spawnCattle} on its own, so the drop-pod port and the UFO/boss port can each install their part; when
     * set it is used instead of {@link Spawner#dropPod}.
     */
    public static volatile @Nullable DropPod DROP_POD;

    /** The drop-pod part of {@link Spawner}. */
    public interface DropPod {
        void dropPod(ServerLevel level, CBT_Invasion invasion);
    }
    public static volatile @Nullable KillValue KILLS;

    public int wave;
    public int kills;
    public int killreq;
    public double waveTime;
    public boolean isInvading;
    public int lastSpawns; // prevent over-lagging the server
    public int spawndelay;
    public int podBurstCounter = 0;
    public int podCooldown = 0;
    public boolean bossSpawned = false;
    public boolean warningPlayed;
    private @Nullable ServerBossEvent bar;

    public CBT_Invasion() {}

    public CBT_Invasion(int wave, double waveTime, boolean isInvading) {
        this.wave = wave;
        this.waveTime = waveTime;
        this.isInvading = isInvading;
    }

    public void prepare() {
        if (!isInvading && waveTime >= 0) {
            waveTime--;
            warningPlayed = true;
            if (waveTime <= 5) warningPlayed = false;
            if (waveTime <= 0) isInvading = true;
        }
    }

    /** The client half of 1.7 {@code update}: the warning and the meteors (the war family's client installs it). */
    public interface ClientEffects {
        /** 1.7: {@code hbm:alarm.ping} at volume 10 and "Incoming Invasion!" in red to the player. */
        void warning();
        /** 1.7 {@code WorldProviderCelestial.Meteor.addMeteor()}. */
        void meteor();
    }

    public static volatile @Nullable ClientEffects CLIENT;
    public static Runnable CLIENT_WARNING = () -> {
        var effects = CLIENT;
        if (effects != null) effects.warning();
    };
    /** 1.7 kept a {@code new Random()} per trait object; the client's traits are re-made on every sync, so one is shared. */
    private static final java.util.Random CLIENT_RANDOM = new java.util.Random();

    @Override
    public void update(boolean remote, String body) {
        if (remote) {
            ClientEffects effects = CLIENT;
            if (CLIENT_RANDOM.nextInt(Math.max(1, 5 - wave)) == 0 && isInvading && effects != null) {
                effects.meteor();
            }
            return;
        }
        serverUpdate(body, true);
    }

    /** The lifecycle runs simulation before levels and refreshes the audience separately on server END. */
    public void simulate(String body) {
        serverUpdate(body, false);
    }

    private void serverUpdate(String body, boolean maintainBar) {
        prepare();
        CelestialTraits.markChanged();
        if (maintainBar) {
            var server = CelestialTraits.server();
            if (server != null) updateBar(server, body);
        }
        if (isInvading) {
            ServerLevel level = CelestialTraits.levelOf(body);
            if (level == null || level.players().isEmpty()) return;
            logicTick(level);
            handleBurstSpawning(level);
            spawnAttempt(level);
        }
    }

    /** 1.7 {@code handleBurstSpawning} (public for the GameTests). */
    public void handleBurstSpawning(ServerLevel level) {
        if (wave > 3) return;
        if (podCooldown > 0) {
            podCooldown--;
            return;
        }
        if (level.getGameTime() % 10 + level.getRandom().nextInt(3) == 0) {
            DropPod pod = DROP_POD;
            Spawner spawner = SPAWNER;
            if (pod != null) pod.dropPod(level, this);
            else if (spawner != null) spawner.dropPod(level, this);
            podBurstCounter++;
            if (podBurstCounter >= 3 + (wave - 1)) {
                podBurstCounter = 0;
                podCooldown = 500;
            }
        }
    }

    /** 1.7 {@code logicTick} (public for the GameTests). */
    public @Nullable EntityUFO logicTick(ServerLevel level) {
        if (!isInvading) return null;
        switch (wave) {
            case 0 -> advanceWave(level);
            case 1 -> { killreq = 20; if (kills >= killreq) advanceWave(level); }
            case 2 -> { killreq = 100; if (kills >= killreq) advanceWave(level); }
            case 3 -> { killreq = 150; if (kills >= killreq) advanceWave(level); }
            case 4 -> {
                killreq = 1;
                if (!bossSpawned) {
                    Spawner spawner = SPAWNER;
                    var boss = spawner == null ? null : spawner.boss(level, this);
                    // port: Peaceful spawn refusal leaves the boss phase pending until spawning is allowed.
                    bossSpawned = boss != null;
                    return boss;
                }
            }
            default -> {}
        }
        return null;
    }

    private void advanceWave(ServerLevel level) {
        wave++;
        kills = 0;
        broadcast(level, "Wave " + (wave == 4 ? "FINAL" : wave) + " is starting!", ChatFormatting.GOLD);
    }

    public @Nullable EntityCombatDropPod spawnCattle(ServerLevel level) {
        return CombatDropPods.spawnCattle(level, wave);
    }

    public @Nullable LivingEntity spawnAttempt(ServerLevel level) {
        if (wave > 3) return null;
        int timer = 200;
        if (wave == 2) timer = 100;
        if (wave == 3) timer = 80;
        if (level.getGameTime() % timer == 0) {
            Spawner spawner = SPAWNER;
            var spawned = spawner == null ? null : spawner.ufo(level, this);
            if (spawned != null) lastSpawns++;
            return spawned;
        }
        return null;
    }

    /** 1.7 {@code onKill}, from the death event of any living entity on the body. */
    public void onKill(LivingEntity entity, String body) {
        KillValue value = KILLS;
        if (value == null) return;
        int worth = value.kills(entity);
        if (worth > 0) {
            kills += worth;
            CelestialTraits.modifyTraits(body, this);
        }
        if (value.endsInvasion(entity, wave)) {
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = CelestialTraits.getTraits(body);
            traits.remove(CBT_Invasion.class);
            if (entity.level() instanceof ServerLevel level) broadcast(level, "The Invasion Is Over!", ChatFormatting.YELLOW);
            CelestialTraits.setTraits(body, traits);
        }
    }

    private static void broadcast(ServerLevel level, String text, ChatFormatting color) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(text).withStyle(color, ChatFormatting.BOLD));
        }
    }

    /** 1.7 boss bar ({@code IBossDisplayData}): max health is the kill requirement, health what is left of it. */
    public float getMaxHealth() {
        return killreq;
    }

    public float getHealth() {
        return killreq - kills;
    }

    /** The server keeps the invasion boss bar's audience restricted to the invaded body. */
    public void updateBar(MinecraftServer server, String body) {
        if (bar == null) {
            bar = Compat.bossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.PINK,
                    BossEvent.BossBarOverlay.PROGRESS);
        }
        bar.setName(Component.translatable("invasion.hbmspace.wave", wave));
        bar.setProgress(killreq > 0 ? Mth.clamp((float) (killreq - kills) / killreq, 0F, 1F) : 1F);
        var watching = new HashSet<ServerPlayer>();
        if (isInvading && wave < 4) {
            for (var player : server.getPlayerList().getPlayers()) {
                if (CelestialTraits.bodyOf(player.level()).equals(body)) watching.add(player);
            }
        }
        for (var player : List.copyOf(bar.getPlayers())) {
            if (!watching.contains(player)) bar.removePlayer(player);
        }
        for (var player : watching) bar.addPlayer(player);
    }

    public @Nullable ServerBossEvent bar() {
        return bar;
    }

    public void removeBar() {
        if (bar != null) bar.removeAllPlayers();
    }

    public void writeToNBT(CompoundTag tag) {
        tag.merge((CompoundTag) codec().encodeStart(NbtOps.INSTANCE, this).result().orElseThrow());
    }

    public void readFromNBT(CompoundTag tag) {
        var loaded = codec().parse(NbtOps.INSTANCE, tag).result().orElseThrow();
        wave = loaded.wave;
        kills = loaded.kills;
        killreq = loaded.killreq;
        waveTime = loaded.waveTime;
        isInvading = loaded.isInvading;
        warningPlayed = loaded.warningPlayed;
        podBurstCounter = loaded.podBurstCounter;
        podCooldown = loaded.podCooldown;
        bossSpawned = loaded.bossSpawned;
    }

    @Override
    public CBT_Invasion copy() {
        CBT_Invasion copy = new CBT_Invasion(wave, waveTime, isInvading);
        copy.kills = kills;
        copy.killreq = killreq;
        copy.lastSpawns = lastSpawns;
        copy.spawndelay = spawndelay;
        copy.podBurstCounter = podBurstCounter;
        copy.podCooldown = podCooldown;
        copy.bossSpawned = bossSpawned;
        copy.warningPlayed = warningPlayed;
        return copy;
    }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(wave);
        buf.writeInt(kills);
        buf.writeInt(killreq);
        buf.writeDouble(waveTime);
        buf.writeBoolean(isInvading);
        buf.writeBoolean(warningPlayed);
        buf.writeInt(podBurstCounter);
        buf.writeInt(podCooldown);
        buf.writeBoolean(bossSpawned);
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        wave = buf.readInt();
        kills = buf.readInt();
        killreq = buf.readInt();
        waveTime = buf.readDouble();
        isInvading = buf.readBoolean();
        warningPlayed = buf.readBoolean();
        podBurstCounter = buf.readInt();
        podCooldown = buf.readInt();
        bossSpawned = buf.readBoolean();
        if (!isInvading && !warningPlayed) CLIENT_WARNING.run();
    }

    private record Saved(int wave, int kills, int killreq, double waveTime, boolean isInvading, boolean warningPlayed,
                         int podBurst, int podCooldown, boolean bossSpawned) {}

    static Codec<CBT_Invasion> codec() {
        return RecordCodecBuilder.<Saved>create(i -> i.group(
                Codec.INT.optionalFieldOf("wave", 0).forGetter(Saved::wave),
                Codec.INT.optionalFieldOf("kills", 0).forGetter(Saved::kills),
                Codec.INT.optionalFieldOf("killreq", 0).forGetter(Saved::killreq),
                Codec.DOUBLE.optionalFieldOf("waveTime", 0D).forGetter(Saved::waveTime),
                Codec.BOOL.optionalFieldOf("isInvading", false).forGetter(Saved::isInvading),
                Codec.BOOL.optionalFieldOf("warningPlayed", false).forGetter(Saved::warningPlayed),
                Codec.INT.optionalFieldOf("podBurst", 0).forGetter(Saved::podBurst),
                Codec.INT.optionalFieldOf("podCooldown", 0).forGetter(Saved::podCooldown),
                Codec.BOOL.optionalFieldOf("bossSpawned", false).forGetter(Saved::bossSpawned)
        ).apply(i, Saved::new)).xmap(s -> {
            CBT_Invasion inv = new CBT_Invasion(s.wave(), s.waveTime(), s.isInvading());
            inv.kills = s.kills();
            inv.killreq = s.killreq();
            inv.warningPlayed = s.warningPlayed();
            inv.podBurstCounter = s.podBurst();
            inv.podCooldown = s.podCooldown();
            inv.bossSpawned = s.bossSpawned();
            return inv;
        }, inv -> new Saved(inv.wave, inv.kills, inv.killreq, inv.waveTime, inv.isInvading, inv.warningPlayed,
                inv.podBurstCounter, inv.podCooldown, inv.bossSpawned));
    }
}
