package com.ntmspace.world;

import com.hbm.saveddata.TomSaveData;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.item.special.ItemModHeavyBoots;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitEnvironment;
import dev.golem.core.Ids;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.LightLayer;
import com.hbm.api.environment.Environment;
import com.ntmspace.atmosphere.AtmosphereHandler;
import dev.golem.core.hook.Verdict;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Evaporation;
import dev.golem.core.world.Furnaces;
import dev.golem.core.world.Physics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.item.Items;

/**
 * What NTM: Space changes about the environment HBM and vanilla see, through their hooks:
 *
 * <ul>
 *   <li><b>Breathing, air pressure, gas, growth, groundwater, impact, lights, gravity, entity breathing</b>: from the
 *       bodies' runtime traits and the sealed rooms, in {@link com.ntmspace.atmosphere.AtmosphereHooks}.</li>
 *   <li><b>Vanilla furnaces</b> ({@link Furnaces#IGNITION}): hbm-space replaced every placed furnace with its
 *       {@code TileEntityFurnaceSpace}, which lights from any fuel but lava only where a fire can breathe. The same rule
 *       holds for the furnace, smoker and blast furnace through their shared ignition hook, without swapping blocks.</li>
 *   <li><b>Gravity</b>: hbm-space rewrote each living entity's vertical motion every tick to {@code g / 20} per tick
 *       (g = the body's surface gravity scaled so Kerbin is 1.6) and scaled fall distance by {@code g / 1.6}, with no fall
 *       damage below 0.3. The same numbers are the vanilla physics attributes (Golem Core's {@link Physics}): gravity
 *       {@code 0.08 * g / 1.6}, safe fall distance {@code 3 / k} and fall damage multiplier {@code k} for
 *       {@code k = g / 1.6}, which give 1.7's {@code ceil(distance * k - 3)} exactly. As in 1.7: gravity within 1.5 to
 *       1.7 is left alone, a minimum of 0.2 applies except at zero gravity, and water mobs and chickens keep vanilla
 *       surface gravity. High gravity only accelerates descent; crouching heavy boots restore standard pull on a
 *       low-gravity body. In zero gravity, vertical drag follows horizontal drag and players steer with jump/sneak.
 *       The vanilla travel hooks apply the pull once, before modern Slow Falling caps it.</li>
 * </ul>
 */
public final class SpaceEnvironment {

    public static final Identifier BODY = Ids.of("ntmspace", "body_gravity");
    private static final String GRAVITY = BODY.toString();
    private static final String SAFE_FALL = BODY.toString();
    private static final String FALL_DAMAGE = BODY.toString();

    private SpaceEnvironment() {}

    public static void init() {
        // breathing, pressure and the other trait-driven hooks: the bodies' runtime traits and the sealed rooms
        com.ntmspace.atmosphere.AtmosphereHooks.init();
        Evaporation.WATER.register(AtmosphereHandler::waterEvaporation);
        SolarRadiation.init();
        Environment.EMPTY_OILBURNER_COOLING.register(() -> true);
        Environment.EVAPORATION.register((level, pos, vacuumProof) -> {
            if (!(level instanceof ServerLevel server)) return false;
            var impact = TomSaveData.published(server);
            return impact != null && impact.fire > 1e-5 && level.getBrightness(LightLayer.SKY, pos) > 7;
        });
        Environment.DELIVERY_REFUSAL.register(level -> BodyReach.bobmazonRefusal(level.dimension()));
        Furnaces.IGNITION.register((level, pos, fuel) -> {
            if (fuel.is(Items.LAVA_BUCKET)) return Verdict.PASS;
            return Environment.breatheAir(level, pos, 0) ? Verdict.PASS : Verdict.DENY;
        });
        Physics.EFFECTIVE_GRAVITY.register(SpaceEnvironment::effective);
        Physics.VERTICAL_AIR_DRAG.register((entity, vertical, horizontal) -> weightless(entity) ? horizontal : vertical);
        Lifecycle.onEntityLoad(SpaceEnvironment::entered);
    }

    private static void entered(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living)) return;
        if (Orbit.inOrbit(level)) {
            Breathing.setAtmosphere(living,
                    AtmosphereHandler.getAtmosphere(living));
        } else {
            measure(living);
        }
    }

    /** Surface pull on a body, or the current station pull measured from the entity's air in orbit. */
    public static void measure(LivingEntity living) {
        if (Orbit.inOrbit(living.level())) {
            apply(living, OrbitEnvironment.gravity(living));
            return;
        }
        if (living instanceof WaterAnimal || living instanceof Chicken) {
            apply(living, CelestialBodies.STANDARD_GRAVITY);
            return;
        }
        var body = CelestialBodies.of(living.level());
        apply(living, body == null ? CelestialBodies.STANDARD_GRAVITY : body.playerGravity());
    }

    /** High gravity leaves ascent at standard pull; low-gravity heavy boots do the same while crouching. */
    public static double effective(LivingEntity entity, double gravity) {
        var attribute = Physics.gravity(entity);
        if (!Attributes.has(attribute, GRAVITY)) return gravity;
        double ratio = 1D + Attributes.modifierAmount(attribute, GRAVITY);
        if (ratio == 0D) {
            if (!(entity instanceof Player player)) return gravity;
            return (player.isShiftKeyDown() ? 0.01F : 0F) - (Physics.isJumping(player) ? 0.01F : 0F);
        }
        boolean standard = ratio > 1D ? entity.getDeltaMovement().y >= 0D
                : entity instanceof Player player && ItemModHeavyBoots.pulls(player);
        return standard ? gravity / ratio : gravity;
    }

    public static boolean weightless(LivingEntity entity) {
        var attribute = Physics.gravity(entity);
        return Attributes.has(attribute, GRAVITY) && Attributes.modifierAmount(attribute, GRAVITY) == -1D;
    }

    /** The body's transient gravity and fall-distance bands, in the legacy 1.6-standard units. */
    public static void apply(LivingEntity living, float gravity) {
        if (gravity >= 1.5F && gravity <= 1.7F) {
            Attributes.removeTransient(Physics.gravity(living), GRAVITY);
            Attributes.removeTransient(Physics.safeFallDistance(living), SAFE_FALL);
            Attributes.removeTransient(Physics.fallDamageMultiplier(living), FALL_DAMAGE);
            return;
        }
        double k = gravity == 0F ? 0D : (double) Math.max(gravity, 0.2F) / CelestialBodies.STANDARD_GRAVITY;
        Attributes.setTransient(Physics.gravity(living), GRAVITY, k - 1.0D, Attributes.Op.MULTIPLY_TOTAL);
        if (gravity < 0.3F) {
            Attributes.setTransient(Physics.fallDamageMultiplier(living), FALL_DAMAGE, -1.0D, Attributes.Op.MULTIPLY_TOTAL);
            Attributes.removeTransient(Physics.safeFallDistance(living), SAFE_FALL);
        } else if (gravity < 1.5F) {
            double fall = (double) gravity / CelestialBodies.STANDARD_GRAVITY;
            Attributes.setTransient(Physics.safeFallDistance(living), SAFE_FALL, 1.0D / fall - 1.0D, Attributes.Op.MULTIPLY_TOTAL);
            Attributes.setTransient(Physics.fallDamageMultiplier(living), FALL_DAMAGE, fall - 1.0D, Attributes.Op.MULTIPLY_TOTAL);
        } else {
            Attributes.removeTransient(Physics.safeFallDistance(living), SAFE_FALL);
            Attributes.removeTransient(Physics.fallDamageMultiplier(living), FALL_DAMAGE);
        }
    }
}
