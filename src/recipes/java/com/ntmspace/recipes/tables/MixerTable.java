package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.KNO;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.DunaBlocks;
import com.ntmspace.registry.FloraBlocks;
import com.ntmspace.registry.LaytheTektoBlocks;
import com.ntmspace.registry.PopulateBlocks;
import com.ntmspace.registry.SpaceItems;
import com.ntmspace.registry.SpaceMachineItems;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.Ids;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code MixerRecipes.registerDefaults}: the rows hbm-space adds to or changes in HBM's table, each named
 * {@code mixer.<output>.<index in hbm-space's register(...) list>} as HBM names its rows. A changed row replaces HBM's
 * (removed by {@code recipe_removals/mixer.json}) and takes HBM's {@code order}; an added row takes the order of the HBM
 * row it follows (equal orders sort by id), both looked up from HBM's loaded table. Indices hbm-space keeps as HBM has
 * them are {@link #UNCHANGED}. Replaces the former {@code gen_recipe_changes_chem.py}'s mixer rows and the authored
 * {@code mixer.minsol.0}.
 */
public final class MixerTable extends GenericTableSource<GenericRecipe> {

    /** An index of hbm-space's list that is HBM's row unchanged: not written here. */
    private static final MixerRecipe UNCHANGED = null;

    private static final TagKey<Item> KEY_STRAWBERRY = tag("c", "crops/strawberry");

    public MixerTable() {
        super(MixerRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // hbm-space puts the ammonia row first; HBM's niter row moves to index 1
        remove("hbm:mixer/nitric_acid.0");
        register(Fluids.NITRIC_ACID, after("hbm:mixer/nitric_acid.0"),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(SpaceFluids.AMMONIA, 1000)).setStack2(new FluidStack(Fluids.WATER, 500)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.SULFURIC_ACID, 500)).setSolid(KNO.dust()));
        register(Fluids.RADIOSOLVENT, after("hbm:mixer/radiosolvent.0"), UNCHANGED,
                new MixerRecipe(5_000, 20).setStack1(new FluidStack(Fluids.REFORMGAS, 500)).setSolid(new ComparableStack(LaytheTektoBlocks.GEYSIR_CHLORIC.get().asItem())));

        // hbm-space has only the oxygen row (HBM's air row, index 0, and oxygen row, index 1, are removed)
        remove("hbm:mixer/oxyhydrogen.0");
        remove("hbm:mixer/oxyhydrogen.1");
        register("mixer.oxyhydrogen_oxygen", Fluids.OXYHYDROGEN, after("hbm:mixer/oxyhydrogen.0"),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.HYDROGEN, 500)).setStack2(new FluidStack(Fluids.OXYGEN, 500)));

        // after KEROSENE_REFORM
        register(SpaceFluids.SMILK, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(500, 50).setStack1(new FluidStack(SpaceFluids.MILK, 500)).setSolid(new TagStack(KEY_STRAWBERRY, 4)));
        register(SpaceFluids.COFFEE, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(100, 50).setStack1(new FluidStack(Fluids.WATER, 500)).setSolid(new ComparableStack(SpaceItems.POWDER_COFFEE.get(), 4)));
        register(SpaceFluids.TEA, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(200, 50).setStack1(new FluidStack(Fluids.WATER, 500)).setSolid(new ComparableStack(FloraBlocks.TEA_LEAF.get(), 2)));

        register(SpaceFluids.ELBOWGREASE, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.REFORMGAS, 500)).setStack2(new FluidStack(Fluids.SYNGAS, 500)));

        register(SpaceFluids.HTCO4, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(250, 50).setStack1(new FluidStack(Fluids.NITRIC_ACID, 500)).setSolid(new ComparableStack(ModItems.nugget_technetium.get())));

        // 1.7 ore_mineral at any meta: every body's ore_mineral (the addon's ntmspace:ores/ore_mineral tag)
        register(SpaceFluids.MINSOL, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.NITRIC_ACID, 500)).setSolid(new TagStack(tag("ntmspace", "ores/ore_mineral"), 1)));
        register(Fluids.REDMUD, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(50, 100).setStack1(new FluidStack(SpaceFluids.HCL, 1400)).setSolid(new ComparableStack(DunaBlocks.DUNA_SANDS.get().asItem(), 4)));

        register(SpaceFluids.DICYANOACETYLENE, after("hbm:mixer/kerosene_reform.0"), new MixerRecipe(750, 50).setStack1(new FluidStack(SpaceFluids.AMMONIA, 300)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 500)));

        // after PHEROMONE_M
        register(Fluids.ETHANOL, after("hbm:mixer/pheromone_m.0"), new MixerRecipe(450, 30).setStack1(new FluidStack(SpaceFluids.CHLOROETHANE, 250)).setStack2(new FluidStack(Fluids.REDMUD, 300)));

        register(SpaceFluids.SCUTTERBLOOD, after("hbm:mixer/pheromone_m.0"), new MixerRecipe(550, 50).setStack1(new FluidStack(SpaceFluids.HCL, 300)).setSolid(new ComparableStack(SpaceMobs.SCUTTERTAIL.get())));

        // after ALUMINA
        register(SpaceFluids.CONGLOMERA, after("hbm:mixer/alumina.1"), new MixerRecipe(1000, 70).setStack1(new FluidStack(SpaceFluids.AQUEOUS_NICKEL, 250)).setSolid(new ComparableStack(PopulateBlocks.STONE_RESOURCE_CONGLOMERATE.get().asItem(), 1)));
        // 1.7 NIM.dust(): the generic-nickel tag, which the pentlandite chunk joins.
        register(SpaceFluids.AQUEOUS_NICKEL, after("hbm:mixer/alumina.1"), new MixerRecipe(1000, 80).setStack1(new FluidStack(SpaceFluids.BRINE, 250)).setSolid(new TagStack(tag("c", "dusts/nickel"), 1)));
        register(SpaceFluids.AQUEOUS_COPPER, after("hbm:mixer/alumina.1"), new MixerRecipe(1000, 80).setStack1(new FluidStack(SpaceFluids.BRINE, 250)).setSolid(new ComparableStack(ModIntermediateItems.chunk(EnumChunkType.MALACHITE), 1)));

        // after PERFLUOROMETHYL
        register(SpaceFluids.VINYL, after("hbm:mixer/perfluoromethyl.0"),
                new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 250)).setSolid(new ComparableStack(LaytheTektoBlocks.PVC_LOG.get().asItem())),
                new MixerRecipe(500, 20).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 125)).setSolid(new ComparableStack(FloraBlocks.POWDER_PVC.get())),
                new MixerRecipe(500, 20).setStack1(new FluidStack(SpaceFluids.HGAS, 500)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 250)).setSolid(new ComparableStack(ModItems.powder_cadmium.get())));
        register(SpaceFluids.CBENZ, after("hbm:mixer/perfluoromethyl.0"), new MixerRecipe(250, 20).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 500)).setSolid(new ComparableStack(FloraBlocks.LEAF_PET.get(), 32)));

        register(SpaceFluids.LITHYDRO, after("hbm:mixer/perfluoromethyl.0"), new MixerRecipe(1000, 100).setStack1(new FluidStack(SpaceFluids.BRINE, 125)).setSolid(new ComparableStack(ModItems.powder_lithium.get(), 3)));
    }

    /** Rows carry the {@code order} given to {@link #register(FluidType, Integer, MixerRecipe...)}. */
    @Override
    protected boolean ordered() {
        return false;
    }

    /**
     * 1.7 {@code register(type, recipes...)}: one row per recipe, named by the output and the recipe's index, at
     * {@code order} (null: none).
     */
    private void register(FluidType type, Integer order, MixerRecipe... rec) {
        for (int i = 0; i < rec.length; i++) {
            MixerRecipe entry = rec[i];
            if (entry == UNCHANGED) continue;
            register("mixer." + type.getName().toLowerCase(Locale.ROOT) + "." + i, type, order, entry);
        }
    }

    /** One row under an id of its own (a Space replacement keeping its 1.7-descriptive name). */
    private void register(String id, FluidType type, Integer order, MixerRecipe entry) {
        GenericRecipe row = new GenericRecipe(id)
                .setup(entry.processTime, 0)
                .outputFluids(new FluidStack(type, entry.output));
        if (entry.solidInput != null) row.inputItems(entry.solidInput);
        if (entry.input1 != null && entry.input2 != null) row.inputFluids(entry.input1, entry.input2);
        else if (entry.input1 != null) row.inputFluids(entry.input1);
        else if (entry.input2 != null) row.inputFluids(entry.input2);
        if (order != null) row.order = order;
        register(row);
    }

    /** The order of the HBM row a row follows (or, for a changed row, replaces). */
    private int after(String recipeId) {
        return orderOf(recipeId);
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }

    /** 1.7 {@code MixerRecipes.MixerRecipe}, the builder 1.7's lines use. */
    private static final class MixerRecipe {
        FluidStack input1;
        FluidStack input2;
        AStack solidInput;
        final int processTime;
        final int output;

        MixerRecipe(int output, int processTime) {
            this.output = output;
            this.processTime = processTime;
        }

        MixerRecipe setStack1(FluidStack stack) { input1 = stack; return this; }
        MixerRecipe setStack2(FluidStack stack) { input2 = stack; return this; }
        MixerRecipe setSolid(AStack stack) { solidInput = stack; return this; }
    }
}
