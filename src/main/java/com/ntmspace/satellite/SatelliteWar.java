package com.ntmspace.satellite;

import com.hbm.saveddata.satellites.Satellite;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

/**
 * hbm-space's {@code SatelliteWar} ("time to clean up this shit and make it PROPER."): the base of the war satellites,
 * which the war controller clicks ({@link #onClick}), fires ({@link #fire}) and aims at a body ({@link #setTarget(String)}).
 * 1.7 never registered it on its own (only {@link SatelliteRailgun}); its {@code lastOp} is saved, {@code interp} and
 * {@code cooldown} are not ({@code interp} was only synced for the sky renderer).
 */
public abstract class SatelliteWar extends Satellite {

    public long lastOp;
    public float interp;
    public int cooldown;

    /** {@code writeToNBT}/{@code readFromNBT}: {@code lastOp} on top of the base data. */
    protected static <S extends SatelliteWar> MapCodec<S> codec(java.util.function.Supplier<S> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.LONG.optionalFieldOf("lastOp", 0L).forGetter(s -> s.lastOp)
        ).apply(i, lastOp -> {
            S sat = factory.get();
            sat.lastOp = lastOp;
            return sat;
        }));
    }

    public void onClick(ServerLevel level, int x, int z) {}

    public void fire() {}

    /** 1.7 {@code setTarget(CelestialBody)}: the body by name. */
    public void setTarget(String body) {}

    public void fireAtTarget(String body) {}

    public int magSize() {
        return 0;
    }

    @Override
    public String getType() {
        return "WAR";
    }
}
