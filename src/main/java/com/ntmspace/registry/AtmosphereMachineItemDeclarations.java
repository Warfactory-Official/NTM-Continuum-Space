package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the atmosphere machines ({@link AtmosphereMachines}), as hbm-space drew them: {@code ItemRenderLibrary}
 * for the tower and the compressor, the renderers' {@code getRenderer} for the emitter and the scrubber. Dejson runs
 * {@link #declare()}.
 */
public final class AtmosphereMachineItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private AtmosphereMachineItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // ItemRenderLibrary machine_atmo_tower: inv translate(0, -5, 0) scale(4); common rotate(180, Y) scale(0.5), renderAll
        item("machine_atmo_tower", MachineObjModel.of("ntmspace:models/machines/atmo_tower.obj",
                "ntmspace:textures/models/machines/machine_atmo_tower.png").scale(0.5).yaw(180.0).guiScale(4.0), -5.0);
        // ItemRenderLibrary machine_atmo_vent: inv translate(0, -4, 0) scale(6); common rotate(180, Y) scale(0.5), renderAll
        item("machine_atmo_vent", MachineObjModel.of("ntmspace:models/machines/atmo_vent.obj",
                "ntmspace:textures/models/machines/atmo_vent.png").scale(0.5).yaw(180.0).guiScale(6.0), -4.0);
        // RenderAtmosphericEmitter: inv scale(4) translate(0, -1, 0) (= -4 before the scale); common renderAll
        item("machine_atmo_emitter", MachineObjModel.of("ntmspace:models/machines/outtake.obj",
                "ntmspace:textures/models/machines/outtake.png").scale(1.0).yaw(0.0).guiScale(4.0), -4.0);
        // RenderAirScrubber: inv translate(0, -4, 0) scale(4); common scale(1.5), culling off, renderAll
        item("air_scrubber", MachineObjModel.of("ntmspace:models/machines/air_scrubber.obj",
                "ntmspace:textures/models/machines/air_scrubber.png").scale(1.5).yaw(0.0).guiScale(4.0).noCull(), -4.0);
    }

    private static void item(String id, MachineObjModel model, double guiY) {
        Dejson.item(NS, id)
                .definition(model.ty(0.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, guiY, 0.0))
                .oversizedInGui().register();
    }
}
