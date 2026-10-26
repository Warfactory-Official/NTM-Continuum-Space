package com.ntmspace.atmosphere;

import com.hbm.inventory.fluid.FluidType;
import com.ntmspace.fluid.trait.SpaceFluidTraits;
import com.ntmspace.handler.ReentryFlames;
import net.minecraft.server.level.ServerLevel;
import com.hbm.api.environment.Environment;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CBT_Water;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.CelestialBodies;
import dev.golem.core.hook.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * HBM's environment hooks answered from the bodies' runtime traits and the sealed rooms, where hbm-space consulted
 * {@code CelestialBody.getTrait}, {@code ChunkAtmosphereManager} and {@code CelestialBody.getGravity}:
 *
 * <ul>
 *   <li>{@link Environment#BREATHING}: 1.7 {@code TileEntityMachineBase.breatheAir} - the body's air (not in orbit) with
 *       0.19 atm of earth air or 0.09 of oxygen, else a sealed room with as much, which then consumes the amount.</li>
 *   <li>{@link Environment#PRESSURE}: the pressure of the air at the position, rooms included (1.7
 *       {@code ChunkAtmosphereManager.getAtmosphere}, what the fan read). The condenser, cooling towers and Torex read
 *       the body's air alone in 1.7; through this one hook they now also see a sealed room (a known difference).</li>
 *   <li>{@link Environment#COMPRESSION}: 1.7 {@code TileEntityMachineIntake.canCompress}.</li>
 *   <li>{@link Environment#GROWTH}: 1.7 {@code EntityChemical}'s seed slurry - grass only under air, Duna air, CO2 or
 *       oxygen, spreading 4 x 6, and the CO2 (or Duna air) it eats becomes earth air.</li>
 *   <li>{@link Environment#GROUNDWATER}: the body's {@code CBT_Water} (none without it).</li>
 *   <li>{@link Environment#GAS_RELEASE}, {@link Environment#IMPACT}, {@link Environment#LIGHT_ON}: the traits base HBM's
 *       code wrote in hbm-space ({@code emitGas}, {@code CBT_Impact}, {@code CBT_Lights}).</li>
 *   <li>{@link Environment#GRAVITY}: 1.7 {@code CelestialBody.getGravity} over Earth's on a body (orbit: the orbit port's),
 *       and the orbit port's "has gravity" question: an entity in a sealed room ({@link Breathing#hasGravity}).</li>
 *   <li>{@link Environment#LIVING_TICK}: breathing ({@link Breathing}).</li>
 * </ul>
 */
public final class AtmosphereHooks {

    private AtmosphereHooks() {}

    public static void init() {
        Environment.BREATHING.register((level, pos, amount) -> breatheAir(level, pos, amount) ? Verdict.ALLOW : Verdict.DENY);
        Environment.PRESSURE.register(AtmosphereHandler::pressure);
        Environment.COMPRESSION.register((level, pos, amount) -> canCompress(level, pos, amount) ? Verdict.ALLOW : Verdict.DENY);
        Environment.GROWTH.register(AtmosphereHooks::grow);
        Environment.CHEMICAL_IMPACT.register(AtmosphereHooks::chemicalImpact);
        Environment.GROUNDWATER.register((level, pos) -> {
            CBT_Water water = CelestialTraits.getTrait(level, CBT_Water.class);
            return water == null ? Fluids.NONE : water.fluid;
        });
        Environment.GAS_RELEASE.register((level, pos, type, mB) -> CelestialTraits.release(level, type, mB));
        Environment.IMPACT.register((level, pos) -> CelestialTraits.modifyTraits(level, new CBT_Impact(level.getGameTime())));
        Environment.LIGHT_ON.register((level, pos, state) -> SpaceLights.add(level, state.getLightEmission()));
        Environment.GRAVITY.register(AtmosphereHooks::gravity);
        // the orbit port's station gravity asks whether an entity stands in a sealed atmosphere (1.7 hasGravity)
        com.ntmspace.orbit.OrbitEnvironment.HAS_GRAVITY.register(entity -> Breathing.hasGravity(entity) ? Verdict.ALLOW : Verdict.PASS);
        Environment.SURFACE.register(level -> CelestialBodies.of(level) != null ? Verdict.ALLOW : Verdict.PASS);
        Breathing.init();
        ReentryFlames.init();
    }

    /** 1.7 {@code TileEntityMachineBase.breatheAir}. */
    public static boolean breatheAir(Level level, BlockPos pos, int amount) {
        CBT_Atmosphere atmosphere = Environment.inOrbit(level) ? null : CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere != null && (atmosphere.hasFluid(SpaceFluids.EARTHAIR, 0.19) || atmosphere.hasFluid(Fluids.OXYGEN, 0.09))) {
            return true;
        }
        for (AtmosphereBlob blob : AtmosphereHandler.getBlobs(level, pos)) {
            if (blob.hasFluid(SpaceFluids.EARTHAIR, 0.19) || blob.hasFluid(Fluids.OXYGEN, 0.09)) {
                blob.consume(amount);
                return true;
            }
        }
        return false;
    }

    /** 1.7 {@code TileEntityMachineIntake.canCompress} past its full-tank shortcut. */
    private static boolean canCompress(Level level, BlockPos pos, int consumption) {
        CBT_Atmosphere atmosphere = AtmosphereHandler.getAtmosphere(level, pos);
        if (atmosphere == null || atmosphere.getPressure() <= 0.01D) return false;

        if (!AtmosphereHandler.hasAtmosphere(level, pos)) {
            CelestialTraits.capture(level, atmosphere.getMainFluid(), consumption);
            return true;
        }
        for (AtmosphereBlob blob : AtmosphereHandler.getBlobs(level, pos)) {
            if (!blob.hasFluid(Fluids.AIR) && blob.hasPressure(0.1)) {
                blob.consume(consumption);
                return true;
            }
        }
        return false;
    }

    /** 1.7 {@code EntityChemical} seed slurry on dirt. */
    private static Verdict grow(Level level, BlockPos pos) {
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere != null && (atmosphere.hasFluid(SpaceFluids.EARTHAIR) || atmosphere.hasFluid(SpaceFluids.DUNAAIR)
                || atmosphere.hasFluid(Fluids.CARBONDIOXIDE) || atmosphere.hasFluid(Fluids.OXYGEN))) {
            if (!level.isClientSide()) {
                AtmosphereHandler.addGrowth(level, Blocks.GRASS_BLOCK, pos.getX(), pos.getY(), pos.getZ(), 4, 6);
                int totalConversion = AtmosphereHandler.GRASS_GROWTH_CONVERSION * 4 * 6;
                String body = CelestialTraits.bodyOf(level);
                if (CelestialTraits.consumeGas(body, Fluids.CARBONDIOXIDE, totalConversion)
                        || CelestialTraits.consumeGas(body, SpaceFluids.DUNAAIR, totalConversion)) {
                    CelestialTraits.emitGas(body, SpaceFluids.EARTHAIR, totalConversion);
                }
                level.levelEvent(2005, pos.above(), 0);
            }
            return Verdict.ALLOW;
        }
        return Verdict.DENY;
    }

    /** A terraforming spray converts only its configured soil, then seeds the real neighboring-growth queue. */
    public static void chemicalImpact(ServerLevel level, BlockPos pos,
                                      FluidType type) {
        var trait = type.getTrait(SpaceFluidTraits.FT_Terraformer.class);
        if (trait == null) return;
        var target = trait.getTargetBlock();
        if (!level.getBlockState(pos).is(target)) return;
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        AtmosphereHandler.addGrowth(level, target, Blocks.DIRT, pos.getX(), pos.getY(), pos.getZ(), 4, 6);
    }

    /**
     * 1.7 {@code CelestialBody.getGravity(entity) / STANDARD_GRAVITY} on a body's surface; in orbit the station's gravity
     * is the orbit port's answer ({@code OrbitEnvironment}), so this passes there.
     */
    private static double gravity(Entity entity) {
        if (entity instanceof WaterAnimal) return 1.0D;
        if (Environment.inOrbit(entity.level())) return Double.NaN;
        CelestialBodies.Body body = CelestialBodies.of(entity.level());
        if (body == null) body = CelestialBodies.KERBIN; // 1.7: a level that is no body is Kerbin
        return body.playerGravity() / CelestialBodies.STANDARD_GRAVITY;
    }
}
