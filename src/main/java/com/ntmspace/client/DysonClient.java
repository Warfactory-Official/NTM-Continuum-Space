package com.ntmspace.client;

import com.hbm.client.particle.ParticleCoolingTower;
import com.hbm.client.particle.ParticleRocketFlame;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.client.render.dyson.DysonRenderers;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * The Dyson swarm's client half: the machines' renderers and their 1.7 client effects - the spinlauncher's exhaust burst
 * at launch ({@code "spinlaunch"}), the receiver's flares over its dish ({@code ParticleUtil.spawnFlare}) and the
 * terraformer's gas plume ({@code "tower"}, with hbm-space's thin-air handling). The swarm in the sky is
 * {@link DysonSky}, drawn by {@link MunSky}.
 */
public final class DysonClient {

    private DysonClient() {}

    public static void init() {
        ClientRegistrations.renderers(DysonRenderers::register);
        TileEntityDysonLauncher.SPINLAUNCH.setHandler(DysonClient::launchBurst);
        TileEntityDysonReceiver.CLIENT_EFFECTS = DysonClient::flare;
        TileEntityDysonConverterAnatmogenesis.CLIENT_EFFECTS = DysonClient::plume;
    }

    /**
     * 1.7's {@code "spinlaunch"} packet as the launcher sends it ({@link TileEntityDysonLauncher#SPINLAUNCH}: count, scale,
     * motion, max age) and as {@code ClientProxy} played it: each puff a rocket flame within one block, its motion scaled
     * by 0.9..1 plus its offset, living max age + rand(5).
     */
    static void launchBurst(net.minecraft.world.level.Level world, double x, double y, double z,
                            net.minecraft.nbt.CompoundTag data) {
        if (!(world instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        int count = dev.golem.core.data.Tags.getIntOr(data, "count", 0);
        float scale = data.contains("scale") ? dev.golem.core.data.Tags.getFloatOr(data, "scale", 1F) : 1F;
        double mX = dev.golem.core.data.Tags.getDoubleOr(data, "moX", 0D);
        double mY = dev.golem.core.data.Tags.getDoubleOr(data, "moY", 0D);
        double mZ = dev.golem.core.data.Tags.getDoubleOr(data, "moZ", 0D);
        int maxAge = dev.golem.core.data.Tags.getIntOr(data, "maxAge", 0);

        for (int i = 0; i < count; i++) {
            double ox = rand.nextDouble() * 2 - 1;
            double oy = rand.nextDouble() * 2 - 1;
            double oz = rand.nextDouble() * 2 - 1;
            double mult = 1.0 - rand.nextDouble() * 0.1;

            ParticleRocketFlame fx = new ParticleRocketFlame(level, x + ox, y + oy, z + oz).setScale(scale);
            fx.setMotion(mX * mult + ox, mY * mult + oy, mZ * mult + oz);
            if (maxAge > 0) fx.setMaxAge(maxAge + rand.nextInt(5));
            Particles.add(fx);
        }
    }

    /** 1.7 {@code ParticleUtil.spawnFlare} over the dish: within 5 of the core, 11 up, rising 0.1..0.2, scale 4..6. */
    static void flare(TileEntityDysonReceiver receiver) {
        if (!(receiver.getLevel() instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        BlockPos p = receiver.getBlockPos();
        Particles.add(new ParticleGlow(level, p.getX() - 5 + rand.nextDouble() * 10, p.getY() + 11, p.getZ() - 5 + rand.nextDouble() * 10,
                0, 0.1 + rand.nextFloat() * 0.1, 0, 4F + rand.nextFloat() * 2));
    }

    /**
     * 1.7's {@code "tower"} effect in the gas's colour (lift 0.5, base 0.8, max 4, life 100..149) from 3.25 above the core
     * with a random motion, played as hbm-space's {@code ClientProxy} did: below 0.08 atm no wind, a random kick of
     * {@code (0.08 - p) * 4} and an eighth of the life; no lift in orbit; fewer and shorter-lived on reduced particles.
     */
    static void plume(TileEntityDysonConverterAnatmogenesis converter) {
        if (!(converter.getLevel() instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        Particles.Quality setting = Particles.quality();
        if (setting == Particles.Quality.MINIMAL) return;
        if (setting == Particles.Quality.DECREASED && !rand.nextBoolean()) return;
        int particleSetting = setting == Particles.Quality.DECREASED ? 1 : 0;

        int life = 100 + rand.nextInt(50);
        double mX = rand.nextDouble() - 0.5, mY = rand.nextDouble() * 2, mZ = rand.nextDouble() - 0.5;
        BlockPos p = converter.getBlockPos();
        double pressure = TileEntityDysonLauncher.pressure(level);

        ParticleCoolingTower fx = new ParticleCoolingTower(level, p.getX() + 0.5, p.getY() + 3.25, p.getZ() + 0.5);
        fx.setLift(0.5F);
        fx.setBaseScale(0.8F);
        fx.setMaxScale(4F);
        if (pressure < 0.08) fx.noWind();
        if (Orbit.inOrbit(level)) fx.setLift(0);
        if (pressure < 0.08) {
            double factor = (0.08 - pressure) * 4;
            mX += (rand.nextDouble() - 0.5) * factor;
            mY += (rand.nextDouble() - 0.5) * factor;
            mZ += (rand.nextDouble() - 0.5) * factor;
            life /= 8;
        }
        fx.setParticleSpeed(mX, mY, mZ);
        fx.setLife(life / (particleSetting + 1));
        int color = converter.fluid.clientMetadata().getColor();
        fx.setColor((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
        Particles.add(fx);
    }

    /**
     * hbm-space's {@code ParticleGlow} ("flare"): the {@code particle/yelflare} sprite, white at alpha 0.8, fullbright,
     * no collision, 50..99 ticks, growing from nothing to its scale at half its life and shrinking back.
     */
    static final class ParticleGlow extends CompatParticle {

        private final float peakScale;

        ParticleGlow(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
            super(level, x, y, z, Particles.sprite(dev.golem.core.Ids.of("ntmspace", "yelflare")));
            setLayer(ParticleLayer.EMISSIVE);
            this.setColor(1.0F, 1.0F, 1.0F);
            this.peakScale = scale;
            this.setQuadSize(0.1F * scale);
            this.xd = mX;
            this.yd = mY;
            this.zd = mZ;
            this.age = 1;
            this.lifetime = 50 + level.getRandom().nextInt(50);
            this.setAlpha(0.8F);
            this.hasPhysics = false;
            this.gravity = 0F;
        }

        @Override
        public void tick() {
            super.tick();

            // Increases in size to maximum then returns to minimum
            float t = (float) this.age / (float) this.lifetime;
            t = Math.abs(0.5F - (t - 0.5F)) * 2;

            setQuadSize(0.1F * peakScale * (float) Math.sin(t * Math.PI / 2));
        }
    }
}
