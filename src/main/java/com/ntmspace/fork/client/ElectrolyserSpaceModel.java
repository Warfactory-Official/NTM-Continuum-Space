package com.ntmspace.fork.client;

import com.hbm.client.render.MachineObjRenderer;
import dev.golem.core.Ids;
import java.util.Set;

/** Space RenderElectrolyser's hull on the host's existing lazy renderer and placement. */
public final class ElectrolyserSpaceModel {
    private ElectrolyserSpaceModel() {
    }

    public static void init() {
        MachineObjRenderer.overrideModel(Ids.of("hbm", "models/machines/electrolyser.obj"),
                new MachineObjRenderer.ModelDef(Ids.of("ntmspace", "models/machines/electrolyser_space.obj"),
                        Ids.of("ntmspace", "textures/models/machines/electrolyser.png"), 0F, Set.of(), true));
    }
}
