package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * hbm-space's {@code CBT_Destroyed}: the body was blown up; {@code destProgress} animates on the client only. As in 1.7
 * the progress is saved ({@code destProgress}) but never synced (its byte methods are empty there too).
 */
public class CBT_Destroyed extends CelestialBodyTrait {

    public float destProgress;

    public CBT_Destroyed() {}

    public CBT_Destroyed(float destProgress) {
        this.destProgress = destProgress;
    }

    @Override public CBT_Destroyed copy() { return new CBT_Destroyed(destProgress); }

    @Override
    public void update(boolean remote, String body) {
        if (remote) {
            destProgress = Math.min(201.0f, destProgress + 0.0025f * (201.0f - destProgress) * 0.15f);
            if (destProgress >= 200) destProgress = 0;
        }
    }

    static Codec<CBT_Destroyed> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("destProgress", 0F).forGetter(t -> t.destProgress)
        ).apply(i, CBT_Destroyed::new));
    }
}
