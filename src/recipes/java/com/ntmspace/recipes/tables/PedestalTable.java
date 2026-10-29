package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PedestalRecipes.Row;
import com.ntmspace.fork.SpacePedestalConditions;
import dev.golem.core.world.RecipeSource;

/**
 * Space PedestalRecipes requires NEW_MOON for the Dani ritual.
 */
public final class PedestalTable extends RecipeSource<Row> {

    public PedestalTable() {
        super(PedestalRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        var id = "hbm:pedestal/gun_light_revolver_dani";
        var base = loaded(id);
        remove(id);
        register("gun_light_revolver_dani", new Row(base.output(), base.count(), base.grid(),
                SpacePedestalConditions.NEW_MOON, base.set(), base.order()));
    }
}
