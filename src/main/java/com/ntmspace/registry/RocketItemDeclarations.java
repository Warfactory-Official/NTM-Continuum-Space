package com.ntmspace.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.items.MachineObjModel;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.ntmspace.rocket.RocketPart;
import com.ntmspace.rocket.RocketParts;
import dev.golem.core.dejson.Dejson;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * The rocket family's OBJ items, spelled as HBM's {@code CustomMissileDeclarations} / {@code ObjItemDeclarations}
 * spell theirs. Touches no Minecraft registry: Dejson runs {@link #declare()} at build time.
 *
 * <ul>
 *   <li>The 18 rocket parts: hbm-space's {@code ItemRenderMissilePart} in the inventory, the chain
 *       {@code T(h/2*s, 0, 0) Rz(135) Rx(145) [T(0, h/8*s) warhead | T(0, (h/4 - 0.5)*s) fuselage] T(3.5, 14, 0) S(-s)}
 *       turning at {@code -t/25} degrees, {@code s = 10 / guiHeight} (hbm-space lowered the fuselage by half a unit), and
 *       {@code [T(0.5, 0, 0)] S(0.4)} in hand and on the ground. The same algebra as HBM's parts.</li>
 *   <li>The VAB ({@code RenderRocketAssembly}): inventory {@code T(0, -1, 0) S(2)}, common {@code S(0.55)}, cull off,
 *       the {@code Base} group.</li>
 *   <li>The launch pad ({@code RenderLaunchPadRocket}): inventory {@code T(0, -2, 0) S(1.25)}, common {@code S(0.55)},
 *       cull off, {@code Base} in the pad skin and the tower groups in the support skin.</li>
 * </ul>
 */
public final class RocketItemDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private RocketItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        for (RocketPart part : RocketParts.all()) {
            if (!part.id().startsWith(NS + ":")) continue;
            String name = part.id().substring(NS.length() + 1);
            Dejson.item(NS, name).shares("hbm:item/machine_obj_base").register();
            Dejson.itemDefinition(NS + ":item/" + name).add("model", partModel(part));
            Dejson.itemDefinition(NS + ":item/" + name).addProperty("oversized_in_gui", true);
        }

        Dejson.item(NS, "machine_rocket_assembly")
                .definition(MachineObjModel.of("ntmspace:models/machines/rocket_assembly.obj", "ntmspace:textures/models/machines/rocket_assembly.png")
                        .scale(0.55).ty(0.0).yaw(0.0).guiScale(2.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0).parts("Base"))
                .oversizedInGui().register();

        Dejson.item(NS, "launch_pad_rocket")
                .definition(MachineObjModel.of("ntmspace:models/machines/rocket_pad.obj", "ntmspace:textures/models/machines/rocket_pad.png")
                        .scale(0.55).ty(0.0).yaw(0.0).guiScale(1.25).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("ntmspace:textures/models/machines/rocket_pad_support.png")
                                .parts("Tower_Base", "Tower_Segment", "Tower_Cap", "Tower_Segment_Small")))
                .oversizedInGui().register();
    }

    /** One part's {@code hbm:machine_obj} model on the missile frame (HBM {@code CustomMissileDeclarations.partModel}). */
    static JsonObject partModel(RocketPart part) {
        float height = part.guiHeight() == 0 ? 4F : (float) part.guiHeight();
        float s = 10F / height;

        Matrix3f rot = new Matrix3f().rotateZ((float) Math.toRadians(135)).rotateX((float) Math.toRadians(145));
        Vector3f euler = rot.getEulerAnglesXYZ(new Vector3f());
        // T(a,0,0) R = R T(R^-1 (a,0,0)); everything after R is already in R's frame
        Vector3f lead = new Matrix3f(rot).transpose().transform(new Vector3f(height / 2F * s, 0F, 0F));
        float lift = part.type() == PartType.WARHEAD ? height / 8F * s
                : part.type() == PartType.FUSELAGE ? (height / 4F - 0.5F) * s : 0F;
        Vector3f translation = lead.add(3.5F, 14F + lift, 0F);

        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", part.mesh().toString());
        model.addProperty("texture", part.skin().toString());
        model.addProperty("frame", "missile");
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 0.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("itemTranslate", numbers(0, 0, 0));
        model.add("itemPreTranslate", numbers(0, 0, 0));
        model.addProperty("ty", 0.0);
        // glRotatef(System.currentTimeMillis() / 25 % 360, 0, -1, 0)
        model.addProperty("guiSpinSpeed", -1D / 25D);

        JsonObject transforms = new JsonObject();
        transforms.add("gui", transform((float) Math.toDegrees(euler.x), (float) Math.toDegrees(euler.y),
                (float) Math.toDegrees(euler.z), translation.x, translation.y, translation.z, -s));
        transforms.add("first_person", transform(0, 0, 0, 0, -0.25F / 0.1F, 0, 0.4F / 0.1F));
        transforms.add("third_person", transform(0, 0, 0, 0, 0.25F / 0.15F, 0, 0.4F / 0.15F));
        transforms.add("non_gui", transform(0, 0, 0, 0, 0.25F / 0.15F, 0, 0.4F / 0.15F));
        transforms.add("ground", transform(0, 0, 0, 0, 0, 0, 0.4F / 0.15F));
        transforms.add("fixed", transform(0, 0, 0, 0, 0, 0, 0.4F / 0.15F));
        model.add("contextTransforms", transforms);
        return model;
    }

    private static JsonObject transform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        JsonObject t = new JsonObject();
        t.add("rotation", numbers(rx, ry, rz));
        t.add("translation", numbers(tx, ty, tz));
        t.addProperty("scale", (double) scale);
        return t;
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }
}
