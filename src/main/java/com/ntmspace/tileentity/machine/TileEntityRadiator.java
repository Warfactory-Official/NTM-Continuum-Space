package com.ntmspace.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.registry.SpaceMachinesC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityRadiator} (Steam Radiator): HBM's condenser with 500 mB tanks, vacuum-optimised (it
 * condenses spent steam to water in a vacuum too, where a plain condenser boils it off). Each tick it converts as much
 * spent steam as the water tank has room for. It takes steam and gives water at the four corner cells, through the faces
 * behind them (1.7 {@code subscribeToAllAround} / {@code sendFluidToAll} at core - dir +/- rot, one up and one down).
 *
 * <p>Not ported: 1.7's {@code IConfigurableMachine} "radiator" entry (both tank sizes, 500 by default); HBM's
 * condensers have no {@code hbmMachines.json} entry either.
 */
public class TileEntityRadiator extends TileEntityCondenser {

    public static final int inputTankSize = 500;
    public static final int outputTankSize = 500;

    public TileEntityRadiator(BlockPos pos, BlockState state) {
        super(SpaceMachinesC.RADIATOR_BE.get(), pos, state, inputTankSize, outputTankSize);
        vacuumOptimised = true;
    }

    @Override public String getDefaultName() { return "block.ntmspace.machine_radiator"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRadiator te) {
        te.serverTickCommon(level);
    }

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, getConPos());
        return cachedPorts;
    }

    /** The four points behind the corner cells, where 1.7 subscribed for steam and sent its water. */
    @Override
    public DirPos[] getConPos() {
        return conPos(worldPosition, MachineCore.facing(getBlockState()));
    }

    /** 1.7 named the facing as each point's direction, though the points lie behind the machine; kept. */
    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x - dir.offsetX + rot.offsetX, y + 1, z - dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX + rot.offsetX, y - 1, z - dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX - rot.offsetX, y + 1, z - dir.offsetZ - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX - rot.offsetX, y - 1, z - dir.offsetZ - rot.offsetZ, dir),
        };
    }

    /** 1.7 {@code getRenderBoundingBox} (41 blocks wide, as written). */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 20, p.getY() - 1, p.getZ() - 20, p.getX() + 21, p.getY() + 1, p.getZ() + 21);
    }
}
