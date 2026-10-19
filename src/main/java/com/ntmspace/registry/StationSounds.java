package com.ntmspace.registry;

import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/**
 * The station thrusters' sounds, hbm-space's {@code sounds.json} entries under their 1.7 names in the {@code ntmspace}
 * namespace: {@code misc.htrstart} (file {@code misc/htr3start}), {@code misc.htrstop}, {@code misc.htrloop},
 * {@code misc.htrfstart} (file {@code misc/htrstart}), {@code misc.lpwstart} ({@code misc/lpw2start}),
 * {@code misc.lpwstop}, {@code misc.lpwloop}.
 */
public final class StationSounds {

    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(SOUNDS);

    public static final RegistryObject<SoundEvent> HTR_START = REG.sound("misc.htrstart");
    public static final RegistryObject<SoundEvent> HTR_STOP = REG.sound("misc.htrstop");
    public static final RegistryObject<SoundEvent> HTR_LOOP = REG.sound("misc.htrloop");
    public static final RegistryObject<SoundEvent> HTRF_START = REG.sound("misc.htrfstart");
    public static final RegistryObject<SoundEvent> LPW_START = REG.sound("misc.lpwstart");
    public static final RegistryObject<SoundEvent> LPW_STOP = REG.sound("misc.lpwstop");
    public static final RegistryObject<SoundEvent> LPW_LOOP = REG.sound("misc.lpwloop");

    private StationSounds() {}

    public static void init() {}
}
