package com.ntmspace.fork;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.lib.Library;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * hbm-space's {@code MachineCapacitor}, a working store there: power in on the face it was placed against, out the far
 * side through a straight run of {@code capacitor_bus}. The charge travels with the dropped item.
 */
public class ComplexCapacitor extends CompatBlock implements TickingBlock, ILookOverlay, ICapabilityBlock {

    /** 1.7's metadata: the side of the clicked neighbour, i.e. this block's input face. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public final long power;
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fe()
            .faces(ComplexCapacitorEntity.class, (owner, side) -> side == owner.inputSide().toDirection());
    @Override public MachineCaps caps() { return CAPS; }

    public ComplexCapacitor(Properties properties, long power) {
        super(properties);
        this.power = power;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComplexCapacitorEntity(pos, state);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAround(server, pos);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ComplexCapacitorEntity battery)) return null;
        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(battery.getPower()) + " / " + Library.getShortNumber(battery.getMaxPower()) + "HE");
        double percent = battery.getMaxPower() == 0 ? 0 : (double) battery.getPower() / (double) battery.getMaxPower();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add("&[" + color + "&]" + (charge / 100D) + "%");
        text.add("§a-> §r+" + Library.getShortNumber(battery.powerReceived) + "HE/t");
        text.add("§c<- §r-" + Library.getShortNumber(battery.powerSent) + "HE/t");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof ComplexCapacitorEntity capacitor) {
            ForkBaseChanges.CAPACITOR_STATE.get(stack).ifPresent(capacitor::restore);
            capacitor.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        // a player break removes the block entity before the drops roll, so the loot context carries it
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be == null) be = params.getLevel().getBlockEntity(BlockPos.containing(params.getParameter(LootContextParams.ORIGIN)));
        if (be instanceof ComplexCapacitorEntity capacitor && (capacitor.getPower() > 0 || capacitor.getMaxPower() != power)) {
            ForkBaseChanges.CAPACITOR_STATE.set(stack,
                    new CapacitorCharge(capacitor.getPower(), capacitor.getMaxPower()));
        }
        return List.of(stack);
    }

    /** hbm-space's order: capacity, charge and discharge, the description, {@code extra}, then the stored charge. */
    public List<Component> tooltip(ItemStack stack, List<Component> extra) {
        var carried = ForkBaseChanges.CAPACITOR_STATE.get(stack);
        var maximum = Math.min(carried.map(CapacitorCharge::maxPower).orElse(power), power);
        if (maximum <= 0) throw new IllegalArgumentException("A complex capacitor needs positive capacity");
        List<Component> list = new ArrayList<>();
        list.add(EnergyNetBlockItem.gold("Stores up to " + Library.getShortNumber(maximum) + "HE"));
        list.add(EnergyNetBlockItem.gold("Charge speed: " + Library.getShortNumber(maximum / 100) + "HE"));
        list.add(EnergyNetBlockItem.gold("Discharge speed: " + Library.getShortNumber(maximum / 300) + "HE"));
        list.add(Component.translatable("desc.capacitor.0").withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("desc.capacitor.1").withStyle(ChatFormatting.YELLOW));
        list.addAll(extra);
        var stored = carried.map(state -> IBatteryItem.clampCharge(state.power(), maximum)).orElse(null);
        if (stored != null) {
            list.add(Component.literal(Library.getShortNumber(stored) + "/" + Library.getShortNumber(maximum) + "HE")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return list;
    }
}
