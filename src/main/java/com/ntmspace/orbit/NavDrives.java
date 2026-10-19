package com.ntmspace.orbit;

import dev.golem.core.hook.Hook;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemVOTVdrive} as the station needs it: what a held navigation drive points at. The drive item
 * belongs to the rocket part of the port; it answers here, and the station computer and {@code /ntmstations} ask.
 *
 * <p>1.7: a drive's {@code Destination} is a {@code SolarSystem.Body} (by metadata) plus {@code x}/{@code z} in its NBT;
 * for {@code ORBIT} the {@code x}/{@code z} are a station's grid coordinates.
 */
public final class NavDrives {

    /** A drive's destination: a landable body name ({@code "duna"}) or {@link #ORBIT}, and its x/z. */
    public record Destination(String body, int x, int z) {
        public boolean isOrbit() {
            return ORBIT.equals(body);
        }
    }

    /** 1.7 {@code SolarSystem.Body.ORBIT}. */
    public static final String ORBIT = "orbit";

    /** Reads a drive. */
    @FunctionalInterface
    public interface Reader {
        /** The stack's destination, or null when the stack is no drive. */
        @Nullable Destination destination(ItemStack stack);
    }

    /** Drive readers, in order; the first non-null answer counts. */
    public static final Hook<Reader> READERS = Hook.of(listeners -> stack -> {
        for (Reader reader : listeners) {
            Destination destination = reader.destination(stack);
            if (destination != null) return destination;
        }
        return null;
    });

    /** Makes a processed drive programmed with a station's grid square (1.7 {@code /ntmstations fetch}). */
    @FunctionalInterface
    public interface StationDriveFactory {
        ItemStack create(int gridX, int gridZ, String stationName);
    }

    /** The drive factory; empty until the drive item exists. */
    public static final Hook<StationDriveFactory> FACTORIES = Hook.of(listeners -> (x, z, name) -> {
        for (StationDriveFactory factory : listeners) {
            ItemStack stack = factory.create(x, z, name);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    });

    private NavDrives() {}

    /** The destination of {@code stack} if it is a drive (1.7 {@code instanceof ItemVOTVdrive}), else null. */
    public static @Nullable Destination destination(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return READERS.invoker().destination(stack);
    }
}
