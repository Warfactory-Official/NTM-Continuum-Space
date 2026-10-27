package com.ntmspace.recipes.tables;

import static com.hbm.inventory.material.MaterialShapes.BLOCK;
import static com.hbm.inventory.material.MaterialShapes.INGOT;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.material.MaterialDistribution.Row;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code MatDistribution.registerDefaults}: the entries hbm-space adds. The id is the item's path or
 * {@code tag_} and the ore tag's path (amounts in 1.7 quanta: an ingot 72, a quarter block 162). Replaces
 * the former {@code gen_recipe_changes_metal.py}'s material_distribution rows.
 */
public final class MaterialDistributionTable extends RecipeSource<Row> {

    public MaterialDistributionTable() {
        super(MaterialDistribution.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        registerEntry(item("ntmspace:blade_syngas"), mat("stainless"), q(INGOT, 3));
        registerOre("c:ores/zinc", mat("zinc"), q(INGOT, 2), mat("stone"), quart());
        // Raw drops keep two thirds of the ore's metal and gangue, rounded to whole material quanta.
        registerOre("c:raw_materials/zinc", mat("zinc"), 96, mat("stone"), 108);
        registerOre("c:ores/conglomerate", mat("conglomerate"), q(INGOT, 4));
        registerEntry(item("ntmspace:stone_resource_calcium"), mat("flux"), q(INGOT, 12));
        registerEntry(item("hbm:ring_starmetal"), mat("starmetal"), q(INGOT, 4));
    }

    /** hbm-space {@code registerEntry(item, material, amount, ...)}; the id is the item's path. */
    private void registerEntry(Item key, Object... matDef) {
        register(BuiltInRegistries.ITEM.getKey(key).getPath(), new Row(Either.right(key), stacks(matDef)));
    }

    /** hbm-space {@code registerOre(oreName, material, amount, ...)}; the id is {@code tag_} and the tag path. */
    private void registerOre(String tag, Object... matDef) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, Ids.parse(tag));
        register("tag_" + key.location().getPath().replace('/', '_'), new Row(Either.left(key), stacks(matDef)));
    }

    private static List<MaterialStack> stacks(Object... matDef) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (int i = 0; i < matDef.length; i += 2) {
            stacks.add(new MaterialStack((NTMMaterial) matDef[i], (int) matDef[i + 1]));
        }
        return stacks;
    }

    private static NTMMaterial mat(String name) {
        return Mats.matByName.get(name);
    }

    /** hbm-space {@code MaterialShapes.q(amount)}. */
    private static int q(MaterialShapes shape, int amount) {
        return shape.quantity * amount;
    }

    /** hbm-space {@code QUART.q(1)}: a quarter block. */
    private static int quart() {
        return BLOCK.quantity / 4;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
