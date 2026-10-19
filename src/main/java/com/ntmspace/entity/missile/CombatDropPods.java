package com.ntmspace.entity.missile;

import com.ntmspace.trait.CBT_Invasion;
import dev.golem.core.registry.Entities;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space {@code CBT_Invasion.spawnCattle}: the invasion's drop pod, answered for {@link CBT_Invasion#DROP_POD}. A
 * random player on the body gets a pod 250 blocks up, a Gaussian 15 blocks off in x and z, carrying glyphids chosen by
 * the wave (1: two glyphids; 2: 1-3 of glyphid / brawler / digger; 3+: 1-5 of those or blasters, or one behemoth), in
 * the yellow skin (colour 2). The glyphids are HBM's ({@code hbm:entity_glyphid*}); the payload is a freshly built one,
 * saved, as 1.7 did ({@code new EntityGlyphid(world)}, then {@code writeToNBT} with its {@code EntityList} id).
 */
public final class CombatDropPods {

    private CombatDropPods() {}

    /** Installs the drop-pod part of the invasion. */
    public static void init() {
        CBT_Invasion.DROP_POD = (level, invasion) -> spawnCattle(level, invasion.wave);
    }

    /** 1.7 {@code spawnCattle}; the pod it spawned, or null when the chosen player is too deep (or there is none). */
    public static @Nullable EntityCombatDropPod spawnCattle(ServerLevel level, int wave) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return null;
        RandomSource rand = level.getRandom();
        // 1.7 also returned for a non-EntityPlayerMP; every player of a server level is one
        ServerPlayer player = players.get(rand.nextInt(players.size()));
        // 1.7 getWorldInfo().getTerrainType(): the save's world type, shared by every dimension = the overworld's
        if (player.getY() < 50 && !isFlatWorld(level)) return null;

        EntityCombatDropPod pod = EntityCombatDropPod.create(level);
        double x = player.getX() + (rand.nextGaussian() * 15);
        double z = player.getZ() + (rand.nextGaussian() * 15);
        pod.setPos(x, 250, z);
        pod.setDeltaMovement(0, -1.5, 0); // 1.7 motionY = -1.5 (the pod's own tick overrides it)

        String glyph;
        int amount = 1;
        if (wave == 1) {
            amount = 2;
            glyph = "hbm:entity_glyphid";
        } else if (wave == 2) {
            int roll = rand.nextInt(3);
            amount = Math.max(1, rand.nextInt(4));
            glyph = switch (roll) {
                case 0 -> "hbm:entity_glyphid";
                case 1 -> "hbm:entity_glyphid_brawler";
                default -> "hbm:entity_glyphid_digger";
            };
        } else if (wave >= 3) {
            int roll = rand.nextInt(5);
            amount = Math.max(1, rand.nextInt(6));
            switch (roll) {
                case 0 -> glyph = "hbm:entity_glyphid";
                case 1 -> glyph = "hbm:entity_glyphid_brawler";
                case 2 -> glyph = "hbm:entity_glyphid_digger";
                case 3 -> glyph = "hbm:entity_glyphid_blaster";
                default -> {
                    glyph = "hbm:entity_glyphid_behemoth";
                    amount = 1;
                }
            }
        } else {
            glyph = "hbm:entity_glyphid";
        }

        CompoundTag nbt = payload(level, glyph);
        if (nbt == null) return null;
        pod.setPayload(nbt, amount, 2);
        level.addFreshEntity(pod);
        return pod;
    }

    /** A new, unspawned {@code id} entity's save data with its registry id under {@code "id"} (1.7 payload form). */
    public static @Nullable CompoundTag payload(Level level, String id) {
        EntityType<?> type = Entities.byId(id);
        if (type == null) return null;
        Entity entity = Entities.createDetached(type, level);
        if (entity == null) return null;
        CompoundTag nbt = Entities.saveToTag(entity);
        nbt.putString("id", id);
        return nbt;
    }

    private static boolean isFlatWorld(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getChunkSource().getGenerator() instanceof FlatLevelSource;
    }
}
