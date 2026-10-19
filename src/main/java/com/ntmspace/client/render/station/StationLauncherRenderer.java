package com.ntmspace.client.render.station;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ntmspace.client.render.rocket.RocketPronter;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code RenderOrbitalStation} for the launcher: the docking port with {@code docking_port_launcher.png},
 * its four arms folding in (90 degrees) while a rocket is docked, and the rocket being built hanging under the port
 * ({@code getHeight() + 0.5} below the port's top, not deployed) unless the rocket has extra issues. The rocket is
 * {@link RocketPronter}, as in flight; it has no fixed shape, so it is drawn outside the declared model.
 */
public final class StationLauncherRenderer extends GemBER<TileEntityOrbitalStationLauncher, StationLauncherRenderer.LauncherState> {

    public static final class LauncherState extends CompatRenderState {
        public float yaw;
        public float arm;
        public @Nullable RocketStruct rocket;
    }

    private final WavefrontObj model = StationRenderers.obj("machines/docking_port.obj");
    private final Surface surface = Surface.cutoutCull(StationRenderers.tex("machines/docking_port_launcher.png"));
    private final RocketPronter pronter = new RocketPronter();

    @Override
    protected void setup(Gem.Model<LauncherState> declaration) {
        Gem.Part<LauncherState> port = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 1.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        port.child(model.part("Port"), surface);
        for (int i = 0; i < 4; i++) {
            // cha cha real smooth
            port.joint().rotate(Gem.Axes.Y, 90F * i)
                    .child(model.part("ArmZP"), surface)
                    // one hop this time, criss cross, one hop this time
                    .translate(0F, -1.75F, -2F)
                    .angle(Gem.Axes.X, state -> -state.arm)
                    .translate(0F, 1.75F, 2F);
        }
    }

    @Override
    public LauncherState createRenderState() {
        return new LauncherState();
    }

    @Override
    public void extract(TileEntityOrbitalStationLauncher be, LauncherState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = StationRenderers.stationYaw(be);
        state.arm = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        state.rocket = be.rocket;
    }

    @Override
    protected void submitUndeclared(LauncherState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        RocketStruct rocket = state.rocket;
        if (rocket == null || rocket.extraIssues.size() != 0) return;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0, -rocket.getHeight() - 0.5, 0);
        pronter.pront(rocket, null, poseStack, sink, state.lightCoords, false, 0, 0, 0F);
        poseStack.popPose();
    }
}
