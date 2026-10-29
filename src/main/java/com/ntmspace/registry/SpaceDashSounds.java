package com.ntmspace.registry;

import com.hbm.handler.ArmorDashHandler;
import com.hbm.registration.Reg;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/** Space's authored dash and stamina-refill recordings. */
public final class SpaceDashSounds {
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);
    public static final RegistryObject<SoundEvent> DASH = REG.sound("player.dash");
    public static final RegistryObject<SoundEvent> RECHARGE = REG.sound("player.dash_recharge");

    private SpaceDashSounds() {
    }

    public static void init() {
        ArmorDashHandler.sounds(DASH::get, RECHARGE::get);
    }
}
