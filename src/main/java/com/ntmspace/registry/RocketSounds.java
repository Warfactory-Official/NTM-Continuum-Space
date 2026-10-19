package com.ntmspace.registry;

import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/**
 * The rideable rocket's sounds, hbm-space's {@code entity.rocketIgnition}, {@code entity.rocketFlyLight},
 * {@code entity.rocketFlyHeavy} and {@code entity.rocketStage}, in HBM's snake-case spelling of 1.7's camel-case names
 * (as HBM renamed {@code entity.rocketTakeoff} to {@code entity.rocket_takeoff}); files {@code sounds/entity/rocket*.ogg}.
 */
public final class RocketSounds {

    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(SOUNDS);

    public static final RegistryObject<SoundEvent> ROCKET_IGNITION = REG.sound("entity.rocket_ignition");
    public static final RegistryObject<SoundEvent> ROCKET_FLY_LIGHT = REG.sound("entity.rocket_fly_light");
    public static final RegistryObject<SoundEvent> ROCKET_FLY_HEAVY = REG.sound("entity.rocket_fly_heavy");
    public static final RegistryObject<SoundEvent> ROCKET_STAGE = REG.sound("entity.rocket_stage");

    private RocketSounds() {}

    public static void init() {}
}
