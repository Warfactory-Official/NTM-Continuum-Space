package com.ntmspace.block.atmosphere;

import com.hbm.api.block.IToolable;
import com.hbm.api.environment.Environment;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphereEditor;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code BlockAtmosphereEditor} ({@code atmosphere_editor}): a screwdriver turns it on and off, a hand drill
 * raises the throughput exponent (sneaking: lowers it), a defuser switches between emitting and capturing, a fluid
 * identifier picks the gas (any fluid; only gases reach the air). Looking at it shows its state and the gas's pressure.
 */
public class BlockAtmosphereEditor extends CompatBlock implements TickingBlock, IToolable, ILookOverlay {

    public BlockAtmosphereEditor(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAtmosphereEditor(pos, state);
    }

    @Override
    public Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (Environment.inOrbit(level)) return null;
        if (!(level.getBlockEntity(pos) instanceof TileEntityAtmosphereEditor editor)) return null;

        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        double pressure = atmosphere != null ? atmosphere.getPressure(editor.fluid) : 0;
        if (pressure < 0.0001) pressure = 0;
        pressure = Math.round(pressure * 1_000.0) / 1_000.0;

        List<String> text = new ArrayList<>();
        text.add("State: " + (editor.isOn ? "RUNNING" : "OFF"));
        text.add("Current gas: " + editor.fluid.getDisplayNameString() + " - " + pressure);
        text.add("Current mode: " + (editor.isEmitting ? "EMITTING" : "CAPTURING"));
        text.add("Current throughput: " + Math.pow(10, editor.throughputFactor) / CelestialTraits.MB_PER_ATM);
        return new Data(Component.translatable(getDescriptionId()).getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityAtmosphereEditor editor)) return false;
        switch (tool) {
            case SCREWDRIVER -> editor.isOn = !editor.isOn;
            case HAND_DRILL -> editor.throughputFactor += (player.isShiftKeyDown() ? -1 : 1);
            case DEFUSER -> editor.isEmitting = !editor.isEmitting;
            default -> {
                return false;
            }
        }
        editor.setChanged();
        return true;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileEntityAtmosphereEditor editor)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        editor.fluid = type;
        editor.setChanged();
        player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                .append(type.getDisplayName()).append(Component.literal("!")));
        return InteractionResult.SUCCESS;
    }

    /** Nothing to open (1.7 answered true on the client only; a side-dependent answer is not ported). */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
