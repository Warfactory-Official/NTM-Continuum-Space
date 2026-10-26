package com.ntmspace.client.render.mob;

import com.hbm.client.render.projectile.TracerRegistry;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.ntmspace.entity.mob.MobWeaponsSpace;

/** Mob-specific copies use the same tracer geometry and materials as the original ammunition. */
final class MobWeaponRenderers {
    private MobWeaponRenderers() {}

    static void init() {
        TracerRegistry.init();
        TracerRegistry.setBulletRenderer(MobWeaponsSpace.R762_AP_STILL.id,
                TracerRegistry.bullet(XFactory762mm.r762_ap.id));
        TracerRegistry.setBeamRenderer(MobWeaponsSpace.EMERALD_OVERCHARGE_STILL.id,
                TracerRegistry.beam(XFactoryEnergy.energy_emerald_overcharge.id));
        TracerRegistry.setBeamRenderer(MobWeaponsSpace.TESLA_OVERCHARGE_STILL.id,
                TracerRegistry.beam(XFactoryEnergy.energy_tesla_overcharge.id));
    }
}
