package com.ntmspace.client.render;

import com.hbm.client.model.WavefrontObj;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.terrain.BlockRubberTall;
import com.ntmspace.block.terrain.PlantType;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code RenderRubberCactus}: the tall rubber plant's OBJ {@code plant_spike} (two crossed planes three
 * blocks tall, four leaves tilted out at the foot) with its type's block texture, standing on the block's centre. 1.7
 * drew it twice in the chunk pass, turned half a turn apart, so each face shows from both sides with the texture the
 * right way round; here likewise, two culled copies.
 */
public final class RenderRubberCactus extends GemBER<BlockRubberTall.Plant, RenderRubberCactus.State> {

    public static final class State extends CompatRenderState {
        public PlantType.RubberTall type = PlantType.RubberTall.CACT;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(NtmSpace.MOD_ID, "models/plant_spike.obj"));

    @Override
    protected void setup(Gem.Model<State> declaration) {
        // three blocks tall: draw while the plant's top may be in view
        Gem.Part<State> plant = declaration.offScreen().viewDistance(64).joint().translate(0.5F, 0F, 0.5F);
        for (PlantType.RubberTall type : PlantType.RubberTall.values()) {
            Surface surface = Surface.cutoutCull(NtmSpace.MOD_ID + ":textures/blocks/rubber_tall." + type.getSerializedName() + ".png");
            plant.child(model.all(), surface).rotate(Gem.Axes.Y, 180F).visibleIf(state -> state.type == type);
            plant.child(model.all(), surface).visibleIf(state -> state.type == type);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockRubberTall.Plant plant, State state, float partialTicks, Vec3 cameraPosition) {
        state.type = plant.getBlockState().hasProperty(BlockRubberTall.TYPE)
                ? plant.getBlockState().getValue(BlockRubberTall.TYPE) : PlantType.RubberTall.CACT;
    }
}
