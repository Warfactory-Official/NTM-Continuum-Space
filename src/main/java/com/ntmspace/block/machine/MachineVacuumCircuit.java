package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityMachineVacuumCircuit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MachineVacuumCircuit} ({@code machine_vacuum_circuit}, the Vacuum Solderer): a 3 x 3 x 1 machine,
 * offset 0, whose every cell is a {@code TileEntityProxyCombo().inventory().power().fluid()} (the machine has no tanks,
 * so the fluid flag reaches nothing). Power comes in at the eight 1.7 {@code getConPos} points around the sides. Opens
 * the GUI on use; the look overlay warns "REQUIRES VACUUM" while the machine is not in a near vacuum.
 */
public class MachineVacuumCircuit extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay,
        WorldlyContainerHolder {

    public MachineVacuumCircuit(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineVacuumCircuit(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MachinePorts.Cells cells = pos -> Math.abs(pos.getX() - core.getX()) <= 1 && pos.getY() == core.getY()
                && Math.abs(pos.getZ() - core.getZ()) <= 1;
        MachinePorts.ports(core, facing, cells, TileEntityMachineVacuumCircuit.conPos(core), ROLE_POWER_IN, visitor);
    }

    /** Every cell is a proxy (items, power). */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_ITEMS);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineVacuumCircuit machine ? machine : null;
    }

    /** 1.7 {@code printHook}: the blinking warning, red and yellow. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineVacuumCircuit machine)) return null;
        List<String> text = new ArrayList<>();
        if (!machine.canOperate) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.noAir").getString() + " ! ! !");
        }
        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
