package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the Dyson swarm, as hbm-space's {@code ItemRenderBase}s drew them (the machines' renderers'
 * {@code getRenderer} and {@code ItemRenderLibrary}'s swarm member). Dejson runs {@link #declare()}.
 */
public final class DysonItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private DysonItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderDysonLauncher: inv translate(0, -2, 0) scale(3); common scale(0.25), renderAll
        item("dyson_launcher", machine("dyson_spinlaunch").scale(0.25).yaw(0.0).guiScale(3.0), 0.0, -2.0);
        // RenderDysonReceiver: inv translate(0, -4, 0) scale(1.5); common scale(0.55), renderAll
        item("dyson_receiver", machine("dyson_receiver").scale(0.55).yaw(0.0).guiScale(1.5), 0.0, -4.0);
        // RenderDysonConverterHE: inv translate(0, -1, 0) scale(3); common scale(0.55), renderAll
        item("dyson_converter_he", machine("dyson_he_converter").scale(0.55).yaw(0.0).guiScale(3.0), 0.0, -1.0);
        // RenderDysonConverterTU: inv translate(3, 1, 0) scale(3); common scale(0.55), renderAll
        item("dyson_converter_tu", machine("dyson_tu_converter").scale(0.55).yaw(0.0).guiScale(3.0), 3.0, 1.0);
        // RenderDysonConverterAnatmogenesis: inv translate(0, -2, 0) scale(2.5); common scale(0.55), renderAll
        item("dyson_converter_anatmogenesis", machine("dyson_anatmogenesis").scale(0.55).yaw(0.0).guiScale(2.5), 0.0, -2.0);
        // ItemRenderLibrary swarm_member: inv translate(0, 3, 0) scale(1.8) rotate(-45, Z) rotate(30, X)
        // rotate(currentTimeMillis % 7200 * -0.05, Y); common renderAll
        item("swarm_member", MachineObjModel.of("ntmspace:models/dyson_swarm_satellite.obj",
                        "ntmspace:textures/models/dyson_swarm_satellite.png").scale(1.0).yaw(0.0).guiScale(1.8)
                .guiRotateStep(0.0, 0.0, -45.0).guiRotateStep(30.0, 0.0, 0.0).guiSpinSpeed(-0.05).guiSpinStep(0.0, 1.0, 0.0),
                0.0, 3.0);
    }

    private static MachineObjModel machine(String name) {
        return MachineObjModel.of("ntmspace:models/machines/" + name + ".obj", "ntmspace:textures/models/machines/" + name + ".png");
    }

    private static void item(String id, MachineObjModel model, double guiX, double guiY) {
        Dejson.item(NS, id)
                .definition(model.ty(0.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(guiX, guiY, 0.0))
                .oversizedInGui().register();
    }
}
