package com.ntmspace.rocket;

import dev.golem.core.Ids;

import java.util.Set;

import net.minecraft.resources.Identifier;

/**
 * Space 1.7 ResourceManager's HFRWavefrontObject part loads; the remaining parts use face normals.
 */
public final class RocketPartNormals {
    private RocketPartNormals() {
    }

    public static final Set<Identifier> MESHES = Set.of(
            mesh("hbm", "mp_f_10_kerosene"), mesh("hbm", "mp_f_10_long_kerosene"),
            mesh("hbm", "mp_f_10_15_kerosene"), mesh("hbm", "mp_f_15_kerosene"),
            mesh("hbm", "mp_f_15_hydrogen"), mesh("hbm", "mp_f_15_20_kerosene"),
            mesh("ntmspace", "mp_f_20_usa"), mesh("ntmspace", "mp_f_20_6_usa"),
            mesh("ntmspace", "mp_f_20_3_usa"), mesh("ntmspace", "mp_f_20_1_usa"),
            mesh("ntmspace", "mp_f_20_neo"), mesh("ntmspace", "mp_w_fairing"),
            mesh("ntmspace", "rp_landing_capsule"), mesh("ntmspace", "rp_drop_pod"));

    private static Identifier mesh(String namespace, String name) {
        return Ids.of(namespace, "models/missile_parts/" + name + ".obj");
    }
}
