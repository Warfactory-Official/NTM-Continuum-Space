package com.ntmspace.fork.machine;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.hbm.world.gen.nbt.INBTTileEntityTransformable;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityCondenserPowered} damaged state over HBM's powered condenser: a damaged condenser (blown
 * open, or placed by an NBT structure) does nothing but sync; it is repaired with a blowtorch and four welded steel
 * plates, twelve steel pipes and two welded resistant-alloy plates. Saved as {@code damaged}. (Its
 * {@code vacuumOptimised = true} is HBM's already.)
 */
public class TileEntityCondenserPoweredSpace extends TileEntityCondenserPowered
        implements DamagedMachines.Damageable, INBTTileEntityTransformable {

    public boolean damaged;
    private @Nullable Object lastExplosion;
    private final List<AStack> repair = new ArrayList<>();

    public TileEntityCondenserPoweredSpace(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCondenserPoweredSpace condenser) {
        if (condenser.damaged) {
            condenser.networkPackNT();
            return;
        }
        TileEntityCondenserPowered.serverTick(level, pos, state, condenser);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCondenserPoweredSpace condenser) {
        TileEntityCondenserPowered.clientTick(level, pos, state, condenser);
    }

    @Override
    public boolean isDamaged() {
        return damaged;
    }

    @Override
    public void setDamaged(boolean damaged) {
        this.damaged = damaged;
        setChanged();
    }

    @Override
    public @Nullable Object lastExplosion() {
        return lastExplosion;
    }

    @Override
    public void lastExplosion(@Nullable Object explosion) {
        this.lastExplosion = explosion;
    }

    @Override
    public List<AStack> getRepairMaterials() {
        if (!repair.isEmpty()) return repair;
        repair.add(new TagStack(TileEntityChungusSpace.tag("plates_welded/steel"), 4));
        repair.add(new TagStack(TileEntityChungusSpace.tag("pipes/steel"), 12));
        repair.add(new TagStack(TileEntityChungusSpace.tag("plates_welded/any_resistant_alloy"), 2));
        return repair;
    }

    /** 1.7: a condenser an NBT structure places is a wreck. */
    @Override
    public void transformTE(LevelAccessor level, int coordBaseMode) {
        damaged = true;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(damaged);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        damaged = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("damaged", damaged);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        damaged = input.getBooleanOr("damaged", false);
    }

    /** 1.7 {@code IPersistentNBT.restoreData}: a damaged item places a damaged machine. */
    @Override
    protected void applyCarriedData(net.minecraft.nbt.CompoundTag data) {
        super.applyCarriedData(data);
        DamagedMachines.applyCarried(this, data);
    }
}
