package com.ntmspace.particle;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * hbm-space ParticleDuoDec/ParticleDuoDec2: world-lit note quads with a slow, jittering fall.
 */
public final class ParticleDuoDec extends CompatParticle {
    private final boolean tune;

    private ParticleDuoDec(ClientLevel level, double x, double y, double z, boolean tune, RandomSource providerRandom) {
        super(level, x, y, z, Particles.sprite(Ids.of(NtmSpace.MOD_ID, tune ? "particlenote2" : "particlenote1")));
        this.tune = tune;
        setLayer(ParticleLayer.TRANSLUCENT);
        var shade = 1F - providerRandom.nextFloat() * 0.2F;
        setColor(shade, shade, shade);
        setQuadSize(0.3F);
        lifetime = 50 + providerRandom.nextInt(50);
        gravity = 0.2F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround) {
            xd += random.nextGaussian() * 0.002D;
            zd += random.nextGaussian() * 0.002D;
            if (yd < -0.025D) yd = -0.025D;
        }
    }

    public boolean isTune() {
        return tune;
    }

    public static final class Provider extends Particles.Provider<SimpleParticleType> {
        private final boolean tune;

        public Provider(boolean tune) {
            this.tune = tune;
        }

        @Override
        protected Particle create(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                  double xd, double yd, double zd, RandomSource random) {
            return new ParticleDuoDec(level, x, y, z, tune, random);
        }
    }
}
