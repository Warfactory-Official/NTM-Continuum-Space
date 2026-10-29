package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityHydroponic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MachineHydroponic} ({@code hydrobay}, the Industrial Greenhouse): offset 0, a hollow 5 x 3 x 3
 * frame built from nine 1.7 {@code fillSpace} boxes ({@link #BOXES}; "hollow machines hate this weird trick"), with
 * three {@code TileEntityProxyCombo(true, true, true)} cells (the two base ends and the top centre). The three positions
 * one above the core and its two lateral neighbours stay empty: the greenhouse plants crops there
 * ({@link TileEntityHydroponic}).
 *
 * <p><b>The lamps.</b> 1.7 swapped the two top cells above the outer plants between {@code hydrobay} and
 * {@code dummy_beam} ({@code BlockDummyableBeam}: light 15, light opacity 0, forwarding everything to the machine) while
 * the machine had at least 200 HE. The port swaps those cells to {@link HydroponicLampCell}: light 15, hardness 10,
 * resistance 12. Shared-cell ownership keeps use, overlay and teardown attached to this core.
 *
 * <p><b>The soil.</b> 1.7 answered {@code canSustainPlant} yes on the core and the four blocks horizontally next to it,
 * so crops stayed on the machine's base ({@link #sustainsPlant}), and no plant anywhere else on it. That answer is
 * Golem Core's soil hook ({@code CompatBlock.onSustainPlant}: NeoForge/Forge {@code canSustainPlant}, Fabric through
 * Golem's plant survival check), so a crop's own survival check keeps it on the base.
 */
public class MachineHydroponic extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2,
        ILookOverlay, WorldlyContainerHolder {

    /**
     * The nine 1.7 {@code fillSpace} calls: origin offset {along dir, along rot, up} from the core, then the box. 1.7's
     * {@code fillSpace} skips each box's own origin, and so does {@link MultiblockHandlerXR#visitBox}.
     */
    static final int[][][] BOXES = {
            // Front/Back
            {{1, 2, 2}, {0, 1, 0, 0, 0, 4}},
            {{-1, 2, 2}, {0, 1, 0, 0, 0, 4}},
            // Top
            {{0, 2, 2}, {0, 0, 0, 0, 0, 4}},
            // Side caps
            {{0, 2, 1}, {1, 0, 1, 1, 0, 0}},
            {{0, -2, 1}, {1, 0, 1, 1, 0, 0}},
            {{0, 0, 0}, {2, 0, 0, 0, -2, 2}},
            {{0, 0, 0}, {2, 0, 0, 0, 2, -2}},
            // Top connector (1.7: "needs to point laterally to avoid disconnecting when light blocks are added"; the
            // port's cells find their core through the index, so the direction no longer matters)
            {{1, 0, 2}, {0, 0, 1, 0, 0, 0}},
            // Base
            {{0, 0, 0}, {0, 0, 0, 0, 2, 2}},
    };

    /**
     * The 1.7 {@code bounding} boxes, in the core's frame before 1.7's {@code getAABBRotationOffset}.
     */
    private static final AABB[] BOUNDS = {
            // Middle
            new AABB(-0.5, 0, -1.5, 0.5, 3, 1.5),
            // Sides
            new AABB(-1.125, 1, -2.5, 1.125, 3, -1.5),
            new AABB(-1.125, 1, 1.5, 1.125, 3, 2.5),
            // Side butts
            new AABB(-0.75, 0, -2.5, 0.75, 1, -1.5),
            new AABB(-0.75, 0, 1.5, 0.75, 1, 2.5),
    };

    public MachineHydroponic(Properties properties) {
        // Vanilla must query the hollow's live shape: its collision extends into adjacent air cells, while the
        // placement-time cube would cause the world collision scan to skip those carriers.
        super(properties.dynamicShape());
        for (AABB box : BOUNDS) bounding.add(box);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHydroponic(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 2, 2};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /**
     * 1.7 {@code checkRequirement}: the standard space and the hollow's {@code {2, -1, 1, 1, 2, 2}}.
     */
    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        return MultiblockHandlerXR.checkSpace(level, core, new int[]{2, -1, 1, 1, 2, 2}, clicked, facing);
    }

    static BlockPos origin(BlockPos core, Direction facing, int[] offset) {
        return MachinePorts.offset(core, MachinePorts.dir(facing), offset[0], MachinePorts.rot(facing), offset[1],
                offset[2]);
    }

    static boolean isCell(BlockPos core, Direction facing, BlockPos pos) {
        for (int[][] box : BOXES) {
            BlockPos origin = origin(core, facing, box[0]);
            if (!pos.equals(origin) && MachinePorts.inBox(origin, box[1], facing, pos)) return true;
        }
        return false;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[][] box : BOXES) MultiblockHandlerXR.visitBox(origin(core, facing, box[0]), box[1], facing, visitor);
        TileEntityHydroponic.Ports ports = TileEntityHydroponic.ports(core, MachinePorts.dir(facing));
        MachinePorts.Cells cells = pos -> isCell(core, facing, pos);
        MachinePorts.ports(core, facing, cells, ports.fluid(), ROLE_FLUID_IN | ROLE_FLUID_OUT, visitor);
        MachinePorts.ports(core, facing, cells, ports.power(), ROLE_POWER_IN, visitor);
    }

    /**
     * The three {@code makeExtra} cells: {@code TileEntityProxyCombo(true, true, true)} (items, power, fluid).
     */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos extra : extras(core, facing)) visitor.passiveCell(extra, MASK_ALL, PASSIVE_ANY);
    }

    /**
     * 1.7 {@code fillSpace}'s {@code makeExtra} positions.
     */
    public static List<BlockPos> extras(BlockPos core, Direction facing) {
        ForgeDirection rot = MachinePorts.rot(facing);
        return List.of(core.offset(rot.offsetX * 2, 0, rot.offsetZ * 2),
                core.offset(-rot.offsetX * 2, 0, -rot.offsetZ * 2),
                core.above(2));
    }

    @Override
    public int[][] getAllDimensions() {
        return new int[][]{getDimensions(), {2, -1, 1, 1, 2, 2}};
    }

    /**
     * 1.7 {@code canSustainPlant}: yes next to the core ({@link #sustainsPlant}), no anywhere else on the machine.
     */
    @Override
    protected dev.golem.core.hook.Verdict onSustainPlant(BlockGetter level, BlockPos soilPos, BlockState soil,
                                                         BlockState plant) {
        return sustainsPlant(level, soilPos) ? dev.golem.core.hook.Verdict.ALLOW : dev.golem.core.hook.Verdict.DENY;
    }

    /**
     * 1.7 {@code canSustainPlant}: "only the blocks immediately horizontally adjacent to the core can actually sustain
     * plants, so we do a 4 block search in all cardinals" (meta >= 12 is the core).
     */
    public static boolean sustainsPlant(BlockGetter level, BlockPos pos) {
        for (BlockPos at : new BlockPos[]{pos, pos.east(), pos.west(), pos.south(), pos.north()}) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof MachineHydroponic && !state.getValue(CELL)) return true;
        }
        return false;
    }

    /**
     * 1.7 inventory proxies: the core and the three extras.
     */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityHydroponic hydro)) return null;
        BlockPos core = hydro.getBlockPos();
        if (pos.equals(core)) return hydro;
        Direction facing = hydro.getBlockState().getValue(FACING);
        return extras(core, facing).contains(pos) ? hydro : null;
    }

    /**
     * 1.7 {@code printHook}.
     */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityHydroponic hydro)) return null;
        List<String> text = new ArrayList<>();
        text.add((hydro.getPower() <= 200 ? ChatFormatting.RED : ChatFormatting.GREEN) + "Power: "
                + BobMathUtil.getShortNumber(hydro.getPower()) + "HE");
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + hydro.tanks[0].getTankType()
                                                                                     .getDisplayNameString() + ": "
                + hydro.tanks[0].getFill() + "/" + hydro.tanks[0].getMaxFill() + "mB");
        text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + hydro.tanks[1].getTankType()
                                                                                   .getDisplayNameString() + ": "
                + hydro.tanks[1].getFill() + "/" + hydro.tanks[1].getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
