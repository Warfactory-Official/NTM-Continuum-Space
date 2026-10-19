package com.ntmspace.satellite;

import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.mojang.serialization.MapCodec;

/** hbm-space's {@code SatelliteDysonRelay}: a relay a Dyson swarm's energy is beamed through; no behaviour of its own. */
public class SatelliteDysonRelay extends Satellite {

    public static final MapCodec<SatelliteDysonRelay> CODEC = MapCodec.unit(SatelliteDysonRelay::new);

    @Override
    public SatelliteType type() {
        return SpaceSatellites.DYSON_RELAY;
    }

    @Override
    public String getType() {
        return "DYSON_RELAY";
    }
}
