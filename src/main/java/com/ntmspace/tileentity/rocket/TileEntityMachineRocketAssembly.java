package com.ntmspace.tileentity.rocket;

import java.util.function.Consumer;
import com.ntmspace.satellite.SpaceSatellites;

import com.hbm.items.ISatChip;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.block.rocket.MachineRocketAssembly;
import com.ntmspace.inventory.rocket.ContainerMachineRocketAssembly;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketStruct.RocketStage;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityMachineRocketAssembly}, the Vehicle Assembly Building: a payload slot, five stages of
 * fuselage / fins (landing legs) / thruster, the result, and two navigation-drive reference slots per stage ("from" and
 * "to", ghost copies the issue list reads). Every tick the stages are read bottom-up from the slots (an emptied stage
 * pulls the ones above it down), and when the rocket's height changes the VAB rebuilds its service platforms (a 9 x 9
 * ring at each stage's top) and the five spires holding them, or reports "VAB ceiling too low" when the rocket does not
 * fit under the first solid block above.
 *
 * <p>Slots: 0 payload, 1..15 stages (3 each, stage 1 at 1..3), 16 result, 17..26 drives (2 each). 1.7 limited every slot
 * to 8 items. The platforms are cells of this multiblock (indexed to the core, torn down with it).
 */
public class TileEntityMachineRocketAssembly extends TileEntityMachineBase implements MenuProvider {

    public static final int SLOT_CAPSULE = 0;
    public static final int SLOT_RESULT = 1 + RocketStruct.MAX_STAGES * 3;
    public static final int SLOT_DRIVES = SLOT_RESULT + 1;
    public static final int SLOT_COUNT = SLOT_DRIVES + RocketStruct.MAX_STAGES * 2;
    public static final int STACK_LIMIT = 8;

    public static int fuselage(int stage) { return 1 + stage * 3; }
    public static int fins(int stage) { return 2 + stage * 3; }
    public static int thruster(int stage) { return 3 + stage * 3; }
    public static int driveFrom(int stage) { return SLOT_DRIVES + stage * 2; }
    public static int driveTo(int stage) { return driveFrom(stage) + 1; }

    public RocketStruct rocket = new RocketStruct();

    private int previousHeight = 0;
    private List<Integer> platforms = new ArrayList<>();

    private boolean platformFailed = false;

    public int currentStage;

    public TileEntityMachineRocketAssembly(BlockPos pos, BlockState state) {
        super(RocketBlocks.ROCKET_ASSEMBLY_BE.get(), pos, state, SLOT_COUNT); // capsule + stages + result + drives
    }

    @Override
    public String getDefaultName() {
        return "container.machineRocketAssembly";
    }

    /** The "from" and "to" drives of the stage the screen shows. */
    public ItemStack fromDrive() {
        return getItem(driveFrom(currentStage));
    }

    public ItemStack toDrive() {
        return getItem(driveTo(currentStage));
    }

    public int spireHeight() {
        return platforms.isEmpty() ? 0 : Math.min(platforms.get(platforms.size() - 1), level.getMaxY() - 1 - worldPosition.getY());
    }
    public int[] platforms() { return platforms.stream().mapToInt(Integer::intValue).toArray(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRocketAssembly te) {
        // updates the orbital station information and syncs it to the client, if necessary
        ItemVOTVdrive.getTarget(te.fromDrive(), level);
        ItemVOTVdrive.getTarget(te.toDrive(), level);

        te.rocket = te.buildRocket();

        int height = (int) te.rocket.getHeight();
        if (height != te.previousHeight) {
            te.rebuildPlatforms(level);
            te.previousHeight = height;
        }

        if (te.platformFailed) {
            te.rocket.addIssue(Component.translatable("desc.gui.rocket.ceilingTooLow").withStyle(ChatFormatting.RED));
        }

        te.networkPackNT();
    }

    /** The rocket in the slots; an empty stage takes the one above it. */
    RocketStruct buildRocket() {
        RocketStruct rocket = new RocketStruct(getItem(0));
        if (!getItem(0).isEmpty() && getItem(0).getItem() instanceof ISatChip) {
            rocket.satFreq = ISatChip.getFreqS(getItem(0));
        }
        for (int i = 1; i < RocketStruct.MAX_STAGES * 3; i += 3) {
            if (getItem(i).isEmpty() && getItem(i + 1).isEmpty() && getItem(i + 2).isEmpty()) {
                // Check for later stages and shift them up into empty stages
                if (i + 3 < RocketStruct.MAX_STAGES * 3 && (!getItem(i + 3).isEmpty() || !getItem(i + 4).isEmpty() || !getItem(i + 5).isEmpty())) {
                    setItem(i, getItem(i + 3));
                    setItem(i + 1, getItem(i + 4));
                    setItem(i + 2, getItem(i + 5));
                    setItem(i + 3, ItemStack.EMPTY);
                    setItem(i + 4, ItemStack.EMPTY);
                    setItem(i + 5, ItemStack.EMPTY);
                } else {
                    break;
                }
            }
            rocket.addStage(getItem(i), getItem(i + 1), getItem(i + 2));
        }
        return rocket;
    }

    private void rebuildPlatforms(Level level) {
        MachineRocketAssembly block = RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get();
        BlockPos core = worldPosition;
        int x = core.getX(), y = core.getY(), z = core.getZ();

        // Delete previously generated platforms
        for (int platform : platforms) {
            deletePlatform(level, block, platform);
        }
        platforms = new ArrayList<>();

        // Check headroom
        int maxHeight = Integer.MAX_VALUE;
        for (int h = 1; y + h < level.getMaxY(); h++) {
            BlockState above = level.getBlockState(new BlockPos(x, y + h, z));
            if (!above.canBeReplaced() && above.getBlock() != block) {
                maxHeight = h;
                break;
            }
        }

        double checkHeight = rocket.getHeight();
        if (rocket.capsule != null) checkHeight -= rocket.capsule.height();
        if (rocket.stages.size() > 0 && rocket.stages.get(0).thruster != null) checkHeight -= rocket.stages.get(0).thruster.height();

        if (checkHeight < maxHeight) {
            // Create platforms to stand on
            int targetHeight = 0;
            for (int i = 0; i < rocket.stages.size(); i++) {
                RocketStage stage = rocket.stages.get(i);
                RocketStage nextStage = i < rocket.stages.size() - 1 ? rocket.stages.get(i + 1) : null;

                if (stage.fuselage != null) targetHeight += stage.fuselage.height() * stage.getStack();
                if (nextStage != null && nextStage.thruster != null) targetHeight += nextStage.thruster.height();

                int platform = Math.round(targetHeight);

                if (platform > 0) {
                    addPlatform(level, block, platform);
                    platforms.add(platform);
                }
            }

            // Create a central spire (required so the VAB can be broken properly)
            for (int i = 1; i < targetHeight + 1; i++) {
                if (y + i >= level.getMaxY()) break;
                for (int[] spire : SPIRES) placeCell(level, block, new BlockPos(x + spire[0], y + i, z + spire[1]));
            }

            for (int[] spire : SPIRES) {
                for (int i = targetHeight + 1; y + i < level.getMaxY(); i++) {
                    BlockPos at = new BlockPos(x + spire[0], y + i, z + spire[1]);
                    if (level.getBlockState(at).getBlock() != block) break;
                    removeCell(level, block, at);
                }
            }

            platformFailed = false;
        } else {
            platformFailed = true;
        }
        setChanged();
    }

    /** The central spire and the four corner spires. */
    static final int[][] SPIRES = {{0, 0}, {-4, -4}, {4, -4}, {-4, 4}, {4, 4}};

    private void addPlatform(Level level, MachineRocketAssembly block, int height) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if ((x == -4 || x == 4) && (z == -4 || z == 4)) continue;
                if (x == 0 && z == 0) continue; // 1.7 left the centre to the spire
                placeCell(level, block, worldPosition.offset(x, height, z));
            }
        }
    }

    private void deletePlatform(Level level, MachineRocketAssembly block, int height) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x == 0 && z == 0) continue;
                if ((x == -4 || x == 4) && (z == -4 || z == 4)) continue;
                removeCell(level, block, worldPosition.offset(x, height, z));
            }
        }
    }

    /** 1.7 {@code setBlock(..., machine_rocket_assembly, meta)} under {@code safeRem}: a cell of this VAB. */
    private void placeCell(Level level, MachineRocketAssembly block, BlockPos at) {
        block.placeDynamicCell(level, worldPosition, at);
    }

    /** 1.7 {@code setBlockToAir} under {@code safeRem}. */
    private void removeCell(Level level, MachineRocketAssembly block, BlockPos at) {
        block.removeDynamicCell(level, worldPosition, at);
    }

    /** Every position a platform or spire may occupy, for the teardown. */
    public static void visitDynamicCells(Level level, BlockPos core, Consumer<BlockPos> visitor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int h = 1; core.getY() + h < level.getMaxY(); h++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    visitor.accept(pos.set(core.getX() + x, core.getY() + h, core.getZ() + z));
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------- build

    public void construct() {
        if (!getItem(SLOT_RESULT).isEmpty() || !rocket.validate()) return;

        ItemStack stack = ItemCustomRocket.build(rocket);
        setItem(SLOT_RESULT, stack);

        for (int i = 0; i < SLOT_RESULT; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    public boolean canDeconstruct() {
        RocketStruct built = ItemCustomRocket.get(getItem(SLOT_RESULT));
        if (built == null) return false;
        for (int i = 0; i < SLOT_RESULT; i++) {
            if (!getItem(i).isEmpty()) return false;
        }

        return true;
    }

    public void deconstruct() {
        if (!canDeconstruct()) return;
        ItemStack source = getItem(SLOT_RESULT);
        int satFreq = ISatChip.getFreqS(source);
        RocketStruct built = ItemCustomRocket.get(source);

        ItemStack capsule = built.capsule != null ? new ItemStack(built.capsule.item()) : ItemStack.EMPTY;
        if (capsule.getItem() instanceof ISatChip) {
            ISatChip.setFreqS(capsule, satFreq);
            SpaceSatellites.ORBIT.get(source).ifPresent(settings -> SpaceSatellites.ORBIT.set(capsule, settings));
        }
        setItem(0, capsule);
        for (int i = 0; i < built.stages.size(); i++) {
            int o = i * 3;
            RocketStage stage = built.stages.get(built.stages.size() - 1 - i);
            setItem(o + 1, new ItemStack(stage.fuselage.item(), stage.fuselageCount));
            if (stage.fins != null) setItem(o + 2, new ItemStack(stage.fins.item()));
            setItem(o + 3, new ItemStack(stage.thruster.item(), stage.thrusterCount));
        }

        setItem(SLOT_RESULT, ItemStack.EMPTY);
    }

    // ---------------------------------------------------------------------------------------- inventory and sync

    /** 1.7 {@code getInventoryStackLimit}. */
    @Override
    public int getMaxStackSize() {
        return STACK_LIMIT;
    }

    /** 1.7 {@code isBreaking}: the drive slots hold ghost copies, which are not dropped. */
    public boolean dropsSlot(int slot) {
        return slot < SLOT_DRIVES;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        for (var i = 0; i < SLOT_COUNT; i++) if (!dropsSlot(i)) setItem(i, ItemStack.EMPTY);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        rocket.writeToByteBuffer(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        rocket = RocketStruct.readFromByteBuffer(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        int[] array = new int[platforms.size()];
        for (int i = 0; i < array.length; i++) array[i] = platforms.get(i);
        output.putIntArray("platforms", array);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        platforms = new ArrayList<>();
        for (int i : input.getIntArrayOr("platforms", new int[0])) platforms.add(i);
    }

    public void setCurrentStage(int stage) {
        currentStage = Math.max(0, Math.min(stage, RocketStruct.MAX_STAGES - 1));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerMachineRocketAssembly(containerId, inventory, this);
    }

    /** The payload a stack is, for the payload slot (1.7 {@code SlotCapsule}): a capsule or a satellite, not the pod. */
    public static boolean isCapsule(ItemStack stack) {
        var part = RocketParts.get(stack);
        if (part == null || part.type() != com.hbm.items.weapon.ItemCustomMissilePart.PartType.WARHEAD) return false;
        if (RocketParts.POD.equals(part.id())) return false;
        return part.isApollo() || part.isSatellite();
    }
}
