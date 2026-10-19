package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** hbm-space's {@code CBT_Bees}: the amount of bees in a dimension (1 == x100 bees), saved as {@code bees}. */
public class CBT_Bees extends CelestialBodyTrait {

    public float bees;

    public CBT_Bees() {}

    public CBT_Bees(float bees) {
        this.bees = bees;
    }

    @Override public CBT_Bees copy() { return new CBT_Bees(bees); }

    @Override public void writeToBytes(RegistryFriendlyByteBuf buf) { buf.writeFloat(bees); }

    @Override public void readFromBytes(RegistryFriendlyByteBuf buf) { bees = buf.readFloat(); }

    static Codec<CBT_Bees> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("bees", 0F).forGetter(t -> t.bees)
        ).apply(i, CBT_Bees::new));
    }
}
