package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.CrucibleUtil;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.inventory.material.SpaceMaterials;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code TileEntityMachineMagma}: on Moho, over a 3x3 of lava four blocks below the core, the drill draws
 * 10,000 HE per tick (1 MHE buffer) and every 10 world ticks adds an ingot of slag and four quanta of mineral-rich magma
 * to its molten buffer (at most 16 blocks), then pours an ingot at a time out of the front, 3.875 blocks from the core,
 * into a foundry channel or mold up to 6 blocks below (never spilling).
 *
 * <p>Not ported: 1.7's empty fluid-tank array ("TODO: probably to handle cooling fluids, remove me if we don't do
 * that"), which carried nothing.
 */
public class TileEntityMachineMagma extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    /** Old {@code AuxParticlePacketNT "foundry"} stream: kept on screen this long after the last pour. */
    private static final int STREAM_LIFETIME = 15;

    public boolean operating;

    public long power;
    public long consumption = 10_000;
    public static final long maxPower = 1_000_000;

    /** 1.7 {@code MaterialShapes.BLOCK.q(16)}. */
    public static final int maxLiquid = MaterialShapes.BLOCK.quantity * 16;
    public List<Mats.MaterialStack> liquids = new ArrayList<>();

    public float drillSpeed;
    public float drillRotation;
    public float prevDrillRotation;

    public float lavaHeight;
    public float prevLavaHeight;

    public boolean validPosition = true;

    /** Pour-stream sync for the renderer: molten colour (-1 none), length and a server-side lifetime. */
    public int pourColor = -1;
    public float pourLen;
    private int pourTimer;

    public TileEntityMachineMagma(BlockPos pos, BlockState state) {
        super(SpaceMachinesC.MAGMA_BE.get(), pos, state);
    }

    /** 1.7 {@code defaultOutputs}: an ingot of slag and {@code QUANTUM.q(4)} of mineral-rich magma. */
    protected Mats.MaterialStack[] getOutputs() {
        SpaceMaterials.register();
        return new Mats.MaterialStack[] {
                new Mats.MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.quantity),
                new Mats.MaterialStack(SpaceMaterials.RICH_MAGMA, 4),
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMagma te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        te.step(level, state, CelestialTraits.bodyOf(level), level.getGameTime());
    }

    /** One 1.7 {@code updateEntity} on the server, for the body {@code body} at world time {@code time}. */
    public void step(Level level, BlockState state, String body, long time) {
        // baby drill, baby
        operating = canOperate(level, body);
        if (operating) {
            power -= consumption;

            int timeBetweenOutputs = 10;

            if (time % timeBetweenOutputs == 0) {
                for (Mats.MaterialStack mat : getOutputs()) {
                    int totalLiquid = 0;
                    for (Mats.MaterialStack m : liquids) totalLiquid += m.amount;

                    int toAdd = mat.amount;

                    if (totalLiquid + toAdd <= maxLiquid) {
                        addToStack(mat);
                    } else {
                        break;
                    }
                }
            }
        }

        if (pourTimer > 0) pourTimer--; else pourColor = -1;

        // pour me a drink, barkeep
        if (!liquids.isEmpty()) {
            ForgeDirection dir = MachineCore.facing(state);

            Vec3[] impact = new Vec3[1];
            BlockPos p = worldPosition;
            Mats.MaterialStack didPour = CrucibleUtil.pourFullStack(level, p.getX() + 0.5D + dir.offsetX * 3.875D, p.getY() + 1.25D,
                    p.getZ() + 0.5D + dir.offsetZ * 3.875D, 6, true, liquids, MaterialShapes.INGOT.quantity, impact);

            if (didPour != null) {
                // 1.7 sent a "foundry" particle (dir, off 0.625, base 0.625) at core + 3.875 dir, one block up; the
                // renderer draws the same stream from these fields. 1.7's impact started at (0, 0, 0) when nothing hit.
                double impactY = impact[0] != null ? impact[0].y : 0.0D;
                pourColor = didPour.material.moltenColor;
                pourLen = Math.max(1F, p.getY() + 1 - (float) (Math.ceil(impactY) - 0.875));
                pourTimer = STREAM_LIFETIME;
            }
        }

        liquids.removeIf(o -> o.amount <= 0);

        setChanged();
        networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMagma te) {
        te.prevLavaHeight = te.lavaHeight;
        te.prevDrillRotation = te.drillRotation;

        if (te.operating) {
            te.drillSpeed += 0.15F;
            if (te.drillSpeed > 15F) te.drillSpeed = 15F;

            te.lavaHeight += (level.getRandom().nextFloat() - 0.5) * 0.01;
            te.lavaHeight = (float) lerp(0.02D, te.lavaHeight, 0.9D);
        } else {
            te.drillSpeed -= 0.3F;
            if (te.drillSpeed < 0F) te.drillSpeed = 0F;

            te.lavaHeight = (float) lerp(0.02D, te.lavaHeight, 0D);
        }

        te.drillRotation += te.drillSpeed;

        if (te.drillRotation > 360F) {
            te.drillRotation -= 360F;
            te.prevDrillRotation -= 360F;
        }
    }

    /** 1.7 {@code BobMathUtil.lerp}. */
    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, conPos(worldPosition, MachineCore.facing(getBlockState())));
        return cachedPorts;
    }

    /** 1.7 {@code getConPos}: one block behind the six back-face cells. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x - dir.offsetX * 4, y - 1, z - dir.offsetZ * 4, dir),
                new DirPos(x - dir.offsetX * 4, y - 2, z - dir.offsetZ * 4, dir),
                new DirPos(x - dir.offsetX * 4 + rot.offsetX, y - 1, z - dir.offsetZ * 4 + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 4 + rot.offsetX, y - 2, z - dir.offsetZ * 4 + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 4 - rot.offsetX, y - 1, z - dir.offsetZ * 4 - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 4 - rot.offsetX, y - 2, z - dir.offsetZ * 4 - rot.offsetZ, dir),
        };
    }

    private boolean canOperate(Level level, String body) {
        // Currently only functions on Moho, so the simplest solution is acceptable (1.7)
        if (!"moho".equals(body)) return false;

        validPosition = isValidPosition(level);
        if (!validPosition) return false;

        return power >= consumption;
    }

    /** 1.7: the 3x3 four blocks below the core must be lava (any lava block, source or flowing). */
    private boolean isValidPosition(Level level) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.getBlockState(worldPosition.offset(x, -4, z)).is(Blocks.LAVA)) return false;
            }
        }
        return true;
    }

    private void addToStack(Mats.MaterialStack matStack) {
        for (Mats.MaterialStack mat : liquids) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        liquids.add(matStack.copy());
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(operating);
        buf.writeLong(power);
        buf.writeBoolean(validPosition);
        buf.writeShort(liquids.size());
        for (Mats.MaterialStack sta : liquids) {
            buf.writeInt(sta.material.id);
            buf.writeInt(sta.amount);
        }
        buf.writeInt(pourColor);
        buf.writeFloat(pourLen);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        operating = buf.readBoolean();
        power = buf.readLong();
        validPosition = buf.readBoolean();
        liquids.clear();
        int mats = buf.readShort();
        for (int i = 0; i < mats; i++) {
            Mats.MaterialStack stack = new Mats.MaterialStack(Mats.matById.get(buf.readInt()), buf.readInt());
            if (stack.material != null) liquids.add(stack);
        }
        pourColor = buf.readInt();
        pourLen = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        // 1.7 saved {id, amount} int pairs; materials are saved by name
        Mats.writeStacks(output, "liquids", liquids);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        liquids.clear();
        liquids.addAll(Mats.readStacks(input, "liquids"));
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 4, p.getY() - 3, p.getZ() - 4, p.getX() + 5, p.getY() + 3, p.getZ() + 5);
    }
}
