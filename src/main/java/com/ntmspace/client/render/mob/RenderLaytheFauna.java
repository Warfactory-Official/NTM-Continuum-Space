package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ntmspace.entity.mob.EntityDepthSquid;
import com.ntmspace.entity.mob.EntityScrapFish;
import com.ntmspace.entity.mob.EntityScutterfish;
import com.ntmspace.entity.mob.EntityScuttlecrab;
import com.ntmspace.entity.mob.EntitySifterEel;
import com.ntmspace.entity.mob.IEntityEnumMulti;
import java.util.Locale;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Laythe's water fauna: hbm-space {@code RenderScutter} + {@code ModelScutter}, and {@code RenderEntityMulti} with
 * {@code ModelScuttlecrab}, {@code ModelDepthSquid}, {@code ModelScrapFish} and {@code ModelSifterEel}. Each
 * {@code draw} is the 1.7 model's {@code render} line for line ({@code cyN} are its walk-cycle phases).
 */
public final class RenderLaytheFauna {

    private RenderLaytheFauna() {}

    /** 1.7 {@code RenderEntityMulti}: {@code textures/entity/<enum>_<constant>.png}, lower case. */
    static Identifier multiSkin(IEntityEnumMulti entity) {
        Enum<?> value = entity.getEnum();
        return SpaceMobModels.texture(value.getDeclaringClass().getSimpleName().toLowerCase(Locale.US) + "_"
                + value.name().toLowerCase(Locale.US));
    }

    private static double cy(float limbSwing, double phase) {
        return Math.sin(limbSwing % (Math.PI * 2) - Math.PI * phase);
    }

    /** {@code RenderScutter(new ModelScutter(), 0.3F)}: one skin, {@code scutterfish.png}. */
    public static final class Scutterfish extends RenderObjMob<EntityScutterfish> {

        public Scutterfish(EntityRendererProvider.Context context) {
            super(context, 0.3F);
        }

        @Override
        protected Identifier skin(EntityScutterfish entity) {
            return SpaceMobModels.SCUTTERFISH;
        }

        @Override
        protected void draw(State state, PoseStack ps, Parts parts) {
            WavefrontObj m = SpaceMobModels.scutterfish();
            float limbSwing = state.walkAnimationPos;
            double cy0 = cy(limbSwing, 0.0);
            double cy1 = cy(limbSwing, 0.2);
            double cy2 = cy(limbSwing, 0.4);
            double cy3 = cy(limbSwing, 0.6);

            rotZ(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            parts.part(m, "body");

            ps.pushPose();
            rotX(ps, state.xRot);
            rotY(ps, state.yRot);
            parts.part(m, "head");
            ps.popPose();

            fin(ps, parts, m, "leftfin1", cy0 * 20);
            fin(ps, parts, m, "rightfin1", cy0 * -20);
            fin(ps, parts, m, "leftfin2", cy1 * 20);
            fin(ps, parts, m, "rightfin2", cy1 * -20);
            fin(ps, parts, m, "leftfin3", cy2 * 20);
            fin(ps, parts, m, "rightfin3", cy2 * -20);

            ps.pushPose();
            rotZ(ps, cy1 * 10);
            rotX(ps, cy2 * -5);
            parts.part(m, "tailmeat");
            rotX(ps, cy3 * -5);
            parts.part(m, "tailthin");
            ps.popPose();
        }

        private static void fin(PoseStack ps, Parts parts, WavefrontObj m, String name, double degrees) {
            ps.pushPose();
            rotZ(ps, degrees);
            parts.part(m, name);
            ps.popPose();
        }
    }

    /** {@code RenderEntityMulti(new ModelScuttlecrab(), Scuttlecrab.class, 0.5F)}. */
    public static final class Scuttlecrab extends RenderObjMob<EntityScuttlecrab> {

        public Scuttlecrab(EntityRendererProvider.Context context) {
            super(context, 0.5F);
        }

        @Override
        protected Identifier skin(EntityScuttlecrab entity) {
            return multiSkin(entity);
        }

        @Override
        protected void draw(State state, PoseStack ps, Parts parts) {
            WavefrontObj m = SpaceMobModels.scuttlecrab();
            float limbSwing = state.walkAnimationPos * 6;
            double cy0 = cy(limbSwing, 0.0);
            double cy1 = cy(limbSwing, 0.4);
            double cy2 = cy(limbSwing, 0.8);
            double cy3 = cy(limbSwing, 1.2);
            double cy4 = cy(limbSwing, 1.6);

            rotZ(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            parts.part(m, "Body");

            eye(ps, parts, m, "EyeL", -0.125F, state);
            eye(ps, parts, m, "EyeR", 0.125F, state);

            parts.part(m, "ClawL");
            parts.part(m, "ClawR");

            leg(ps, parts, m, "LegL1", cy0 * 8);
            leg(ps, parts, m, "LegL2", cy1 * 8);
            leg(ps, parts, m, "LegL3", cy2 * 8);
            leg(ps, parts, m, "LegL4", cy3 * 8);

            leg(ps, parts, m, "LegR1", cy1 * -8);
            leg(ps, parts, m, "LegR2", cy2 * -8);
            leg(ps, parts, m, "LegR3", cy3 * -8);
            leg(ps, parts, m, "LegR4", cy4 * -8);
        }

        private static void eye(PoseStack ps, Parts parts, WavefrontObj m, String name, float x, State state) {
            ps.pushPose();
            ps.translate(x, 0.5625F, -0.3125F);
            rotX(ps, state.xRot);
            rotY(ps, state.yRot);
            ps.translate(-x, -0.5625F, 0.3125F);
            parts.part(m, name);
            ps.popPose();
        }

        private static void leg(PoseStack ps, Parts parts, WavefrontObj m, String name, double degrees) {
            ps.pushPose();
            rotZ(ps, degrees);
            parts.part(m, name);
            ps.popPose();
        }
    }

    /**
     * {@code RenderEntityMulti(new ModelDepthSquid(), DepthSquid.class, 0.5F)}. 1.7's model read the squid's
     * interpolated tentacle angle and body pitch; all eight tentacles draw part {@code T0} (T1..T7 are never used).
     */
    public static final class DepthSquid extends RenderObjMob<EntityDepthSquid> {

        public DepthSquid(EntityRendererProvider.Context context) {
            super(context, 0.5F);
        }

        @Override
        protected Identifier skin(EntityDepthSquid entity) {
            return multiSkin(entity);
        }

        @Override
        protected void extractMore(EntityDepthSquid entity, State state, float partialTicks) {
            state.tentacleAngle = Mth.lerp(partialTicks, entity.oldTentacleAngle, entity.tentacleAngle);
            state.squidPitch = Mth.lerp(partialTicks, entity.xBodyRotO, entity.xBodyRot);
        }

        @Override
        protected void draw(State state, PoseStack ps, Parts parts) {
            WavefrontObj m = SpaceMobModels.depthsquid();
            float limbSwing = state.walkAnimationPos;
            double cy0 = cy(limbSwing, 0.0);
            double cy1 = cy(limbSwing, 0.4);

            ps.translate(0, 1.0F, 0);
            rotX(ps, state.squidPitch);

            parts.part(m, "Body");

            ps.pushPose();
            rotX(ps, cy1 * 20);
            rotY(ps, cy0 * 10);
            parts.part(m, "FinL");
            ps.popPose();
            ps.pushPose();
            rotX(ps, cy1 * 20);
            rotY(ps, cy0 * -10);
            parts.part(m, "FinR");
            ps.popPose();

            for (int i = 0; i < 8; i++) {
                ps.pushPose();
                ps.translate(0, -0.9, 0);
                rotY(ps, (float) i * -45.0F);
                rotX(ps, state.tentacleAngle * (180F / (float) Math.PI));
                ps.translate(0, 0.9, 0);
                parts.part(m, "T0");
                ps.popPose();
            }
        }
    }

    /** {@code RenderEntityMulti(new ModelScrapFish(), ScrapFish.class, 0.5F)}. */
    public static final class ScrapFish extends RenderObjMob<EntityScrapFish> {

        public ScrapFish(EntityRendererProvider.Context context) {
            super(context, 0.5F);
        }

        @Override
        protected Identifier skin(EntityScrapFish entity) {
            return multiSkin(entity);
        }

        @Override
        protected void draw(State state, PoseStack ps, Parts parts) {
            WavefrontObj m = SpaceMobModels.scrapfish();
            float limbSwing = state.walkAnimationPos;
            double cy0 = cy(limbSwing, 0.0);
            double cy1 = cy(limbSwing, 0.2);
            double cy2 = cy(limbSwing, 0.4);
            double cy3 = cy(limbSwing, 0.6);
            double cy4 = cy(limbSwing, 0.8);
            double cy5 = cy(limbSwing, 1.0);

            rotZ(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            parts.part(m, "Body");

            ps.pushPose();
            ps.translate(0, 0, -0.3125F);
            rotX(ps, state.xRot);
            rotY(ps, state.yRot);
            ps.translate(0, 0, 0.3125F);
            parts.part(m, "Head");
            ps.popPose();

            finX(ps, parts, m, "FinL", cy0 * 20);
            finX(ps, parts, m, "FinR", cy0 * -20);
            finX(ps, parts, m, "VentralFL", cy1 * 20);
            finX(ps, parts, m, "VentralFR", cy1 * -20);
            finX(ps, parts, m, "VentralBL", cy2 * 20);
            finX(ps, parts, m, "VentralBR", cy2 * -20);

            ps.pushPose();
            rotY(ps, cy3 * 10);
            parts.part(m, "Dorsal");
            rotY(ps, cy4 * 6);
            parts.part(m, "Tail");
            rotY(ps, cy5 * 4);
            parts.part(m, "TailFin");
            ps.popPose();
        }
    }

    /** {@code RenderEntityMulti(new ModelSifterEel(), SifterEel.class, 0.5F)}. */
    public static final class SifterEel extends RenderObjMob<EntitySifterEel> {

        public SifterEel(EntityRendererProvider.Context context) {
            super(context, 0.5F);
        }

        @Override
        protected Identifier skin(EntitySifterEel entity) {
            return multiSkin(entity);
        }

        @Override
        protected void draw(State state, PoseStack ps, Parts parts) {
            WavefrontObj m = SpaceMobModels.siftereel();
            float limbSwing = state.walkAnimationPos;
            double cy0 = cy(limbSwing, 0.0);
            double cy1 = cy(limbSwing, 0.2);
            double cy2 = cy(limbSwing, 0.4);
            double cy3 = cy(limbSwing, 0.6);
            double cy4 = cy(limbSwing, 0.9);
            double cy5 = cy(limbSwing, 1.2);
            double cy6 = cy(limbSwing, 1.5);
            double cy7 = cy(limbSwing, 1.9);

            rotZ(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            ps.pushPose();
            ps.translate(0, 0, -0.5F);
            rotX(ps, state.xRot);
            rotY(ps, state.yRot);
            ps.translate(0, 0, 0.5F);
            parts.part(m, "Head");
            parts.part(m, "Jaw");
            ps.popPose();

            finX(ps, parts, m, "FinL", cy0 * 20);
            finX(ps, parts, m, "FinR", cy0 * -20);

            ps.pushPose();
            parts.part(m, "Body1");

            rotY(ps, cy1 * 10);
            rotY(ps, cy2 * -2);
            parts.part(m, "Dorsal1");

            parts.part(m, "Body2");
            rotY(ps, cy3 * -2);
            parts.part(m, "Dorsal2");
            parts.part(m, "Dorsal3");
            rotY(ps, cy4 * -6);

            parts.part(m, "Body3");

            parts.part(m, "Ventral1");
            parts.part(m, "Ventral2");
            parts.part(m, "Ventral3");
            rotY(ps, cy5 * -4);
            parts.part(m, "Dorsal4");
            parts.part(m, "Ventral4");
            parts.part(m, "Body4");
            rotY(ps, cy6 * -6);
            parts.part(m, "Body5");

            parts.part(m, "Body6");

            rotY(ps, cy7 * -5);
            parts.part(m, "Tail");
            ps.popPose();
        }
    }

    private static void finX(PoseStack ps, RenderObjMob.Parts parts, WavefrontObj m, String name, double degrees) {
        ps.pushPose();
        RenderObjMob.rotX(ps, degrees);
        parts.part(m, name);
        ps.popPose();
    }
}
