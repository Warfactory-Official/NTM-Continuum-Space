package com.ntmspace.fork;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Charge data on a complex capacitor stack; the decorative block reads none of it. */
public record CapacitorCharge(long power, long maxPower) {
    public static final Codec<CapacitorCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("power").forGetter(CapacitorCharge::power),
            Codec.LONG.fieldOf("max_power").forGetter(CapacitorCharge::maxPower)
    ).apply(instance, CapacitorCharge::new));
}
