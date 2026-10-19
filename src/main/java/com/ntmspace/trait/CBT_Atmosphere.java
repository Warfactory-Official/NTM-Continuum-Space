package com.ntmspace.trait;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * hbm-space's {@code CBT_Atmosphere}: a body's (or a sealed room's) air, as fluids with their partial pressures in
 * atmospheres. Ported member for member; saved as {@code fluids: [{type: "<fluid id>", percentage: <atm>}]} (1.7's keys,
 * the fluid by registry name instead of 1.7's numeric id).
 */
public class CBT_Atmosphere extends CelestialBodyTrait {

    public ArrayList<FluidEntry> fluids;

    public static class FluidEntry {
        public FluidType fluid;
        public double pressure;

        public FluidEntry(FluidType fluid, double percentage) {
            this.fluid = fluid;
            this.pressure = percentage;
        }
    }

    public CBT_Atmosphere() {
        fluids = new ArrayList<>();
    }

    public CBT_Atmosphere(FluidType fluid, double pressure) {
        fluids = new ArrayList<>();
        fluids.add(new FluidEntry(fluid, pressure));
    }

    public CBT_Atmosphere and(FluidType fluid, double pressure) {
        fluids.add(new FluidEntry(fluid, pressure));
        return this;
    }

    @Override
    public CBT_Atmosphere copy() {
        CBT_Atmosphere clone = new CBT_Atmosphere();
        for (FluidEntry entry : fluids) clone.fluids.add(new FluidEntry(entry.fluid, entry.pressure));
        return clone;
    }

    /** Add to the pressure by incrementing existing fluid entries. */
    public void add(FluidType fluid, double pressure) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid == fluid) {
                entry.pressure += pressure;
                return;
            }
        }
        fluids.add(new FluidEntry(fluid, pressure));
    }

    /**
     * Reduce the pressure by proportionally reducing the pressure of each entry. 1.7 quirk, kept: the entries are
     * multiplied by {@code pressure / total}, not by {@code 1 - pressure / total}.
     */
    public void reduce(double pressure) {
        double totalPressure = getPressure();
        if (pressure >= totalPressure) {
            fluids = new ArrayList<>();
            return;
        }
        double pressureRatio = pressure / totalPressure;
        for (FluidEntry entry : fluids) entry.pressure *= pressureRatio;
    }

    /** Fluid must be above at least .1 millibar to be "present". */
    public boolean hasFluid(FluidType fluid) {
        return hasFluid(fluid, 0.0001);
    }

    public boolean hasFluid(FluidType fluid, double abovePressure) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid == fluid) return entry.pressure >= abovePressure;
        }
        return false;
    }

    public boolean hasTrait(Class<?> trait) {
        return hasTrait(trait, 0.0001);
    }

    public boolean hasTrait(Class<?> trait, double abovePressure) {
        for (FluidEntry entry : fluids) {
            if (entry.pressure >= abovePressure && entry.fluid.hasTrait(trait)) return true;
        }
        return false;
    }

    /** The highest-pressure fluid (sorts the entries, as 1.7 did); NONE for an empty atmosphere. */
    public FluidType getMainFluid() {
        sortDescending();
        // 1.7 called fluids.get(0) unguarded (an exception on an empty list); NONE is what its null check meant
        return fluids.isEmpty() ? Fluids.NONE : fluids.get(0).fluid;
    }

    public void sortDescending() {
        fluids.sort((a, b) -> Double.compare(b.pressure, a.pressure));
    }

    /** FluidEntries store PARTIAL pressure; the total atmospheric pressure is their sum. */
    public double getPressure() {
        double pressure = 0;
        for (int i = 0; i < fluids.size(); i++) pressure += fluids.get(i).pressure;
        return pressure;
    }

    public double getPressure(FluidType fluid) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid == fluid) return entry.pressure;
        }
        return 0;
    }

    public List<Integer> getFluidColors() {
        List<Integer> colors = new ArrayList<>();
        for (FluidEntry entry : fluids) colors.add(entry.fluid.getColor());
        return colors;
    }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(fluids.size());
        for (FluidEntry entry : fluids) {
            buf.writeInt(Fluids.networkId(entry.fluid));
            buf.writeFloat((float) entry.pressure);
        }
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        fluids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            FluidType fluid = Fluids.byNetworkId(buf.readInt());
            double percentage = buf.readFloat();
            fluids.add(new FluidEntry(fluid, percentage));
        }
    }

    private record Saved(FluidType fluid, double percentage) {}

    static Codec<CBT_Atmosphere> codec() {
        Codec<Saved> entry = RecordCodecBuilder.create(i -> i.group(
                Fluids.CODEC.fieldOf("type").forGetter(Saved::fluid),
                Codec.DOUBLE.optionalFieldOf("percentage", 0.0D).forGetter(Saved::percentage)
        ).apply(i, Saved::new));
        return RecordCodecBuilder.create(i -> i.group(
                entry.listOf().optionalFieldOf("fluids", List.of()).forGetter(a -> {
                    List<Saved> list = new ArrayList<>();
                    for (FluidEntry e : a.fluids) list.add(new Saved(e.fluid, e.pressure));
                    return list;
                })
        ).apply(i, list -> {
            CBT_Atmosphere atmosphere = new CBT_Atmosphere();
            for (Saved saved : list) atmosphere.fluids.add(new FluidEntry(saved.fluid(), saved.percentage()));
            return atmosphere;
        }));
    }
}
