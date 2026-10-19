package com.ntmspace.client.render;

import com.hbm.client.model.WavefrontObj;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.generic.BlockAlgaeFilm;
import com.ntmspace.tileentity.machine.TileEntityAlgaeFilm;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code RenderAlgaeFilm} (a chunk-pass renderer in 1.7): the OBJ {@code algae_film} from the block's
 * centre at its foot, with the block's texture, turned by the facing - metadata 2 (north) not at all, 4 (west) a
 * quarter turn, 3 (south) a half, 5 (east) three quarters ({@code ObjUtil.renderWithIcon}'s {@code rotateAroundY}).
 */
public final class RenderAlgaeFilm extends GemBER<TileEntityAlgaeFilm, RenderAlgaeFilm.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/blocks/algae_film.obj"));
    private final Surface surface = Surface.cutoutCull(NtmSpace.MOD_ID + ":textures/blocks/algae_film.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.joint().translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, state -> state.yaw)
                .child(model.all(), surface);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityAlgaeFilm film, State state, float partialTicks, Vec3 cameraPosition) {
        Direction facing = film.getBlockState().hasProperty(BlockAlgaeFilm.FACING)
                ? film.getBlockState().getValue(BlockAlgaeFilm.FACING) : Direction.NORTH;
        state.yaw = yaw(facing);
    }

    /** 1.7: meta 4 = 90, 5 = 270, 3 = 180, else 0 (degrees about +Y). */
    public static float yaw(Direction facing) {
        return switch (facing) {
            case WEST -> 90F;
            case EAST -> 270F;
            case SOUTH -> 180F;
            default -> 0F;
        };
    }
}
