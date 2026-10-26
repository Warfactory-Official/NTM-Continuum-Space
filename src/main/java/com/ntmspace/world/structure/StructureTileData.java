package com.ntmspace.world.structure;

import com.hbm.world.gen.nbt.LegacyStructureTiles;
import dev.golem.core.data.Tags;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;

/** The tile payloads in the bundled celestial structures, read by their current block entities. */
final class StructureTileData {

    private StructureTileData() {}

    static void register() {
        for (var id : new String[]{"tileentity_stardar", "tileentity_air_vent", "tileentity_chungus", "ntmspace:plushie"}) {
            LegacyStructureTiles.registerDataMigration(id, UnaryOperator.identity());
        }
        for (var id : new String[]{"tileentity_radiator", "tileentity_condenser_powered"}) {
            LegacyStructureTiles.registerDataMigration(id, StructureTileData::condenser);
        }
    }

    private static CompoundTag condenser(CompoundTag data) {
        // 1.7 named the condenser's input "water" and its output "steam", independently of their fluid types.
        for (var suffix : new String[]{"", "_max", "_type", "_p"}) {
            for (var names : new String[][]{{"water", "input"}, {"steam", "output"}}) {
                var old = names[0] + suffix;
                if (Tags.contains(data, old)) data.put(names[1] + suffix, data.get(old));
            }
        }
        return data;
    }
}
