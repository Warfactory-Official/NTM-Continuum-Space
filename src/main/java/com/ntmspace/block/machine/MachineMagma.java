package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityMachineMagma;
import com.ntmspace.trait.CelestialTraits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineMagma} (Magma Drill): a 7x7x7 cube ({@code getDimensions} {3, 3, 3, 3, 3, 3}, offset 3,
 * three blocks up), core in the middle. Six {@code TileEntityProxyCombo().power().fluid()} cells on the back face
 * ({@code makeExtra} at core - 3 dir, one and two below, and one to each side); the 1.7 {@code getConPos} points are one
 * block further back, so the back faces of those cells are the ports (1.7 subscribed power there only: the drill's
 * fluid side, an empty tank array "probably to handle cooling fluids", was never used and is not ported). It has no
 * GUI; looking at it shows the drill.
 */
public class MachineMagma extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay {

    public MachineMagma(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMagma(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {3, 3, 3, 3, 3, 3}; }
    @Override public int getOffset() { return 3; }
    @Override public int getHeightOffset() { return 3; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        DirPos[] ports = TileEntityMachineMagma.conPos(core, MachinePorts.dir(facing));
        int[] dims = getDimensions();
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, dims, facing, pos), ports,
                ROLE_POWER_IN, visitor);
    }

    /** The six {@code makeExtra} cells: {@code TileEntityProxyCombo().power().fluid()}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos extra : extras(core, facing)) visitor.passiveCell(extra, MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    /** 1.7 {@code fillSpace}'s {@code makeExtra} positions. */
    static List<BlockPos> extras(BlockPos core, Direction facing) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        List<BlockPos> out = new ArrayList<>();
        for (int y : new int[] {-1, -2}) {
            for (int side : new int[] {0, 1, -1}) out.add(MachinePorts.offset(core, dir, -3, rot, side, y));
        }
        return out;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineMagma drill)) return null;

        List<String> text = new ArrayList<>();
        // 1.7 compared CelestialBody.getBody(world).name with "moho" (on the client too)
        if (!"moho".equals(CelestialTraits.bodyOf(level))) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! MUST BE ON MOHO ! ! !");
        } else if (!drill.validPosition) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! INSUFFICIENT LAVA FOUND ! ! !");
        } else {
            text.add((drill.power < drill.consumption ? ChatFormatting.RED : ChatFormatting.GREEN) + "Power: "
                    + BobMathUtil.getShortNumber(drill.power) + "HE");
            // 1.7 listed its tanks here too; it has none ("TODO: probably to handle cooling fluids")
            for (Mats.MaterialStack stack : drill.liquids) {
                text.add(ChatFormatting.YELLOW + stack.material.getDisplayName() + ": " + Mats.formatAmount(stack.amount, false));
            }
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
