package com.ntmspace.rocket;

import com.ntmspace.breathing.Breathing;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.EntityRideableRocket.RocketState;
import com.ntmspace.entity.rocket.RocketEntities;
import com.ntmspace.entity.rocket.RocketInputs;
import com.ntmspace.entity.rocket.RocketStationSync;
import com.ntmspace.orbit.StationDock;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.RocketSounds;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * The rocket family's common start-up: its registries, packets and the hooks it answers for the orbit and atmosphere
 * families (the station port's craft from a held item, the nav drive, the sheltered cabin).
 */
public final class RocketContent {

    private RocketContent() {}

    public static void init() {
        RocketItems.init();
        RocketBlocks.init();
        RocketEntities.init();
        RocketSounds.init();
        RocketInputs.init();
        RocketStationSync.init();
        com.ntmspace.registry.StationLauncherBlocks.init();
        ItemVOTVdrive.registerHooks();
        com.ntmspace.registry.DriveProcessorBlocks.init();
        com.ntmspace.command.RocketCommands.init();

        // hbm-space BlockOrbitalStation: a fuelled rocket_custom, or a capsule / pod alone, docks at a free port
        StationDock.CRAFT_ITEMS.register(RocketContent::isCraftItem);
        StationDock.SPAWNERS.register((port, held) -> {
            if (held.getItem() == RocketItems.ROCKET_CUSTOM.get() && ItemCustomRocket.hasFuel(held)) {
                return spawnRocket(port, held);
            }
            if (held.getItem() == RocketItems.RP_C_20.get() || held.getItem() == RocketItems.RP_POD_20.get()) {
                return spawnRocket(port, ItemCustomRocket.build(new RocketStruct(held)));
            }
            return null;
        });

        // hbm-space EntityEffectHandler.handleOxy: the rocket's rider breathes
        Breathing.SHELTERED_VEHICLE = vehicle -> vehicle instanceof EntityRideableRocket;
    }

    static boolean isCraftItem(ItemStack held) {
        if (held.getItem() == RocketItems.ROCKET_CUSTOM.get()) return ItemCustomRocket.hasFuel(held);
        return held.getItem() == RocketItems.RP_C_20.get() || held.getItem() == RocketItems.RP_POD_20.get();
    }

    /** 1.7 {@code TileEntityOrbitalStation.spawnRocket}: the craft stands on the port, landed (a pod needing fuel). */
    public static EntityRideableRocket spawnRocket(TileEntityOrbitalStation port, ItemStack stack) {
        BlockPos pos = port.getBlockPos();
        EntityRideableRocket rocket = EntityRideableRocket.create(port.getLevel(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, stack);
        rocket.setPos(rocket.getX(), rocket.getY() - rocket.getBbHeight(), rocket.getZ());
        rocket.setState(rocket.isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
        port.getLevel().addFreshEntity(rocket);
        return rocket;
    }
}
