package com.ntmspace.fork.machine;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.world.gen.nbt.INBTTileEntityTransformable;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code TileEntityChungus} damaged state over HBM's Leviathan: a damaged turbine (blown open, or placed by
 * an NBT structure) does nothing but sync; it is repaired with a blowtorch and six welded steel plates, eight steel
 * pipes, eight resistant-alloy ingots, 24 dense gold wires and a beryllium flywheel. Saved as {@code damaged}.
 *
 * <p>Not ported, on purpose: hbm-space's turbine body and lever are the pre-{@code TileEntityTurbineBase} upstream code
 * (the lever cycles the steam tier without the "operational" check); that is the fork lagging behind 1.7, not
 * NTM: Space content, so HBM's turbine logic stays.
 */
public class TileEntityChungusSpace extends TileEntityChungus implements DamagedMachines.Damageable, INBTTileEntityTransformable {

    public boolean damaged;
    private @Nullable Object lastExplosion;
    private final List<AStack> repair = new ArrayList<>();

    public TileEntityChungusSpace(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChungusSpace turbine) {
        if (turbine.damaged) {
            turbine.powerBuffer = 0;
            turbine.networkPackNT();
            return;
        }
        TileEntityChungus.serverTick(level, pos, state, turbine);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityChungusSpace turbine) {
        TileEntityChungus.clientTick(level, pos, state, turbine);
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
        repair.add(new TagStack(tag("plates_welded/steel"), 6));
        repair.add(new TagStack(tag("pipes/steel"), 8));
        repair.add(new TagStack(tag("ingots/any_resistant_alloy"), 8));
        repair.add(new TagStack(tag("wires_dense/gold"), 24));
        repair.add(new ComparableStack(ModItems.flywheel_beryllium.get(), 1));
        return repair;
    }

    static TagKey<net.minecraft.world.item.Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Ids.of("c", path));
    }

    /** 1.7: a turbine an NBT structure places is a wreck. */
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
