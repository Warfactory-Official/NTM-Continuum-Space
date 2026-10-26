package com.ntmspace.breathing;

import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Space's atmospheric damage keys; their flags are declared with the atmosphere content. */
public final class SpaceDamageTypes {
    public static final ResourceKey<DamageType> EVE = ResourceKey.create(Registries.DAMAGE_TYPE, Ids.of(NtmSpace.MOD_ID, "eve"));
    public static final ResourceKey<DamageType> OXYPRIME = Breathing.OXYPRIME;

    private SpaceDamageTypes() {}
}
