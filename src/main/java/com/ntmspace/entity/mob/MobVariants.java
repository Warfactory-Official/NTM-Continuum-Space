package com.ntmspace.entity.mob;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/** The server chooses a skin once; entity tracking and saves preserve that exact enum constant. */
final class MobVariants {
    private MobVariants() {}

    static void load(SynchedEntityData data, EntityDataAccessor<Byte> field, Enum<?>[] variants, DataIn input) {
        var ordinal = input.getByteOr("type", data.get(field));
        data.set(field, (byte) variants[ordinal].ordinal());
    }

    static void save(SynchedEntityData data, EntityDataAccessor<Byte> field, DataOut output) {
        output.putByte("type", data.get(field));
    }
}
