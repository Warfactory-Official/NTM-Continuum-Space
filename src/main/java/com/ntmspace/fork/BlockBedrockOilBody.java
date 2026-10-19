package com.ntmspace.fork;

import com.ntmspace.block.BlockBodyOre;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * hbm-space's {@code ore_bedrock_oil} off Kerbin ({@code BlockOreFluid(rock, null, OIL)}, metadata = body): the bedrock
 * oil bubble Tekto's {@code MapGenBedrockOil} places with Tekto's metadata, whose fracking deposit is Tekto crude and
 * hydrogen-rich gas ({@code getPrimaryFluid}/{@code getSecondaryFluid}). Kerbin's form stays HBM's
 * {@code hbm:ore_bedrock_oil}; this block carries the other bodies in a {@code body} state (as the per-body ores do) and
 * looks like HBM's. Unbreakable, drops nothing; its deposit is {@code data/ntmspace/value_maps/patches/hbm/block/fluid_deposit_states.json}.
 */
public class BlockBedrockOilBody extends CompatBlock {

    public BlockBedrockOilBody(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockBodyOre.BODY, BlockBodyOre.Body.TEKTO));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockBodyOre.BODY);
    }
}
