package com.ntmspace.block.generic;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityAlgaeFilm;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code BlockAlgaeFilm} ({@code algae_film}, "Microgravity Algae Film", "Converts CO2 back into O2. Only
 * functions in orbit."): iron, hardness 5, resistance 10, machine tab; drawn as the OBJ {@code algae_film} with the
 * block's own texture ({@code RenderAlgaeFilm}, a chunk-pass renderer in 1.7, the block entity's renderer here), turned
 * by its facing. 1.7 stored the facing as metadata 2..5 from the placer's yaw (the side facing the placer); the look
 * overlay lists both tanks and, outside orbit, blinks "REQUIRES ZERO-G" ({@code atmosphere.noGravity}). The machine
 * itself is {@link TileEntityAlgaeFilm}.
 */
public class BlockAlgaeFilm extends CompatBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockAlgaeFilm(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 1.7 {@code onBlockPlacedBy}: yaw quadrant 0..3 to metadata 2, 5, 3, 4 - the side that faces the placer. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAlgaeFilm(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityAlgaeFilm film)) return null;
        List<String> text = new ArrayList<>();
        if (!film.canOperate) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.noGravity").getString() + " ! ! !");
        }
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + film.tanks[0].getTankType().getDisplayNameString() + ": "
                + film.tanks[0].getFill() + "/" + film.tanks[0].getMaxFill() + "mB");
        text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + film.tanks[1].getTankType().getDisplayNameString() + ": "
                + film.tanks[1].getFill() + "/" + film.tanks[1].getMaxFill() + "mB");
        return new Data(Component.translatable(getDescriptionId()).getString(), 0xffff00, 0x404000, text);
    }
}
