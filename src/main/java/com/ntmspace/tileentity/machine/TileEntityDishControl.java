package com.ntmspace.tileentity.machine;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.block.machine.MachineStardar;
import com.ntmspace.registry.SpaceMachinesD;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityDishControl}: remembers the core of one StarDar ({@code linkPosition}, saved as 1.7's
 * {@code linkPosX/Y/Z} block coordinates) and re-checks the link once after loading. No inventory.
 */
public class TileEntityDishControl extends TileEntityMachineBase {

    public BlockPos linkPosition = BlockPos.ZERO;
    public boolean isLinked;
    private boolean foundLink = false;

    public TileEntityDishControl(BlockPos pos, BlockState state) {
        super(SpaceMachinesD.DISH_CONTROL_BE.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "container.dishControl";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDishControl te) {
        // 1.7: the saved link is verified on the first tick (the StarDar may be gone)
        if (!te.foundLink && te.isLinked) {
            te.isLinked = te.establishLink(te.linkPosition);
            te.foundLink = true;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityDishControl te) {}

    /** 1.7 {@code getLinkedDishClientSafe}: the linked StarDar's block entity, if it is loaded. */
    public @Nullable TileEntityMachineStardar getLinkedDish() {
        if (!isLinked || level == null) return null;
        return level.getBlockEntity(linkPosition) instanceof TileEntityMachineStardar stardar ? stardar : null;
    }

    /** 1.7 {@code starDarHasDisk}: drives the console's drive part. */
    public boolean starDarHasDisk() {
        TileEntityMachineStardar link = getLinkedDish();
        return link != null && link.hasDrive();
    }

    /** 1.7 {@code TryLink}: links to the StarDar the reactor sensor remembers. */
    public void tryLink(ItemStack stack) {
        isLinked = linkWithSensor(stack);
        setChanged();
    }

    private boolean linkWithSensor(ItemStack stack) {
        // Get the StarDar coordinates from the sensor
        BlockPos target = ItemReactorSensor.getTarget(stack);
        return target != null && establishLink(target);
    }

    private boolean establishLink(BlockPos pos) {
        if (level == null) return false;

        // Get the StarDar block
        if (!(level.getBlockState(pos).getBlock() instanceof MachineStardar)) return false;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityMachineStardar) {
            // Dish linked successfully
            linkPosition = core.immutable();
            return true;
        }

        return false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isLinked);
        buf.writeInt(linkPosition.getX());
        buf.writeInt(linkPosition.getY());
        buf.writeInt(linkPosition.getZ());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isLinked = buf.readBoolean();
        linkPosition = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isLinked", isLinked);
        // 1.7 left the position out while it was 0, 0, 0
        if (!linkPosition.equals(BlockPos.ZERO)) {
            output.putInt("linkPosX", linkPosition.getX());
            output.putInt("linkPosY", linkPosition.getY());
            output.putInt("linkPosZ", linkPosition.getZ());
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isLinked = input.getBooleanOr("isLinked", false);
        linkPosition = new BlockPos(input.getIntOr("linkPosX", 0), input.getIntOr("linkPosY", 0), input.getIntOr("linkPosZ", 0));
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 1, p.getZ() + 2);
    }
}
