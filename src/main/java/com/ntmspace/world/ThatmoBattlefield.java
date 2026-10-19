package com.ntmspace.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * The client half of hbm-space's {@code WorldProviderThatmo.updateWeather} while Thatmo has its battlefield trait
 * ({@code CBT_BATTLEFIELD}): the sky's state machine and its meteors, one tick per client tick. Side-neutral and
 * deterministic for a given {@link Random}, so GameTests can step it; {@code com.ntmspace.client.ThatmoSky} owns the
 * client's one instance (1.7 kept these as statics of the provider) and draws it.
 *
 * <ul>
 *   <li>{@code chargetime} counts up to 1001; then the railgun fires: {@code flashd} eases to 100 (the flash fades with
 *       it), {@code misc.fireflash} sounds while it is at most 5, and at 100 the charge restarts. {@code misc.impact}
 *       sounds at charge 285.</li>
 *   <li>Between charge 300 and 430 the shield is hit: the beam {@code scale} grows by 1.5 a tick to 20, then the shield
 *       ({@code shield}, {@code nmass}, {@code shielde}, {@code csyw}) eases out; after 120 ticks of shield it resets.
 *       Outside that window everything resets every tick.</li>
 *   <li>{@code altitude} climbs 5 a tick to 400 and wraps, picking a new {@code randPos} (the falling fire's spread).</li>
 *   <li>A meteor every tick ({@code rand.nextInt(1) == 0} is always true in 1.7), up to 8,000 blocks off the player at
 *       y 2017, falling at (-31.2, -20.8, 20); each leaves a smoke puff from where it was, every tick; meteors die at
 *       y 500, smoke after 60 ticks. 1.7's {@code fragments} list is never filled.</li>
 * </ul>
 *
 * All arithmetic is 1.7's, float and double where it was (a {@code float += 1.5} is a double add then a cast).
 */
public final class ThatmoBattlefield {

    /** {@link #tick} result bit: play {@code misc.fireflash} (volume 10, pitch 1) at the player. */
    public static final int SOUND_FIREFLASH = 1;
    /** {@link #tick} result bit: play {@code misc.impact} (volume 10, pitch 1) at the player. */
    public static final int SOUND_IMPACT = 2;

    public int chargetime;
    public float flashd;
    public float altitude;
    public float randPos;
    public float scale;
    public float shield;
    public float cooldown;
    public float nmass;
    public float shielde;
    public float csyw;
    public final List<Meteor> meteors = new ArrayList<>();
    public final List<Meteor> fragments = new ArrayList<>();
    public final List<Meteor> smoke = new ArrayList<>();

    /** hbm-space {@code WorldProviderThatmo.MeteorType}. */
    public enum MeteorType { STANDARD, FRAGMENT, SMOKE }

    /** hbm-space {@code WorldProviderThatmo.Meteor} (its own class, not {@code WorldProviderCelestial}'s). */
    public final class Meteor {
        public double posX, posY, posZ;
        public double prevPosX, prevPosY, prevPosZ;
        public double motionX, motionY, motionZ;
        public boolean isDead = false;
        public long age;
        public final MeteorType type;

        public Meteor(double posX, double posY, double posZ) {
            this(posX, posY, posZ, MeteorType.STANDARD, -31.2, -20.8, 20);
        }

        public Meteor(double posX, double posY, double posZ, MeteorType type, double motionX, double motionY, double motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.type = type;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
        }

        /** 1.7 {@code update}: the smoke puff is left before the meteor moves. */
        private void update(Random rand) {
            if (this.type != MeteorType.SMOKE && this.type != MeteorType.FRAGMENT) {
                smoke.add(new Meteor((this.posX + rand.nextInt(16)) - 8, (this.posY + rand.nextInt(16)), (this.posZ + rand.nextInt(16)) - 8,
                        MeteorType.SMOKE, 0, 0, 0));
            }
            if (this.posY <= 500 && this.type != MeteorType.SMOKE) this.isDead = true;
            if (this.type == MeteorType.SMOKE) {
                this.age++;
                if (this.age >= 60) this.isDead = true;
            }
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
        }
    }

    /**
     * One client tick of a battlefield: {@code rand} stands for 1.7's fresh {@code new Random()} per meteor and per tick,
     * {@code worldRand} for {@code worldObj.rand.nextFloat()} (the new {@code randPos}); the player's position places the
     * new meteor. Returns the {@code SOUND_*} bits to play.
     */
    public int tick(Random rand, DoubleSupplier worldRand, double playerX, double playerZ) {
        int sounds = 0;
        if (chargetime <= 0 || chargetime <= 1000) {
            chargetime += 1;
            flashd = 0;
        } else if (chargetime >= 100) {
            flashd += 0.3f;
            flashd = Math.min(100.0f, flashd + 0.3f * (100.0f - flashd) * 0.15f);
            if (flashd <= 5) sounds |= SOUND_FIREFLASH;
            if (flashd >= 100) chargetime = 0;
        }

        if (chargetime == 285) sounds |= SOUND_IMPACT;

        if (chargetime >= 300 && chargetime <= 430) {
            if (scale <= 0 || scale <= 20) scale += 1.5;
            if (scale >= 20) {
                scale = 20;
                shield += 0.4f;
                shield = Math.min(25.0f, shield + 0.5f * (25.0f - shield) * 0.15f);
                nmass = Math.min(180.0f, nmass + 0.2f * (180.0f - nmass) * 0.15f);
                shielde = Math.min(15.0f, shielde + 0.6f * (15.0f - shielde) * 0.10f);
                csyw += 0.2f;
                csyw = Math.min(100.0f, csyw + 0.2f * (100.0f - csyw) * 0.15f);
            }
            if (shield > 0) {
                if (cooldown <= 0 || cooldown <= 60) cooldown += 0.5;
                if (cooldown >= 60) reset();
            }
        } else {
            reset();
        }

        if (altitude <= 400) altitude += 5;
        if (altitude >= 400) {
            altitude = 0;
            randPos = (float) worldRand.getAsDouble();
        }

        for (Meteor meteor : meteors) meteor.update(rand);
        for (Meteor fragment : fragments) fragment.update(rand);
        // the smoke left this tick is in the list already and ages this tick too (1.7)
        for (int i = 0; i < smoke.size(); i++) smoke.get(i).update(rand);

        meteors.add(new Meteor((playerX + rand.nextInt(16000)) - 8000, 2017, (playerZ + rand.nextInt(16000)) - 8000));

        meteors.removeIf(x -> x.isDead);
        fragments.removeIf(x -> x.isDead);
        smoke.removeIf(x -> x.isDead);
        return sounds;
    }

    private void reset() {
        cooldown = shield = scale = nmass = shielde = csyw = 0;
    }

    /** {@code getSkyColor}/{@code getFogColor}/{@code getSunBrightness}'s flash term: {@code 1 - flashd / 100}, 0 at rest. */
    public float flashAlpha() {
        return (flashd <= 0) ? 0.0F : 1.0F - Math.min(1.0F, flashd / 100);
    }

    /** Their shield term: {@code 0.5 - csyw / 100}, 0 at rest. */
    public float shieldAlpha() {
        return (csyw <= 0) ? 0.0F : 0.5F - Math.min(0.5F, csyw / 100);
    }
}
