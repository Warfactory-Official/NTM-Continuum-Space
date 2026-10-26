package com.ntmspace.block;

import com.ntmspace.atmosphere.AtmosphereText;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.machine.IItemFluidId;
import com.ntmspace.atmosphere.BlockSealable;
import com.ntmspace.tileentity.TileEntityAirPump;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code BlockAirPump} ({@code air_vent}, "Air Vent"): the block of {@link TileEntityAirPump}. A fluid
 * identifier sets the gas it vents (gases only); looking at it shows the tank, the seal and the air of the room. It never
 * seals a room itself (air passes through a vent).
 */
public class BlockAirPump extends CompatBlock implements TickingBlock, ILookOverlay, BlockSealable {

    public BlockAirPump(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAirPump(pos, state);
    }

    @Override
    public Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityAirPump pump)) return null;

        CBT_Atmosphere atmosphere = pump.currentAtmosphere;
        List<String> text = new ArrayList<>();
        boolean blink = System.currentTimeMillis() % 1000 < 500; // BobMathUtil.getBlink

        text.add(pump.tank.getTankType().getDisplayNameString() + ": " + pump.tank.getFill() + "/" + pump.tank.getMaxFill() + "mB");

        if (pump.tank.getFill() <= 10) {
            text.add("&[" + (blink ? 0xff0000 : 0xffff00) + "&]! ! ! " + Component.translatable("atmosphere.noTank").getString() + " ! ! !");
        } else if (!pump.hasSeal()) {
            text.add("&[" + (blink ? 0xff0000 : 0xffff00) + "&]! ! ! " + Component.translatable("atmosphere.noSeal").getString() + " ! ! !");
        }

        text.add(Component.translatable("atmosphere.name").getString() + ": ");

        boolean hasPressure = false;
        if (atmosphere != null) {
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                if (entry.pressure > 0.01) {
                    double pressure = Math.round(entry.pressure * 1000.0D) / 1000.0D; // BobMathUtil.roundDecimal(x, 3)
                    text.add(ChatFormatting.AQUA + " - " + entry.fluid.getDisplayNameString() + " - " + pressure + "atm");
                    hasPressure = true;
                }
            }
        }
        if (!hasPressure) text.add(ChatFormatting.AQUA + " - " + Component.translatable("atmosphere.vacuum").getString());

        if (pump.isRecycling()) text.add(ChatFormatting.GREEN + Component.translatable("atmosphere.recycling").getString());

        return new Data(Component.translatable(getDescriptionId()).getString(), 0xffff00, 0x404000, text);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileEntityAirPump pump)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        var type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        AtmosphereText.retypeGaseous(type, player, pump.tank, pump);
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code onBlockActivated} answered true for any click. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos) {
        return false;
    }
}
