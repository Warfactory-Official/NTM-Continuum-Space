package com.ntmspace.world;

import com.google.gson.JsonObject;
import dev.golem.core.dejson.Features;

/** The saplings' authored TTree(2,4,5,3,2) and TTree(2,5,7,4,3) configurations. */
public final class TektoTreeData {
    static {
        declare("sapling_vinyl", 4, 5, 3, 2, "vinyl_log", "pet_leaves");
        declare("sapling_pvc", 5, 7, 4, 3, "pvc_log", "rubber_leaves");
        rubber("tekto/rubber_plants", false);
        rubber("tekto/rubber_plants_forest", true);
    }

    private TektoTreeData() {}

    private static void declare(String name, int smallest, int tallest, int xz, int y, String log, String leaves) {
        var shape = new JsonObject();
        shape.addProperty("offset", 2);
        shape.addProperty("smallest", smallest);
        shape.addProperty("tallest", tallest);
        shape.addProperty("xz", xz);
        shape.addProperty("y", y);
        shape.addProperty("vines", false);
        shape.add("log", Features.state("ntmspace:" + log));
        shape.add("leaves", Features.state("ntmspace:" + leaves));
        Features.configured("ntmspace", name, "ntmspace:ttree", shape);
    }

    private static void rubber(String name, boolean forest) {
        var config = new JsonObject();
        config.addProperty("forest", forest);
        Features.configured("ntmspace", name, "ntmspace:rubber_plant", config);
        Features.placed("ntmspace", name, "ntmspace:" + name, Features.count(64), Features.inSquare(),
                Features.uniformHeight(Features.absolute(0), Features.absolute(79)), Features.biome());
    }

    public static void init() {}
}
