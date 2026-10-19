package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the progression machines, part D (see {@link MachineItemDeclarations}): the StarDar and its remote
 * access console, from hbm-space's {@code RenderStardar} and {@code RenderDishControl} item renderers. Dejson runs
 * {@link #declare()}.
 */
public final class MachineItemDeclarationsD {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private MachineItemDeclarationsD() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderStardar: inv translate(0, -5, 0) scale(1.5); common scale(0.55), cull off, renderAll (the drive too)
        Dejson.item(NS, "machine_stardar")
                .definition(MachineObjModel.of("ntmspace:models/machines/stardar.obj", "ntmspace:textures/models/machines/stardar.png")
                        .scale(0.55).ty(0.0).yaw(0.0).guiScale(1.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0))
                .oversizedInGui().register();
        // RenderDishControl: inv translate(0, -0.5, 0) scale(6); common translate(0.5, 0, 0), renderAll (the drive too)
        Dejson.item(NS, "dish_control")
                .definition(MachineObjModel.of("ntmspace:models/machines/dish_controller.obj", "ntmspace:textures/models/machines/dish_controller.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(6.0).rawLegacyTransform()
                        .itemTranslate(0.5, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -0.5, 0.0))
                .oversizedInGui().register();
    }
}
