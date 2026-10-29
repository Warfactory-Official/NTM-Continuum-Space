package com.ntmspace.registry;

import com.hbm.registration.Reg;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/** The two note effects named by hbm-space's ParticleUtil. */
public final class SpaceParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, NtmSpace.MOD_ID);
    private static final Reg REG = Reg.of(PARTICLES);

    public static final RegistryObject<SimpleParticleType> DUOEWE =
            REG.register(Registries.PARTICLE_TYPE, "duoewe", () -> new SimpleParticleType(false) {
            });
    public static final RegistryObject<SimpleParticleType> DUODEC =
            REG.register(Registries.PARTICLE_TYPE, "duodec", () -> new SimpleParticleType(false) {
            });

    private SpaceParticles() {
    }

    public static void init() {
    }
}
