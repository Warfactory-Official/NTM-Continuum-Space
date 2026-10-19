package com.ntmspace.block.dyson;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModSatelliteItems;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import java.util.ArrayList;
import java.util.List;
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
 * hbm-space's {@code MachineDysonReceiver}: {@code getDimensions} {2, 0, 4, 2, 2, 2}, offset 2, plus the main structure
 * reaching eight behind the core, the 13 x 13 dish ten up and the tower 17 up (only the base box is checked for space
 * when placing, as in 1.7, so the rest overwrites what stands there). No proxies, no ports. Right-click with a
 * satellite ID-chip installs it (if empty), an empty hand takes it back; sneaking does nothing.
 */
public class MachineDysonReceiver extends BlockMultiblockMachine implements ILookOverlay {

    /** The extra boxes of 1.7's {@code fillSpace}: {up offset, dims...}. */
    private static final int[][] BOXES = {
            // Main structure
            {0, 1, 0, 8, -4, 1, 1},
            {0, 4, 0, 4, 0, 1, 1},
            // Dish
            {10, 1, 0, 6, 6, 6, 6},
            // Tower
            {0, 17, 0, 1, 1, 1, 1},
    };

    public MachineDysonReceiver(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonReceiver(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {2, 0, 4, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    private static int[] dims(int[] box) {
        return new int[] {box[1], box[2], box[3], box[4], box[5], box[6]};
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core.above(box[0]), dims(box), facing, visitor);
    }

    @Override
    public int[][] getAllDimensions() {
        int[][] all = new int[BOXES.length + 1][];
        all[0] = getDimensions();
        for (int i = 0; i < BOXES.length; i++) {
            int[] d = dims(BOXES[i]);
            all[i + 1] = new int[] {d[0], d[1], d[2], d[3], d[4], d[5], 0, BOXES[i][0], 0};
        }
        return all;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return interact(level, pos, player, stack);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return interact(level, core, player, ItemStack.EMPTY);
    }

    private InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonReceiver receiver)) return InteractionResult.FAIL;

        ItemStack chip = receiver.inventory.getStackInSlot(0);
        if (!held.isEmpty() && held.is(ModSatelliteItems.sat_chip.get())) {
            if (!chip.isEmpty()) return InteractionResult.SUCCESS; // 1.7 returned false; nothing else happens
            receiver.inventory.setStackInSlot(0, held.copy());
            receiver.markChanged();
            held.setCount(0);
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (held.isEmpty() && !chip.isEmpty()) {
            if (player.getInventory().add(chip.copy())) {
                receiver.inventory.setStackInSlot(0, ItemStack.EMPTY);
                receiver.markChanged();
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonReceiver receiver)) return null;

        long energyOutput = 0;
        if (receiver.isReceiving && receiver.swarmConsumers > 0) {
            energyOutput = TileEntityDysonReceiver.getEnergyOutput(receiver.swarmCount) / receiver.swarmConsumers * 20;
        }

        List<String> text = new ArrayList<>();
        if (receiver.swarmId > 0) {
            text.add("ID: " + receiver.swarmId);
            text.add("Swarm: " + receiver.swarmCount + " members");
            text.add("Consumers: " + receiver.swarmConsumers + " consumers");
            text.add("Power: " + BobMathUtil.getShortNumber(energyOutput) + "HE/s");
        } else {
            text.add("No Satellite ID-Chip installed!");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
