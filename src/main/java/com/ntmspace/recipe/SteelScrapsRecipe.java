package com.ntmspace.recipe;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code PowderRecipes}: iron dust and {@code ANY_COAL_COKE.dust()} mix into steel scraps, one of each for an ingot
 * or four of each for four. HBM's own recipe takes coal dust only. In Java because a scraps stack carries its material.
 */
public final class SteelScrapsRecipe extends CompatCustomRecipe {

    public static final String PATH = "crafting/steel_scraps_coke";

    public static final SteelScrapsRecipe INSTANCE = new SteelScrapsRecipe();
    public static final RecipeSerializer<SteelScrapsRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(() -> Registries.RECIPE_SERIALIZER, NtmSpace.MOD_ID);
    public static final RegistryObject<RecipeSerializer<SteelScrapsRecipe>> TYPE =
            SERIALIZERS.register("steel_scraps_coke", () -> SERIALIZER);

    private static final class Dusts {
        static final TagKey<Item> IRON = TagKey.create(Registries.ITEM, Ids.parse("c:dusts/iron"));
        static final TagKey<Item> COAL_COKE = TagKey.create(Registries.ITEM, Ids.parse("c:dusts/any_coal_coke"));
    }

    private SteelScrapsRecipe() {
        super(NtmSpace.MOD_ID, PATH);
    }

    public static void init() {}

    /** The ingots the grid makes: 1 or 4, or 0 when it is not this recipe. */
    private static int ingots(List<ItemStack> input) {
        int iron = 0;
        int fuel = 0;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            if (stack.is(Dusts.IRON)) iron++;
            else if (stack.is(Dusts.COAL_COKE)) fuel++;
            else return 0;
        }
        return iron == fuel && (iron == 1 || iron == 4) ? iron : 0;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        return ingots(input) > 0;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        int ingots = ingots(input);
        if (ingots == 0) return ItemStack.EMPTY;
        return ItemScraps.create(new Mats.MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * ingots));
    }

    @Override
    protected RecipeSerializer<SteelScrapsRecipe> serializer() {
        return SERIALIZER;
    }
}
