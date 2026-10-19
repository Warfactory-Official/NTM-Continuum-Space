package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code ArcWelderRecipes.registerDefaults}: the rows hbm-space adds. Each takes the order of the HBM row it
 * follows (the welded stainless plate after the welded aluminium plate; the space rows after HBM's last, the Volcano
 * missile); the rocket fuselage rows have none (after HBM's). {@code OreDictStack(MAT.plateWelded())} and the like are
 * material stacks. Replaces the former {@code gen_recipe_changes_nuclear.py}'s {@code gen_arc_welder} and the rocket
 * follow-up's {@code rp_fuselage_20_*} rows.
 */
public final class ArcWelderTable extends RecipeSource<Row> {

    public ArcWelderTable() {
        super(ArcWelderRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        add("plate_welded_stainless", 12, new ArcWelderRecipe(item("ntmspace:plate_welded_stainless"), 250, 20_000L,
                mat("stainless", MaterialShapes.CASTPLATE, 2)));

        // the rocket fuselages (no order: after HBM's rows)
        add("rp_fuselage_20_1", Integer.MAX_VALUE, new ArcWelderRecipe(item("ntmspace:rp_f_20_1"), 100, 20_000L,
                mat("stainless", MaterialShapes.WELDEDPLATE, 1), comp("hbm:seg_20", 2), mat("titanium", MaterialShapes.SHELL, 1))); // 1 welded stainless, 1 titanium shell
        add("rp_fuselage_20_3", Integer.MAX_VALUE, new ArcWelderRecipe(item("ntmspace:rp_f_20_3"), 150, 30_000L,
                mat("stainless", MaterialShapes.WELDEDPLATE, 1), comp("ntmspace:rp_f_20_1", 1), mat("titanium", MaterialShapes.SHELL, 2))); // 2 weld stain, 3 tit shells
        add("rp_fuselage_20_6", Integer.MAX_VALUE, new ArcWelderRecipe(item("ntmspace:rp_f_20_6"), 200, 50_000L,
                mat("stainless", MaterialShapes.WELDEDPLATE, 2), comp("ntmspace:rp_f_20_3", 1), mat("titanium", MaterialShapes.SHELL, 3)));
        add("rp_fuselage_20_12", Integer.MAX_VALUE, new ArcWelderRecipe(item("ntmspace:rp_f_20_12"), 250, 60_000L, new FluidStack(Fluids.OXYGEN, 500),
                mat("stainless", MaterialShapes.WELDEDPLATE, 4), comp("ntmspace:rp_f_20_6", 1), mat("titanium", MaterialShapes.SHELL, 6)));

        // space misc, after HBM's last row (missile_volcano)
        int late = 41;
        add("insert_cmb", late, new ArcWelderRecipe(item("ntmspace:insert_cmb"), 600, 50_000L, new FluidStack(SpaceFluids.NEON, 2_000),
                tag("c:plates/combine_steel", 2), tag("c:ingots/u238", 1)));
        add("machine_xenon_thruster", late, new ArcWelderRecipe(item("ntmspace:machine_xenon_thruster"), 200, 50_000L, new FluidStack(SpaceFluids.ARGON, 1_000),
                mat("tungsten", MaterialShapes.WELDEDPLATE, 2), tag("c:plates/stainless", 6), comp("hbm:arc_electrode", 1)));
        add("circuit_avionics", late, new ArcWelderRecipe(item("ntmspace:circuit_avionics"), 250, 25_000L,
                mat("aluminium", MaterialShapes.CASTPLATE, 2), comp("ntmspace:circuit_aero", 2)));
    }

    /** hbm-space {@code ArcWelderRecipe}. */
    private record ArcWelderRecipe(Item output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {

        ArcWelderRecipe(Item output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }
    }

    /** hbm-space {@code recipes.add(recipe)}. */
    private void add(String id, int order, ArcWelderRecipe recipe) {
        register(id, new Row(List.of(recipe.ingredients()), Optional.ofNullable(recipe.fluid()),
                new ItemSpec(recipe.output(), 1, ItemData.NONE), recipe.duration(), recipe.consumption(), order));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static AStack comp(String id, int count) {
        return new ComparableStack(item(id), count);
    }

    private static AStack tag(String id, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)), count);
    }

    /** hbm-space {@code new OreDictStack(MAT.shape(), count)}: the material's item of that shape. */
    private static AStack mat(String material, MaterialShapes shape, int count) {
        return new MaterialStack(Mats.matByName.get(material), shape, count);
    }
}
