package com.ntmspace.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.ntmspace.client.render.rocket.RenderCombatDropPod;
import com.ntmspace.registry.DropPodContent;
import com.ntmspace.tileentity.machine.storage.TileEntityCombatDropPod;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.Random;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code RenderCombatPod}: the landed pod, centred, 1.25 blocks up ({@code 1.5 - 0.25}), turned by a yaw
 * (0-360), pitch (0-8) and roll (0-6) drawn from a {@code Random} seeded with the 1.7 position identity
 * {@code (y + z * 27644437) * 27644437 + x}, so every pod lands askew in its own fixed way. The two hatches swing open
 * from the tile entity's interpolated {@code hatchopen} / {@code hatchopen2}; the bomb body stays. Culled, smooth-shaded,
 * in the skin of its colour (as the entity). 1.7 moved hatch 2 back by {@code -open * 0.01}, using hatch 1's angle, not
 * its own; kept. Default 1.7 tile renderer distance (64 blocks); drawn off-screen (1.7 render box -3..+4, 8 high).
 */
public final class RenderCombatPod extends GemBER<TileEntityCombatDropPod, RenderCombatPod.State> {

    private static final Random RAND = new Random();

    private final WavefrontObj model = RenderCombatDropPod.model();
    private final Surface white = Surface.cutoutCull(RenderCombatDropPod.skin(0));
    private final Surface red = Surface.cutoutCull(RenderCombatDropPod.skin(1));
    private final Surface yellow = Surface.cutoutCull(RenderCombatDropPod.skin(2));

    public static void register(ClientRegistrations.Renderers renderers) {
        HbmBlockEntityRenderers.Sink sink = new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends CompatRenderState> void register(BlockEntityType<? extends T> type,
                                                                                         CompatBERProvider<T, S> provider) {
                renderers.blockEntity(type, provider);
            }
        };
        HbmBlockEntityRenderers.regGem(sink, DropPodContent.COMBAT_DROP_BE.get(), RenderCombatPod::new);
    }

    public static final class State extends CompatRenderState {
        public float yaw, pitch, roll;
        public float open, open2;
        public int color;
    }

    private Surface skin(State state) {
        return switch (state.color) {
            case 1 -> red;
            case 2 -> yellow;
            default -> white;
        };
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> pod = declaration.offScreen().viewDistance(64).joint()
                .translate(0.5F, 1.5F, 0.5F)
                .translate(0F, -0.25F, 0F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .angle(Gem.Axes.X, state -> state.pitch)
                .angle(Gem.Axes.Z, state -> state.roll);
        pod.child(model.part("hatch1"), white).skin(this::skin)
                .slide(Gem.Axes.Y, state -> state.open * 0.013F)
                .slide(Gem.Axes.Z, state -> state.open * 0.01F)
                .angle(Gem.Axes.X, state -> state.open);
        pod.child(model.part("hatch2"), white).skin(this::skin)
                .slide(Gem.Axes.Y, state -> -state.open2 * 0.013F)
                .slide(Gem.Axes.Z, state -> -state.open * 0.01F)
                .angle(Gem.Axes.X, state -> state.open2);
        pod.child(model.part("bomb"), white).skin(this::skin);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCombatDropPod pod, State state, float partialTicks, Vec3 cameraPosition) {
        var pos = pod.getBlockPos();
        RAND.setSeed(identity(pos.getX(), pos.getY(), pos.getZ()));
        state.yaw = (float) (RAND.nextDouble() * 360);
        state.pitch = (float) (RAND.nextDouble() * 8);
        state.roll = (float) (RAND.nextDouble() * 6);
        state.open = (float) (pod.prevHatchopen + (pod.hatchopen - pod.prevHatchopen) * partialTicks);
        state.open2 = (float) (pod.prevHatchopen2 + (pod.hatchopen2 - pod.prevHatchopen2) * partialTicks);
        state.color = pod.color;
    }

    /** 1.7 {@code fauxpointtwelve.BlockPos.getIdentity} (int arithmetic, overflow included). */
    public static int identity(int x, int y, int z) {
        return (y + z * 27644437) * 27644437 + x;
    }
}
