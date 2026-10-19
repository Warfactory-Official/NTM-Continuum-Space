package com.ntmspace.client;

import com.hbm.client.particle.ParticleRotating;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * hbm-space's {@code ParticleDust} (the {@code "duststorm"} effect): a big {@code particle_base} quad in Duna's rust
 * brown (0.4, 0.2, 0.1) at alpha 0.5, half-size {@code scale} (its own {@code renderParticle} draws {@code scale}, not
 * vanilla's {@code 0.1 * scale}), lit by the world, passing through blocks, 50 .. 99 ticks, drifting with its motion and
 * vanilla's 0.98 drag.
 */
public final class ParticleDust extends CompatParticle {

    public ParticleDust(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        super(level, x, y, z, Particles.sprite(ParticleRotating.PARTICLE_BASE));
        setLayer(ParticleLayer.TRANSLUCENT);
        setColor(0.4F, 0.2F, 0.1F);
        setQuadSize(scale);
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.age = 1;
        this.lifetime = 50 + level.getRandom().nextInt(50);
        setAlpha(0.5F);
        this.hasPhysics = false;
        this.gravity = 0F;
    }
}
