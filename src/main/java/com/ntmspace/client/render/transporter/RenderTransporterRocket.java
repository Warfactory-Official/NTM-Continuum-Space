package com.ntmspace.client.render.transporter;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.ntmspace.NtmSpace;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code RenderTransporterRocket}: the pad ({@code transporter_pad}: "base", and the "pipe" swinging up to
 * 30 degrees about (0, 0.75, -0.75) as the rocket leaves or lands), turned 90 degrees plus the facing (N 0, W 90, S 180,
 * E 270), and the miner rocket (HBM's {@code miner_rocket}, culling off) 0.75 above the pad plus its flight height while
 * {@code launchTicks < 100}. Seen from 256 blocks, also off-screen (1.7 {@code INFINITE_EXTENT_AABB}).
 */
public final class RenderTransporterRocket extends GemBER<TileEntityTransporterRocket, RenderTransporterRocket.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public float pipe;
        public float lift;
        public boolean rocket;
    }

    private final WavefrontObj pad = WavefrontObj.load(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/machines/transporter_pad.obj"));
    private final WavefrontObj minerRocket = WavefrontObj.load(dev.golem.core.Ids.of("hbm", "models/miner_rocket.obj"));
    private final Surface padSurface = Surface.cutoutCull(NtmSpace.MOD_ID + ":textures/models/machines/transporter_pad.png");
    private final Surface rocketSurface = Surface.cutoutNoCull("hbm:textures/models/miner_rocket.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        root.child(pad.part("base"), padSurface);
        root.child(pad.part("pipe"), padSurface).spin(Gem.Axes.X, 0F, 0.75F, -0.75F, state -> state.pipe * -30.0F);
        root.child(minerRocket.all(), rocketSurface).slide(Gem.Axes.Y, state -> state.lift).visibleIf(state -> state.rocket);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityTransporterRocket pad, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (MachineCore.facingOrdinal(pad.getBlockState(), 2)) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.pipe = Mth.clamp(getPipeEngage(pad, partialTicks), 0, 1);
        state.rocket = pad.launchTicks < 100;
        state.lift = 0.75F + Mth.clamp(pad.launchTicks + (pad.hasRocket ? -partialTicks : partialTicks), 0, 200);
    }

    private static float getPipeEngage(TileEntityTransporterRocket pad, float interp) {
        if (pad.launchTicks >= 0) {
            return !pad.hasRocket ? (pad.launchTicks + interp) * 0.25F : 1.0F;
        } else {
            return 1 - (-pad.launchTicks - 1 + interp) * 0.25F;
        }
    }
}
