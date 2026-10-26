package com.ntmspace.entity.mob;

import com.ntmspace.data.SpaceData;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.hbm.interfaces.IRadiationImmune;
import com.ntmspace.entity.mob.ai.MoonWalkControl;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.data.CompatCow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code EntityMoonCow}: a vanilla cow in a boron-glass helmet that moonwalks ({@link MoonWalkControl}),
 * is muffled by the helmet and drops cheese. Immune to radiation ({@link IRadiationImmune}) and, through the entity
 * tag {@code ntmspace:suffocation_immune}, to the vacuum (1.7 {@code ISuffocationImmune}).
 */
public class EntityMoonCow extends CompatCow implements IRadiationImmune {

    public EntityMoonCow(EntityType<? extends EntityMoonCow> type, Level level) {
        super(type, level);
        this.moveControl = new MoonWalkControl(this);
    }

    @Override
    public @Nullable EntityMoonCow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return dev.golem.core.registry.Entities.create(SpaceMobs.MOON_COW.get(), level, EntitySpawnReason.BREEDING);
    }

    /** 1.7: muffled by the helmet. */
    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    /**
     * 1.7 {@code EntityAnimal.getCanSpawnHere} for the periodic spawner: grass below and a full light value above 8.
     * The Mun's turf is not grass, so in hbm-space Mun cows appeared only while chunks generated (1.7's
     * {@code performWorldGenSpawning}, which never asked this; the biome's spawning chance is 0.008) - kept as found.
     */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (!SpaceData.ENABLE_MUN_COWS.get() && level.getBiome(blockPosition()).is(
                ResourceKey.create(Registries.BIOME,
                        Ids.of("ntmspace", "mun")))) return false;
        if (reason == EntitySpawnReason.NATURAL) {
            BlockPos pos = blockPosition();
            return level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) && level.getRawBrightness(pos, 0) > 8;
        }
        return true;
    }
}
