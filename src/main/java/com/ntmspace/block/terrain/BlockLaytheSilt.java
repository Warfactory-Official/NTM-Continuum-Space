package com.ntmspace.block.terrain;

import net.minecraft.world.level.material.MapColor;
import dev.golem.core.world.CompatFallingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Laythe's falling silt: white, a lighter cave band (1.7 metadata 3), or its darker band (metadata 6). */
public final class BlockLaytheSilt extends CompatFallingBlock {
    public static final IntegerProperty SHADE = IntegerProperty.create("shade", 0, 2);

    private static final MapColor[] MAP_COLORS = TextureMapColors.sample(
            BlockLaytheSilt.class, "/assets/ntmspace/textures/blocks/laythe_silt.png", tint(0), tint(1), tint(2));

    public static MapColor mapColor(BlockState state) {
        return MAP_COLORS[state.getValue(SHADE)];
    }

    public BlockLaytheSilt(BlockBehaviour.Properties properties) {
        super(properties, BlockLaytheSilt::new);
        registerDefaultState(stateDefinition.any().setValue(SHADE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHADE);
    }

    public static int color(BlockState state) {
        return tint(state.getValue(SHADE));
    }

    private static int tint(int shade) {
        var brightness = Math.round((1F - shade * 3 / 15F) * 255);
        return 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getMapColor(level, pos).col;
    }
}
