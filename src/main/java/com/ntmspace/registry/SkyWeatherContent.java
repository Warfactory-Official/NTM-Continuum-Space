package com.ntmspace.registry;

import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/**
 * The common half of the bodies' weather and sky objects: each body's rain and thunder applied to its level
 * ({@link com.ntmspace.weather.BodyWeather}), Duna's dust storms ({@link com.ntmspace.weather.DustStorm}), and the
 * satellites and stations the client draws in the sky ({@link com.ntmspace.satellite.SatelliteSky}). The client half is
 * {@link com.ntmspace.client.SkyWeatherClient}.
 *
 * <p>Sound: hbm-space's {@code misc.rumble}, which {@code WorldProviderEve} played (as {@code hbm:misc.rumble}) when one
 * of Eve's lightning flashes began; HBM has no such sound, so it is this mod's.
 */
public final class SkyWeatherContent {

    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(SOUNDS);

    public static final RegistryObject<SoundEvent> MISC_RUMBLE = REG.sound("misc.rumble");

    private SkyWeatherContent() {}

    public static void init() {
        com.ntmspace.weather.BodyWeather.init();
        com.ntmspace.weather.DustStorm.init();
        com.ntmspace.satellite.SatelliteSky.init();
    }
}
