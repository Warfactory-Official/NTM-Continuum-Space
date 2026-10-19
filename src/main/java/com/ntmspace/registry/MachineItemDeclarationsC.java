package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the progression machines, part C (see {@link MachineItemDeclarations}), as hbm-space's
 * {@code ItemRenderBase}s drew them. Dejson runs {@link #declare()}.
 */
public final class MachineItemDeclarationsC {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private MachineItemDeclarationsC() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderMagma: inv translate(0, -2, 0) scale(3); common rotate(90, Y) scale(0.5), renderAll
        item("machine_magma", MachineObjModel.of("ntmspace:models/machines/magma_drill.obj",
                "ntmspace:textures/models/machines/magma_drill.png").scale(0.5).yaw(90.0).guiScale(3.0), -2.0);
        // RenderSolarPanel: inv scale(3.75); common scale(1), culling off, renderAll
        item("machine_solar", MachineObjModel.of("ntmspace:models/machines/solar_panel.obj",
                "ntmspace:textures/models/machines/solar_panel.png").scale(1.0).yaw(0.0).guiScale(3.75).noCull(), 0.0);
        // RenderRadiator: inv translate(0, -5, 0) scale(1.2); common scale(0.75), renderAll
        item("machine_radiator", MachineObjModel.of("ntmspace:models/machines/radiator.obj",
                "ntmspace:textures/models/machines/radiator.png").scale(0.75).yaw(0.0).guiScale(1.2), -5.0);
        // ItemRenderLibrary gas_dock: inv scale(3); common rotate(90 about -Y), satDock with the SAT DOCK's texture
        // (1.7 bound satdock_tex for the gas dock's item, not gasdock_tex; kept)
        item("gas_dock", MachineObjModel.of("hbm:models/sat_dock.obj", "hbm:textures/models/sat_dock.png")
                .scale(1.0).yaw(-90.0).guiScale(3.0), 0.0);
    }

    private static void item(String id, MachineObjModel model, double guiY) {
        Dejson.item(NS, id)
                .definition(model.ty(0.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, guiY, 0.0))
                .oversizedInGui().register();
    }
}
