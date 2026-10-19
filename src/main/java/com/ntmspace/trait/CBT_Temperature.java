package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** hbm-space's {@code CBT_Temperature}: the body's temperature in degrees Celsius (saved as {@code c}). */
public class CBT_Temperature extends CelestialBodyTrait {

    public float degrees; // In SI units (Celsius)

    public CBT_Temperature() {
        this.degrees = 20;
    }

    public CBT_Temperature(float degrees) {
        this.degrees = degrees;
    }

    @Override public CBT_Temperature copy() { return new CBT_Temperature(degrees); }

    @Override public void writeToBytes(RegistryFriendlyByteBuf buf) { buf.writeFloat(degrees); }

    @Override public void readFromBytes(RegistryFriendlyByteBuf buf) { degrees = buf.readFloat(); }

    static Codec<CBT_Temperature> codec() {
        // 1.7 readFromNBT: getFloat("c"), 0 when absent
        return RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("c", 0F).forGetter(t -> t.degrees)
        ).apply(i, CBT_Temperature::new));
    }
}
