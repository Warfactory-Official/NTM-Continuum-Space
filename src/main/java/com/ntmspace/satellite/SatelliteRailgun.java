package com.ntmspace.satellite;

import com.hbm.saveddata.satellites.SatelliteType;
import com.mojang.serialization.MapCodec;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.SolarSystem;
import java.util.Random;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code SatelliteRailgun}, launched by {@code sat_war}: aimed at a body, a click starts a war on it
 * ({@code CBT_War(100, 0)}) or, once at war, fires one small projectile at the body it was told to hit. The firing
 * animation ({@code interp}) and the sky model are client rendering, left to the sky port.
 */
public class SatelliteRailgun extends SatelliteWar {

    public static final MapCodec<SatelliteRailgun> CODEC = codec(SatelliteRailgun::new);

    private boolean canFire = false;
    private boolean hasTarget = false;
    private @Nullable String target;

    @Override
    public SatelliteType type() {
        return SpaceSatellites.RAILGUN;
    }

    @Override
    public void onClick(ServerLevel level, int x, int z) {
        fireAtTarget(target);
        canFire = hasTarget;
    }

    @Override
    public void fire() {
        if (canFire) {
            interp += 0.5f;
            interp = Math.min(100.0f, interp + 0.3f * (100.0f - interp) * 0.15f);

            if (interp >= 100) {
                interp = 0;
                canFire = false;
            }
        }
    }

    /** 1.7 {@code target = CelestialBody.getBody(body.dimensionId)}: any body answers (Kerbin for one without a level). */
    @Override
    public void setTarget(String body) {
        target = Orbit.isLandable(SolarSystem.body(body)) ? body : "kerbin";
        hasTarget = true;
    }

    @Override
    public void fireAtTarget(@Nullable String body) {
        if (!hasTarget || target == null) return;
        CBT_War war = CelestialTraits.getTrait(target, CBT_War.class, false);
        if (war == null) {
            CelestialTraits.modifyTraits(target, new CBT_War(100, 0));
        } else {
            // don't crash servers please (1.7)
            float rand = new Random().nextFloat();
            // TODO (1.7): be able to choose projectile types
            String level = body == null ? "minecraft:overworld"
                    : dev.golem.core.Ids.keyId(Orbit.levelOf(SolarSystem.body(body))).toString();
            CBT_War.Projectile projectile = new CBT_War.Projectile(100, 20, 50, 28 * rand * 5, 55, 20,
                    CBT_War.ProjectileType.SMALL, level);
            projectile.GUIangle = (int) (rand * 360);
            war.launchProjectile(projectile);
            CelestialTraits.modifyTraits(target, war);
        }
    }
}
