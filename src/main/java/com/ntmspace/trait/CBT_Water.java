package com.ntmspace.trait;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * hbm-space's {@code CBT_Water}: a water table (or any sort of fluid cycle, for exotic planets) - what ground pumps lift
 * there ({@code Environment.GROUNDWATER}). Saved as {@code fluid} by registry name; NONE reads as water, as in 1.7.
 */
public class CBT_Water extends CelestialBodyTrait {

    // Doesn't necessarily have to be water
    public FluidType fluid;

    public CBT_Water() {
        fluid = Fluids.WATER;
    }

    public CBT_Water(FluidType fluid) {
        this.fluid = fluid;
    }

    @Override public CBT_Water copy() { return new CBT_Water(fluid); }

    @Override public void writeToBytes(RegistryFriendlyByteBuf buf) { buf.writeInt(Fluids.networkId(fluid)); }

    @Override public void readFromBytes(RegistryFriendlyByteBuf buf) { fluid = Fluids.byNetworkId(buf.readInt()); }

    static Codec<CBT_Water> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Fluids.CODEC.optionalFieldOf("fluid", Fluids.WATER).forGetter(w -> w.fluid)
        ).apply(i, fluid -> new CBT_Water(fluid == Fluids.NONE ? Fluids.WATER : fluid)));
    }
}
