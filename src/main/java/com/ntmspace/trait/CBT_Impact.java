package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** hbm-space's {@code CBT_Impact}: when Tom hit the body (the level's total time, saved as {@code time}). */
public class CBT_Impact extends CelestialBodyTrait {

    public long time;

    public CBT_Impact() {}

    public CBT_Impact(long time) {
        this.time = time;
    }

    @Override public CBT_Impact copy() { return new CBT_Impact(time); }

    @Override public void writeToBytes(RegistryFriendlyByteBuf buf) { buf.writeLong(time); }

    @Override public void readFromBytes(RegistryFriendlyByteBuf buf) { time = buf.readLong(); }

    static Codec<CBT_Impact> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("time", 0L).forGetter(t -> t.time)
        ).apply(i, CBT_Impact::new));
    }
}
