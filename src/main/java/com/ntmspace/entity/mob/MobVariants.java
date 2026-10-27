package com.ntmspace.entity.mob;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/** The server chooses a skin once; entity tracking and saves preserve that exact enum constant. */
final class MobVariants {
    private MobVariants() {}

    static void load(SynchedEntityData data, EntityDataAccessor<Byte> field, Enum<?>[] variants, DataIn input) {
        var name = input.getStringOr("variant", "");
        for (var variant : variants) {
            if (variant.name().equals(name)) {
                data.set(field, (byte) variant.ordinal());
                return;
            }
        }
        // Older saves kept the ordinal as "type".
        int ordinal = input.getByteOr("type", data.get(field));
        if (ordinal >= 0 && ordinal < variants.length) data.set(field, (byte) ordinal);
    }

    static void save(SynchedEntityData data, EntityDataAccessor<Byte> field, Enum<?>[] variants, DataOut output) {
        output.putString("variant", variants[data.get(field)].name());
    }
}
