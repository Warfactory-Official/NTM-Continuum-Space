package com.ntmspace.war;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUFO;
import com.ntmspace.entity.mob.siege.EntitySiegeCraft;
import com.ntmspace.entity.mob.siege.EntitySiegeUFO;
import com.ntmspace.entity.mob.siege.SiegeTier;
import com.ntmspace.registry.SpaceMobs;
import com.ntmspace.registry.WarContent;
import com.ntmspace.trait.CBT_Invasion;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.registry.Entities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.jspecify.annotations.Nullable;

/**
 * What hbm-space's {@code CBT_Invasion} spawns and what a kill is worth ({@link CBT_Invasion#SPAWNER},
 * {@link CBT_Invasion#KILLS}); the drop pod is the rocket port's ({@link CBT_Invasion#DROP_POD},
 * {@code CombatDropPods}).
 *
 * <ul>
 *   <li>{@link #ufo} = 1.7 {@code spawnAttempt}'s spawn: a random player of the body; none if that player is below y 50
 *       (unless the world is superflat); 30 blocks (Gaussian) off in x and z, 30..49 above the player. In wave 2 one in
 *       ten is a siege craft (wave 3: one in five), STONE below {@code 0.5 - waveFactor} else IRON; the rest are siege
 *       UFOs whose tier is rolled against the wave ({@code waveFactor = min(0.2, wave * 0.05)}). Neither gets a spawn-egg
 *       roll: the tier is the one chosen here.</li>
 *   <li>{@link #boss} = 1.7 {@code spawnBoss}: HBM's UFO ({@code hbm:entity_ufo}) 50 blocks over a random player, scan
 *       cooldown 100, with its spawn-egg initialisation.</li>
 *   <li>Kills (1.7 {@code onKill}): a siege UFO is 1 kill, 2 from GOLD up (max health 25 or more); a siege craft 10. In
 *       wave 4 the death of HBM's UFO ends the invasion.</li>
 * </ul>
 */
public final class Invasions {

    private Invasions() {}

    /** Installs the UFO, boss and kill parts of the invasion. */
    public static void init() {
        CBT_Invasion.SPAWNER = new CBT_Invasion.Spawner() {
            @Override
            public void dropPod(ServerLevel level, CBT_Invasion invasion) {
                // the drop pod is CBT_Invasion.DROP_POD's (CombatDropPods); this is only reached without it
            }

            @Override
            public @Nullable LivingEntity ufo(ServerLevel level, CBT_Invasion invasion) {
                return Invasions.ufo(level, invasion.wave);
            }

            @Override
            public @Nullable EntityUFO boss(ServerLevel level, CBT_Invasion invasion) {
                return Invasions.boss(level);
            }
        };
        CBT_Invasion.KILLS = new CBT_Invasion.KillValue() {
            @Override
            public int kills(LivingEntity entity) {
                return Invasions.kills(entity);
            }

            @Override
            public boolean endsInvasion(LivingEntity entity, int wave) {
                return wave >= 4 && entity instanceof EntityUFO;
            }
        };
    }

    /** 1.7 {@code onKill}: what {@code entity}'s death counts for the wave. */
    public static int kills(LivingEntity entity) {
        if (entity instanceof EntitySiegeUFO) {
            // GOLD and above are worth 2 kills (1.7)
            return entity.getMaxHealth() >= 100 * 0.25 ? 2 : 1;
        }
        if (entity instanceof EntitySiegeCraft) return 10;
        return 0;
    }

    /** 1.7 {@code spawnAttempt}'s spawn; the saucer it spawned, or null (no player, or the player is too deep). */
    public static @Nullable LivingEntity ufo(ServerLevel level, int wave) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return null;
        RandomSource rand = level.getRandom();
        ServerPlayer player = players.get(rand.nextInt(players.size()));

        if (player.getY() < 50 && !isFlatWorld(level)) return null;

        double spawnX = player.getX() + rand.nextGaussian() * 30;
        double spawnZ = player.getZ() + rand.nextGaussian() * 30;
        double spawnY = player.getY() + 30 + rand.nextInt(20);

        float bigUfoChance = 0.0F;
        if (wave == 2) bigUfoChance = 0.1F;
        else if (wave == 3) bigUfoChance = 0.2F;

        float waveFactor = wave * 0.05F;
        if (waveFactor > 0.2F) waveFactor = 0.2F;

        if (rand.nextFloat() < bigUfoChance) {
            EntitySiegeCraft bigUfo = Entities.create(WarContent.SIEGE_CRAFT.get(), level, EntitySpawnReason.EVENT);
            bigUfo.snapTo(spawnX, spawnY, spawnZ, rand.nextFloat() * 360.0F, 0.0F);

            float bigRoll = rand.nextFloat();
            SiegeTier chosen = (bigRoll < 0.5F - waveFactor) ? SiegeTier.STONE : SiegeTier.IRON;

            bigUfo.setTier(chosen);
            return level.addFreshEntity(bigUfo) ? bigUfo : null;
        }

        EntitySiegeUFO smallUfo = Entities.create(SpaceMobs.SIEGE_UFO.get(), level, EntitySpawnReason.EVENT);
        smallUfo.snapTo(spawnX, spawnY, spawnZ, rand.nextFloat() * 360.0F, 0.0F);
        smallUfo.setTier(smallTier(rand.nextFloat(), waveFactor));
        return level.addFreshEntity(smallUfo) ? smallUfo : null;
    }

    /** 1.7's small-saucer tier table for one roll in [0, 1). */
    public static SiegeTier smallTier(float smallRoll, float waveFactor) {
        if (smallRoll < 0.15F - (waveFactor * 1.5F)) return SiegeTier.CLAY;
        if (smallRoll < 0.35F - (waveFactor * 1.2F)) return SiegeTier.STONE;
        if (smallRoll < 0.50F - waveFactor) return SiegeTier.IRON;
        if (smallRoll < 0.65F - (waveFactor * 0.8F)) return SiegeTier.SILVER;
        if (smallRoll < 0.80F - (waveFactor * 0.5F)) return SiegeTier.GOLD;
        if (smallRoll < 0.90F - (waveFactor * 0.2F)) return SiegeTier.DESH;
        if (smallRoll < 0.97F) return SiegeTier.SCHRAB;
        return SiegeTier.DNT;
    }

    /** 1.7 {@code spawnBoss}: HBM's UFO 50 blocks over a random player of the body. */
    public static @Nullable EntityUFO boss(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return null;
        RandomSource rand = level.getRandom();
        ServerPlayer player = players.get(rand.nextInt(players.size()));
        // Keep the player-selection draw when the hostile boss must wait for Peaceful to end.
        if (level.getDifficulty() == Difficulty.PEACEFUL) return null;
        EntityUFO entity = Entities.create(ModNtmEntities.UFO.get(), level, EntitySpawnReason.EVENT);
        if (entity == null) return null;
        entity.scanCooldown = 100;
        entity.snapTo(player.getX(), player.getY() + 50, player.getZ(), Mth.wrapDegrees(rand.nextFloat() * 360.0F), 0.0F);
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(entity.getYRot());
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(entity.position())), EntitySpawnReason.EVENT, null);
        return level.addFreshEntity(entity) ? entity : null;
    }

    /**
     * 1.7 {@code getWorldInfo().getTerrainType() != FLAT}: the save's world type, shared by every dimension = the
     * overworld's generator (as {@code CombatDropPods}).
     */
    private static boolean isFlatWorld(ServerLevel level) {
        return level.getServer().overworld().getChunkSource().getGenerator() instanceof FlatLevelSource;
    }

    /**
     * 1.7 {@code EntityBFAngel.onDeathUpdate}: the angel's death starts an invasion of its body
     * ({@code CBT_Invasion(0, 122, false)}: a 122-tick countdown, then wave 1) unless one is already there.
     */
    public static void angelDied(ServerLevel level) {
        CBT_Invasion invasion = CelestialTraits.getTrait(level, CBT_Invasion.class);
        if (invasion == null) {
            CelestialTraits.modifyTraits(level, new CBT_Invasion(0, 122, false));
        }
    }

    /** Removes the invasion of {@code level}'s body, if any (tests and {@code /ntmwar invasion stop}). */
    public static void stop(ServerLevel level) {
        CelestialTraits.attach(level);
        String body = CelestialTraits.bodyOf(level);
        if (!CelestialTraits.hasTrait(body, CBT_Invasion.class, false)) return;
        java.util.Map<Class<? extends com.ntmspace.trait.CelestialBodyTrait>, com.ntmspace.trait.CelestialBodyTrait> traits =
                CelestialTraits.getTraits(body);
        traits.remove(CBT_Invasion.class);
        CelestialTraits.setTraits(body, traits);
    }
}
