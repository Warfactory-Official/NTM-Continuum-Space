package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the progression machines (part one), as hbm-space's {@code ItemRenderBase}s drew them
 * ({@code renderInventory}: translate and scale in the GUI; {@code renderCommon}: scale and parts), spelled the way
 * HBM's {@code ObjItemDeclarations} spells its machines. Touches no Minecraft class: Dejson runs {@link #declare()} at
 * build time.
 */
public final class MachineItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private MachineItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderCryoDistill: inv translate(0, -3, 0) scale(3.5); common scale(0.5), renderAll
        machine("machine_cryo_distill", "machines/cryo_distill.obj", "machines/cryo_distiller.png", 3.5, -3.0);
        // RenderAlkylation: inv translate(0, -2, 0) scale(5); common scale(0.5), renderAll
        machine("machine_alkylation", "machines/alkylation_unit.obj", "machines/alkylation_unit.png", 5.0, -2.0);
        // RenderMilkReformer: inv translate(0, -3, 0) scale(3.5); common scale(0.5), renderAll
        machine("machine_milk_reformer", "machines/milker.obj", "machines/milker.png", 3.5, -3.0);
    }

    /** A machine item drawn whole: {@code renderCommon} scale 0.5, {@code renderInventory} translate y and scale. */
    public static void machine(String id, String obj, String texture, double guiScale, double guiY) {
        Dejson.item(NS, id)
                .definition(MachineObjModel.of("ntmspace:models/" + obj, "ntmspace:textures/models/" + texture)
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(guiScale).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, guiY, 0.0))
                .oversizedInGui().register();
    }
}
