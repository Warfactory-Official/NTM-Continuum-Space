package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the progression machines, part B (see {@link MachineItemDeclarations}), from hbm-space's
 * {@code ItemRenderBase} numbers. Dejson runs {@link #declare()}.
 */
public final class MachineItemDeclarationsB {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private MachineItemDeclarationsB() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderHydroponic: inv translate(0, -1.75, 0) scale(3); common renderAll (no scale)
        machine("hydrobay", "machines/hydroponic.obj", "machines/hydroponic.png", 1.0, 3.0, -1.75);
        // RenderVacuumCircuit: inv translate(0, -1, 0) scale(3.5); common renderAll (no scale)
        machine("machine_vacuum_circuit", "machines/vacuum_solderer.obj", "machines/vacuum_solderer.png", 1.0, 3.5, -1.0);
    }

    /** A machine item drawn whole: {@code renderCommon} scale, {@code renderInventory} translate y and scale. */
    private static void machine(String id, String obj, String texture, double commonScale, double guiScale, double guiY) {
        Dejson.item(NS, id)
                .definition(MachineObjModel.of("ntmspace:models/" + obj, "ntmspace:textures/models/" + texture)
                        .scale(commonScale).ty(0.0).yaw(0.0).guiScale(guiScale).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, guiY, 0.0))
                .oversizedInGui().register();
    }
}
