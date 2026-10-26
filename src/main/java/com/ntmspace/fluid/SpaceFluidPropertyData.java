package com.ntmspace.fluid;

import com.google.gson.JsonParser;
import dev.golem.core.dejson.Data;

/** Authored ammonia properties, including the Duna-soil terraformer carried by the chemical thrower. */
public final class SpaceFluidPropertyData {
    static {
        Data.declare("data/ntmspace/fluid_property/ammonia.json", JsonParser.parseString("""
                {
                  "type": "gaseous",
                  "color": "#00A0F7",
                  "temperature": 20,
                  "hazard": {"poison": 2, "flammability": 0, "reactivity": 1, "symbol": "asphyxiant"},
                  "traits": {
                    "poison": {"withering": true, "level": 4},
                    "ntmspace:terraformer": {"block": "DUNA"}
                  }
                }
                """).getAsJsonObject());
    }
    private SpaceFluidPropertyData() {}
    public static void init() {}
}
