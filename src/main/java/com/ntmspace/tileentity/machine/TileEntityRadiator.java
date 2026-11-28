package com.ntmspace.tileentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.ntmspace.registry.SpaceMachinesC;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityRadiator} (Steam Radiator): HBM's condenser with 500 mB tanks, vacuum-optimised (it
 * condenses spent steam to water in a vacuum too, where a plain condenser boils it off). Each tick it converts as much
 * spent steam as the water tank has room for. It takes steam and gives water at the four corner cells, through the faces
 * behind them (1.7 {@code getPorts}).
 *
 * <p>Both tank capacities are 1.7's {@code radiator} entry of {@code hbmMachines.json}.
 */
public class TileEntityRadiator extends TileEntityCondenser {

    public static int inputTankSize = 500;
    public static int outputTankSize = 500;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "radiator"; }

        @Override public void readIfPresent(JsonObject obj) {
            inputTankSize = IConfigurableMachine.grab(obj, "I:inputTankSize", inputTankSize);
            outputTankSize = IConfigurableMachine.grab(obj, "I:outputTankSize", outputTankSize);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:inputTankSize").value(inputTankSize);
            writer.name("I:outputTankSize").value(outputTankSize);
        }
    };

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
        if (cachedPorts == null) cachedPorts = ports(worldPosition, MachineCore.facing(getBlockState()));
        return cachedPorts;
    }

    /** 1.7 {@code getPorts}: the four corner cells, through the faces behind them. */
    public static PortDef[] ports(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new PortDef[] {
                PortDef.make(x + rot.offsetX, y + 1, z + rot.offsetZ, dir.getOpposite()),
                PortDef.make(x + rot.offsetX, y - 1, z + rot.offsetZ, dir.getOpposite()),
                PortDef.make(x - rot.offsetX, y + 1, z - rot.offsetZ, dir.getOpposite()),
                PortDef.make(x - rot.offsetX, y - 1, z - rot.offsetZ, dir.getOpposite()),
        };
    }

    public static DirPos[] conPos(BlockPos core, ForgeDirection dir) {
        return PortDef.connections(ports(core, dir));
    }

    /** 1.7 {@code getRenderBoundingBox} (41 blocks wide, as written). */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 20, p.getY() - 1, p.getZ() - 20, p.getX() + 21, p.getY() + 1, p.getZ() + 21);
    }
}
