package com.ntmspace.block.dyson;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModSatelliteItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code MachineDysonLauncher} ("swarm shitter"), the Dyson Spinlauncher: offset 0, its
 * {@code getDimensions} {16, 0, 8, 0, 0, 0} only "the major offsets, for placing on walls and ceilings (rare)" - the
 * real footprint is the 28 boxes of its own {@code checkRequirement}/{@code fillSpace} (legs, back dish, dish, base),
 * each at its 1.7 origin and, as in 1.7's {@code MultiblockHandlerXR.fillSpace}, without that origin cell. Five
 * {@code TileEntityProxyCombo(false, true, false)} power cells along the base's left edge take power through their
 * outer face (1.7 {@code getConPos}, three to the side), and one {@code TileEntityProxyCombo(true, false, false)}
 * inventory cell at the back (eight behind the core) takes swarm members.
 *
 * <p>Right-click: a satellite ID-chip goes into the chip slot (if empty), an empty hand takes it back; a FATO plushie
 * (hbm-space's hidden plushie) switches on "sunset overdrive" for good. Sneaking does nothing, as in 1.7.
 */
public class MachineDysonLauncher extends BlockMultiblockMachine implements ILookOverlay, IEnergyConnectorBlock {

    /** {along dir, up, dims...} of each 1.7 box, in 1.7's order. */
    private static final int[][] BOXES = {
            // LEG FUCKER
            {6, 14, 0, 14, 1, 0, 6, -5},
            {6, 14, 0, 14, 1, 0, -5, 6},
            // BACKDISH CRAPPER
            {2, 7, 2, 0, 0, 3, 2, 2},
            {1, 6, 2, 0, 0, 3, 3, 3},
            {0, 5, 2, 0, 0, 3, 3, 3},
            {-1, 4, 2, 0, 0, 3, 3, 3},
            {-2, 3, 2, 0, 0, 3, 2, 2},
            // DISH SHITTER
            {6, 14, 2, 0, 0, 0, 3, 3},
            {6, 14, 0, 0, 0, 2, 3, 3},
            {5, 13, 2, 0, 0, 2, 10, 5},
            {4, 12, 2, 0, 0, 2, 10, 7},
            {3, 11, 2, 0, 0, 2, 10, 8},
            {2, 10, 2, 0, 0, 2, 10, 9},
            {1, 9, 2, 0, 0, 2, 10, 10},
            {0, 8, 2, 0, 0, 2, 10, 10},
            {-1, 7, 2, 0, 0, 2, 10, 10},
            {-2, 6, 2, 0, 0, 2, 10, 10},
            {-3, 5, 2, 0, 0, 2, 10, 10},
            {-4, 4, 2, 0, 0, 2, 9, 9},

            {-3, 3, 2, 0, 2, 0, 8, 8},
            {-4, 2, 2, 0, 2, 0, 7, 7},
            {-7, 2, 1, 0, 0, 0, 6, 6},

            {0, 2, 0, 0, 8, -8, 3, 3},
            {0, 1, 1, 1, 8, -8, 1, 1},

            {-7, 0, 1, 0, 0, 0, 4, 4},
            {-6, 0, 1, 0, 0, 0, 5, 5},
            {-5, 0, 1, 0, 0, 0, 6, 6},
            // BASE PIECE (OF ASS)
            {0, 0, 3, 0, 7, 0, 2, 2},
    };

    public MachineDysonLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonLauncher(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {16, 0, 8, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    private static BlockPos origin(BlockPos core, ForgeDirection dir, int[] box) {
        return core.offset(dir.offsetX * box[0], box[1], dir.offsetZ * box[0]);
    }

    private static int[] dims(int[] box) {
        return new int[] {box[2], box[3], box[4], box[5], box[6], box[7]};
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        ForgeDirection dir = MachinePorts.dir(facing);
        for (int[] box : BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, origin(core, dir, box), dims(box), clicked, facing)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        ForgeDirection dir = MachinePorts.dir(facing);
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(origin(core, dir, box), dims(box), facing, visitor);
        List<BlockPos> power = powerCells(core, facing);
        MachinePorts.ports(core, facing, power::contains, TileEntityDysonLauncher.conPos(core, dir), ROLE_POWER_IN, visitor);
    }

    /** The five {@code makeExtra} power cells: core - dir * 0..4 - rot * 2. */
    public static List<BlockPos> powerCells(BlockPos core, Direction facing) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        List<BlockPos> cells = new ArrayList<>();
        for (int k = 0; k <= 4; k++) cells.add(MachinePorts.offset(core, dir, -k, rot, -2, 0));
        return cells;
    }

    /** The {@code makeExtra} inventory cell: eight behind the core (meta 6, a {@code DOWN} dummy, is the inventory proxy). */
    public static BlockPos itemCell(BlockPos core, Direction facing) {
        ForgeDirection dir = MachinePorts.dir(facing);
        return core.offset(-dir.offsetX * 8, 0, -dir.offsetZ * 8);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos cell : powerCells(core, facing)) visitor.passiveCell(cell, MASK_ALL, PASSIVE_POWER_IN);
        visitor.passiveCell(itemCell(core, facing), MASK_ALL, PASSIVE_ITEMS);
    }

    /** The placement highlight: every box, at its forward and upward offset. */
    @Override
    public int[][] getAllDimensions() {
        int[][] all = new int[BOXES.length][];
        for (int i = 0; i < BOXES.length; i++) {
            int[] box = BOXES[i];
            all[i] = new int[] {box[2], box[3], box[4], box[5], box[6], box[7], box[0], box[1], 0};
        }
        return all;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND; // the empty hand: useAtCore
        return interact(level, pos, player, stack);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return interact(level, core, player, ItemStack.EMPTY);
    }

    /** 1.7 {@code onBlockActivated}: always handled (1.7 returned true), even when nothing happened. */
    private InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonLauncher launcher)) return InteractionResult.FAIL;

        ItemStack chip = launcher.inventory.getStackInSlot(TileEntityDysonLauncher.SLOT_CHIP);
        if (!held.isEmpty() && held.is(ModSatelliteItems.sat_chip.get())) {
            if (!chip.isEmpty()) return InteractionResult.SUCCESS; // 1.7 returned false; nothing else happens
            launcher.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_CHIP, held.copy());
            held.setCount(0);
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (held.isEmpty() && !chip.isEmpty()) {
            if (player.getInventory().add(chip.copy())) {
                launcher.inventory.setStackInSlot(TileEntityDysonLauncher.SLOT_CHIP, ItemStack.EMPTY);
                launcher.markChanged();
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        if (!launcher.sunsetOverdrive && !held.isEmpty() && TileEntityDysonLauncher.OVERDRIVE.test(held)) {
            launcher.sunsetOverdrive = true;
            held.shrink(1);
            launcher.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonLauncher launcher)) return null;
        List<String> text = new ArrayList<>();
        if (launcher.swarmId > 0) {
            text.add("ID: " + launcher.swarmId);
            text.add("Swarm: " + launcher.swarmCount + " members");
            text.add((launcher.power < TileEntityDysonLauncher.MAX_POWER ? ChatFormatting.RED : ChatFormatting.GREEN)
                    + "Power: " + BobMathUtil.getShortNumber(launcher.power) + "HE");
        } else {
            text.add("No Satellite ID-Chip installed!");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
