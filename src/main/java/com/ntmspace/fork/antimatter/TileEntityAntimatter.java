package com.ntmspace.fork.antimatter;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.ModItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.registry.SpaceItems;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space {@code TileEntityAntimatter}: eight slots (the menu shows five): lead particles in 0 and 1, the antimatter
 * pellet (the "core") in 2, hafnium ingots in 3 and 4. Once loaded the start button runs the countdown (default 15
 * minutes, the Balefire Bomb's ping every second); at zero, or on any trigger, it clears itself, removes the block and
 * spawns a 150-block antimatter balefire with the rainbow cloud ({@code startFacAnti(..., 1000)}, five blocks up).
 * Removing a part while counting stops the countdown but keeps the timer.
 */
public class TileEntityAntimatter extends TileEntityNukeBomb implements IControlReceiver {

    public boolean loaded;
    public boolean started;
    public int timer = 18000;

    public TileEntityAntimatter(BlockPos pos, BlockState state) {
        super(ForkContent.NUKE_ANTIMATTER_BE.get(), pos, state, 8);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityAntimatter bomb) {
        bomb.updateEntity();
    }

    /** hbm-space {@code updateEntity}. */
    public void updateEntity() {
        if (level == null || level.isClientSide()) return;

        this.loaded = isLoaded();
        if (!loaded) started = false;

        if (started) {
            timer--;
            if (timer % 20 == 0) {
                level.playSound(null, worldPosition, ModSounds.FSTBMB_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            }
        }

        if (timer <= 0) {
            explode();
            return;
        }

        // 1.7: networkPackNT(250)
        networkPackNT();
    }

    /** hbm-space {@code handleButtonPacket}: meta 0 starts (when loaded), meta 1 sets the timer in seconds. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 0 && isLoaded()) {
            if (level != null) {
                level.playSound(null, worldPosition, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            }
            started = true;
        }
        if (meta == 1) timer = value * 20;
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "start", false)) handleButtonPacket(0, 0);
        if (data.contains("timer")) handleButtonPacket(Mth.clamp(Tags.getIntOr(data, "timer", 1), 1, 999), 1);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerNukeAntimatter menu && menu.getBomb() == this;
    }

    public boolean isLoaded() {
        return hasAmatCore() && hasRest();
    }

    @Override
    public boolean isReady() {
        return isLoaded();
    }

    public boolean hasAmatCore() {
        return is(2, ModSingularityItems.pellet_antimatter.get());
    }

    public boolean hasRest() {
        return getRest() > 0;
    }

    /** 1 when both lead particles and both hafnium ingots sit in their slots. */
    public int getRest() {
        return is(0, ModItems.particle_lead.get()) && is(1, ModItems.particle_lead.get())
                && is(3, SpaceItems.INGOT_HAFNIUM.get()) && is(4, SpaceItems.INGOT_HAFNIUM.get()) ? 1 : 0;
    }

    private boolean is(int slot, Item item) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getItem() == item;
    }

    /** hbm-space {@code explode}. */
    public void explode() {
        if (level == null || level.isClientSide()) return;
        clearSlots();
        level.destroyBlock(worldPosition, false);

        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        AntimatterBalefire.spawn(level, x, y, z, 150);
        AntimatterBalefire.cloud(level, x, worldPosition.getY() + 5, z, 1000);
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        return mins.length() == 1 ? "0" + mins : mins;
    }

    public String getSeconds() {
        String secs = "" + ((timer / 20) % 60);
        return secs.length() == 1 ? "0" + secs : secs;
    }

    @Override
    public String getDefaultName() {
        return "container.ntmspace.nuke_antimatter";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeAntimatter(containerId, inventory, this);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("started", started);
        output.putInt("timer", timer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        started = input.getBooleanOr("started", false);
        timer = input.getIntOr("timer", 18000);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeInt(timer);
        buffer.writeBoolean(started);
        buffer.writeBoolean(loaded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        timer = buffer.readInt();
        started = buffer.readBoolean();
        loaded = buffer.readBoolean();
    }
}
