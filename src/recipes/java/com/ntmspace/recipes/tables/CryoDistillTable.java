package com.ntmspace.recipes.tables;

import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.CryoRecipes;
import java.util.Locale;

/**
 * hbm-space {@code CryoRecipes.registerDefaults}: one row per {@code recipes.put(atmosphere, Quartet(...))}, named
 * {@code cryo.<atmosphere>}, in registration order. A row is 100 mB of the atmosphere (the amount 1.7's
 * {@code TileEntityMachineCryoDistill} takes per step) becoming the four fractions in tank order, for 20,000 HE per step,
 * one step per tick ({@code duration} 1), as {@link CryoRecipes} reads it. Replaces the authored rows of
 * {@code data/ntmspace/recipe/cryo_distill/}.
 */
public final class CryoDistillTable extends GenericTableSource<GenericRecipe> {

    /** 1.7 {@code TileEntityMachineCryoDistill}: mB of the atmosphere per step. */
    private static final int INPUT = CryoRecipes.FEED;
    /** 1.7 {@code TileEntityMachineCryoDistill}: HE per step. */
    private static final long POWER = TileEntityMachineCryoDistill.CONSUMPTION;

    public CryoDistillTable() {
        super(CryoRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.EARTHAIR,
            new FluidStack(SpaceFluids.NITROGEN, 60),
            new FluidStack(Fluids.OXYGEN, 25),
            new FluidStack(SpaceFluids.KRYPTON, 10),
            new FluidStack(Fluids.CARBONDIOXIDE, 5));

        put(SpaceFluids.TEKTOAIR,
            new FluidStack(SpaceFluids.CHLOROMETHANE, 55),
            new FluidStack(Fluids.CHLORINE, 25),
            new FluidStack(Fluids.GAS, 15),
            new FluidStack(SpaceFluids.HCL, 5));

        put(SpaceFluids.JOOLGAS,
            new FluidStack(Fluids.HELIUM3, 40),
            new FluidStack(Fluids.HYDROGEN, 30),
            new FluidStack(Fluids.CHLORINE, 20),
            new FluidStack(SpaceFluids.NEON, 10));

        put(SpaceFluids.NGAS,
            new FluidStack(Fluids.GAS, 45),
            new FluidStack(SpaceFluids.AMMONIA, 25),
            new FluidStack(Fluids.UNSATURATEDS, 15),
            new FluidStack(Fluids.XENON, 15));

        put(SpaceFluids.UGAS,
            new FluidStack(Fluids.OXYGEN, 45),
            new FluidStack(SpaceFluids.NITROGEN, 25),
            new FluidStack(SpaceFluids.METHANOL, 15),
            new FluidStack(Fluids.AROMATICS, 15));

        put(SpaceFluids.SARNUSGAS,
            new FluidStack(Fluids.HYDROGEN, 45),
            new FluidStack(Fluids.HELIUM3, 35),
            new FluidStack(Fluids.GAS, 15),
            new FluidStack(SpaceFluids.NITROGEN, 5));

        put(SpaceFluids.EVEAIR,
            new FluidStack(SpaceFluids.KMnO4, 40),
            new FluidStack(Fluids.MERCURY, 35),
            new FluidStack(Fluids.XENON, 15),
            new FluidStack(SpaceFluids.AMMONIA, 10));

        put(SpaceFluids.DUNAAIR,
            new FluidStack(Fluids.CARBONDIOXIDE, 70),
            new FluidStack(SpaceFluids.NITROGEN, 15),
            new FluidStack(SpaceFluids.ARGON, 10),
            new FluidStack(Fluids.OXYGEN, 5));
    }

    /** 1.7 {@code recipes.put(input, new Quartet<>(a, b, c, d))}. */
    private void put(FluidType input, FluidStack a, FluidStack b, FluidStack c, FluidStack d) {
        register(new GenericRecipe("cryo." + input.getName().toLowerCase(Locale.ROOT)).setup(1, POWER)
                .inputFluids(new FluidStack(input, INPUT))
                .outputFluids(a, b, c, d));
    }
}
