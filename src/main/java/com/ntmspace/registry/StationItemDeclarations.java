package com.ntmspace.registry;

import com.hbm.items.MachineObjModel;
import dev.golem.core.dejson.Dejson;

/**
 * The OBJ items of the station blocks, as hbm-space's {@code ItemRenderBase}s drew them ({@code renderInventory}:
 * translate and scale in the GUI; {@code renderCommon}: scale and parts), spelled the way HBM's
 * {@code ObjItemDeclarations} spells its machines. Touches no Minecraft class: Dejson runs {@link #declare()} at build
 * time.
 */
public final class StationItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private StationItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderOrbitalStation: inv translate(0, 2, 0) scale(2); common cull off, docking_port.renderAll
        for (String id : new String[] {"orbital_station", "orbital_station_port"}) {
            Dejson.item(NS, id)
                    .definition(MachineObjModel.of("ntmspace:models/machines/docking_port.obj", "ntmspace:textures/models/machines/docking_port.png")
                            .scale(1.0).ty(0.0).yaw(0.0).guiScale(2.0).rawLegacyTransform().noCull()
                            .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 2.0, 0.0))
                    .oversizedInGui().register();
        }
        // RenderOrbitalComputer: inv translate(0, -2.5, 0) scale(6); common cull off, renderAllExcept("Drive")
        Dejson.item(NS, "orbital_station_computer")
                .definition(MachineObjModel.of("ntmspace:models/machines/spaceship_computer.obj", "ntmspace:textures/models/machines/spaceship_computer.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(6.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.5, 0.0).exclude("drive"))
                .oversizedInGui().register();
        // RenderHTR3: inv translate(0, -3, 0) scale(2.5); common scale(0.5), renderAllExcept("ExhaustVacuum"), lpw2 texture
        Dejson.item(NS, "machine_htr3")
                .definition(MachineObjModel.of("ntmspace:models/machines/htr3.obj", "ntmspace:textures/models/machines/lpw2.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0).exclude("ExhaustVacuum"))
                .oversizedInGui().register();
        // RenderHTRF4: inv translate(0, -1, 0) scale(1.5); common scale(0.5), renderAll, lpw2 texture
        Dejson.item(NS, "machine_htrf4")
                .definition(MachineObjModel.of("ntmspace:models/machines/htrf4.obj", "ntmspace:textures/models/machines/lpw2.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(1.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0))
                .oversizedInGui().register();
        // RenderHTRNeo: inv translate(0, -1, 0) scale(1.5); common scale(0.5), renderAllExcept("Exhaust")
        Dejson.item(NS, "machine_htrf4neo")
                .definition(MachineObjModel.of("ntmspace:models/fusion/htrf4.obj", "ntmspace:textures/models/fusion/htrf4.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(1.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0).exclude("Exhaust"))
                .oversizedInGui().register();
        // RenderXenonThruster: inv translate(0, -4, 0) scale(4); common renderPart("Thruster")
        Dejson.item(NS, "machine_xenon_thruster")
                .definition(MachineObjModel.of("ntmspace:models/xenon_thruster.obj", "ntmspace:textures/models/xenon_thruster.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.0, 0.0).parts("Thruster"))
                .oversizedInGui().register();
    }
}
