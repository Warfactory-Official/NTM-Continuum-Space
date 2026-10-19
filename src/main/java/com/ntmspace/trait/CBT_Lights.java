package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * hbm-space's {@code CBT_Lights}: the summed light value of the bright blocks placed above the horizon (and spotlights
 * repaired) on the body, which its night side shows from space. Saved as {@code lights} and {@code isCiv}.
 */
public class CBT_Lights extends CelestialBodyTrait {

    public int lights;
    public boolean isCivilized;

    public CBT_Lights() {}

    public CBT_Lights(int light) {
        this.lights = light;
    }

    private CBT_Lights(int lights, boolean isCivilized) {
        this.lights = lights;
        this.isCivilized = isCivilized;
    }

    public int getIntensity() {
        if (lights > 10000) return 3;
        if (lights > 2000) return 2;
        if (lights > 400) return 1;
        return 0;
    }

    /** 1.7 {@code addLight(block, x, y, z)}: {@code lights += block.getLightValue()}. */
    public void addLight(int lightValue) {
        lights += lightValue;
    }

    /** 1.7 {@code removeLight}. */
    public void removeLight(int lightValue) {
        lights -= lightValue;
        if (lights < 0) lights = 0;
    }

    @Override public CBT_Lights copy() { return new CBT_Lights(lights, isCivilized); }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(lights);
        buf.writeBoolean(isCivilized);
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        lights = buf.readInt();
        isCivilized = buf.readBoolean();
    }

    static Codec<CBT_Lights> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("lights", 0).forGetter(t -> t.lights),
                Codec.BOOL.optionalFieldOf("isCiv", false).forGetter(t -> t.isCivilized)
        ).apply(i, CBT_Lights::new));
    }
}
