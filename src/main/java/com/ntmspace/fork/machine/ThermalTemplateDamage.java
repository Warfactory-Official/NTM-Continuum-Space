package com.ntmspace.fork.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Captured thermal machines are wrecks; newly authored cores without block-entity data remain intact. */
public final class ThermalTemplateDamage {
    private ThermalTemplateDamage() {}

    public static List<StructureBlockInfo> mark(List<StructureBlockInfo> source) {
        List<StructureBlockInfo> changed = null;
        for (var i = 0; i < source.size(); i++) {
            var info = source.get(i);
            if (info.nbt() == null || !(info.state().getBlock() instanceof SpaceMachineChungus
                    || info.state().getBlock() instanceof SpaceMachineCondenserPowered)) continue;
            if (changed == null) changed = new ArrayList<>(source);
            var data = info.nbt().copy();
            data.putBoolean("damaged", true);
            changed.set(i, new StructureBlockInfo(info.pos(), info.state().setValue(DamagedMachines.DAMAGED, true), data));
        }
        return changed == null ? source : changed;
    }
}
