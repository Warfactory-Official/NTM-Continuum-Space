package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModels;

/**
 * The OBJ items of the progression machines (part one), as hbm-space's {@code ItemRenderBase}s drew them
 * ({@code renderInventory}: translate and scale in the GUI; {@code renderCommon}: scale and parts), spelled the way
 * HBM's {@code ObjItemDeclarations} spells its machines. Touches no Minecraft class: Dejson runs {@link #declare()} at
 * build time.
 */
public final class MachineItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private MachineItemDeclarations() {
    }

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderCryoDistill: inv translate(0, -3, 0) scale(3.5); common scale(0.5), renderAll
        machine("machine_cryo_distill", "machines/cryo_distill.obj", "machines/cryo_distiller.png", 3.5, -3.0);
        // RenderAlkylation: inv translate(0, -2, 0) scale(5); common scale(0.5), renderAll
        machine("machine_alkylation", "machines/alkylation_unit.obj", "machines/alkylation_unit.png", 5.0, -2.0);
        // RenderMilkReformer: inv translate(0, -3, 0) scale(3.5); common scale(0.5), renderAll
        machine("machine_milk_reformer", "machines/milker.obj", "machines/milker.png", 3.5, -3.0);

        thermal("machine_chungus",
                turbine("hbm:models/machines/chungus.obj", "hbm:textures/models/machines/chungus.png"),
                turbine("ntmspace:models/machines/chungus_destroyed.obj",
                        "ntmspace:textures/models/machines/chungus_destroyed.png"));
        thermal("machine_condenser_powered",
                condenser("hbm:models/machines/condenser.obj", "hbm:textures/models/machines/condenser.png", 0),
                condenser("ntmspace:models/machines/condenser_damaged.obj",
                        "ntmspace:textures/models/machines/condenser_damaged.png", 180).noCull());
    }

    /**
     * A machine item drawn whole: {@code renderCommon} scale 0.5, {@code renderInventory} translate y and scale.
     */
    public static void machine(String id, String obj, String texture, double guiScale, double guiY) {
        Dejson.item(NS, id)
              .definition(MachineObjModel.of("ntmspace:models/" + obj, "ntmspace:textures/models/" + texture)
                                         .scale(0.5).ty(0.0).yaw(0.0).guiScale(guiScale).rawLegacyTransform()
                                         .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                         .guiTranslate(0.0, guiY, 0.0))
              .oversizedInGui().register();
    }

    private static MachineObjModel turbine(String obj, String texture) {
        return MachineObjModel.of(obj, texture).scale(0.5).ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform()
                              .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.5, 0.0, 0.0);
    }

    private static MachineObjModel condenser(String obj, String texture, int yaw) {
        return MachineObjModel.of(obj, texture).scale(0.75).ty(0.0).yaw(yaw).guiScale(2.75).rawLegacyTransform()
                              .itemTranslate(0.375, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                              .guiTranslate(-1.0, -1.0, 0.0);
    }

    private static void thermal(String id, MachineObjModel normal, MachineObjModel damaged) {
        var choice = ItemModels.select("minecraft:block_state").when("true", damaged).fallback(normal);
        Dejson.item("hbm", id).definition(() -> {
            var json = choice.toJson();
            json.addProperty("block_state_property", "damaged");
            return json;
        }).oversizedInGui().register();
    }
}
