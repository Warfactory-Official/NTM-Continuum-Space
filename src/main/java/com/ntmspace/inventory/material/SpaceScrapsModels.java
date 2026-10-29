package com.ntmspace.inventory.material;

import com.google.gson.JsonObject;
import com.hbm.items.ScrapsModels;
import dev.golem.core.dejson.Data;
import dev.golem.core.dejson.Dejson;
import java.util.List;
/** Extends the host selector with the palettes of Space's actual material registrations. */
public final class SpaceScrapsModels {
    private SpaceScrapsModels() {}

    public static void declare() {
        SpaceOreMaterials.register();
        SpaceAlloyMaterials.register();
        SpaceMaterials.register();
        Dejson.generatedSprites("hbm:items/plate_welded", "_", List.of(SpaceAlloyMaterials.IRIDIUM.name));
        var json = new JsonObject();
        json.add("model", ScrapsModels.definition("ntmspace").toJson());
        Data.declareAsset("assets/hbm/models/item/scraps.json",
                ScrapsModels.FALLBACK_MODEL.models(ScrapsModels.FALLBACK_MODEL_ID)
                        .get(ScrapsModels.FALLBACK_MODEL_ID));
        Data.declareAsset("assets/hbm/items/scraps.json", json);
    }
}
