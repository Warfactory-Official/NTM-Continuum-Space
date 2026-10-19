package com.ntmspace.entity.rocket;

import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.Registrar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The rocket's entity types, hbm-space's {@code EntityMappings} names: {@code entity_rideable_rocket} and its capsule
 * hit box {@code entity_rideable_rocket_dummy}, both tracked 1000 blocks (63 chunks, as HBM's missiles) every tick.
 */
public final class RocketEntities {

    public static final DeferredRegister.EntityTypes ENTITY_TYPES = DeferredRegister.createEntityTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ENTITY_TYPES);

    /** 1.7 {@code setSize(2, 8)} until the rocket sets its own height. */
    public static final Registrar.EntityHandle<EntityRideableRocket> RIDEABLE_ROCKET = REG.entity("entity_rideable_rocket", key -> Entities.build(
            Entities.noLoot(EntityType.Builder.<EntityRideableRocket>of(EntityRideableRocket::new, MobCategory.MISC))
                    .fireImmune().sized(2.0F, 8.0F).clientTrackingRange(63).updateInterval(1), key));
    /** 1.7 {@code setSize(4, 2.5F)}. */
    public static final Registrar.EntityHandle<EntityRideableRocketDummy> RIDEABLE_ROCKET_DUMMY = REG.entity("entity_rideable_rocket_dummy", key -> Entities.build(
            Entities.noLoot(EntityType.Builder.<EntityRideableRocketDummy>of(EntityRideableRocketDummy::new, MobCategory.MISC))
                    .fireImmune().sized(4.0F, 2.5F).clientTrackingRange(63).updateInterval(1), key));

    private RocketEntities() {}

    public static void init() {}
}
