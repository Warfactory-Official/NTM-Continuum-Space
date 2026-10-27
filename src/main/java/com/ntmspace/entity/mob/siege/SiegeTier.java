package com.ntmspace.entity.mob.siege;

import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code SiegeTier} (the 1.7 siege tiers with hbm-space's rebalanced drops: more steel, capacitor and
 * advanced circuits, cobalt, a star metal ingot for DNT). Tiers are saved by {@link #name}, never by their index.
 */
public class SiegeTier {

    public static final SiegeTier[] tiers = new SiegeTier[100];
    private static int nextID = 0;

    public static int getLength() {
        return nextID;
    }

    public static final SiegeTier DEFAULT_BUFF = new SiegeTier(20, "buff").addDrop("hbm:ingot_steel", 1).setDR(0.2F).setDMG(2F).setLaser(0F, 0F, false);
    public static final SiegeTier CLAY = new SiegeTier(30, "clay").addDrop("hbm:ingot_steel", 1).setDR(0.2F).setDMG(3F).setLaser(0F, 0F, false);
    public static final SiegeTier STONE = new SiegeTier(40, "stone").addDrop("hbm:ingot_steel", 4).setDR(0.3F).setDT(1F).setFP().setDMG(5F).setLaser(0F, 0F, true);
    public static final SiegeTier IRON = new SiegeTier(50, "iron").addDrop("hbm:ingot_steel", 6).setDR(0.3F).setDT(2F).setFP().setDMG(7.5F).setFF().setLaser(0F, 1F, true);
    public static final SiegeTier SILVER = new SiegeTier(70, "silver").addDrop("hbm:ingot_steel", 8).setDR(0.5F).setDT(3F).setNF().setFP().setDMG(10F).setSP(0.5F).setFF().setLaser(0.01F, 1F, true);
    public static final SiegeTier GOLD = new SiegeTier(100, "gold").addDrop("hbm:ingot_steel", 8).addDrop("hbm:circuit_capacitor", 2).setDR(0.5F).setDT(5F).setNF().setFP().setDMG(15F).setSP(0.5F).setFF().setLaser(0.02F, 1.5F, true);
    public static final SiegeTier DESH = new SiegeTier(150, "desh").addDrop("hbm:ingot_steel", 16).addDrop("hbm:circuit_capacitor", 3).setDR(0.7F).setDT(7F).setNF().setFP().setDMG(25F).setSP(0.5F).setFF().setLaser(0.05F, 1.5F, true);
    public static final SiegeTier SCHRAB = new SiegeTier(250, "schrab").addDrop("hbm:ingot_steel", 32).addDrop("hbm:ingot_cobalt", 2).addDrop("hbm:circuit_advanced", 3).setDR(0.7F).setDT(10F).setNF().setFP().setDMG(50F).setSP(1F).setFF().setLaser(0.1F, 2F, true);
    public static final SiegeTier DNT = new SiegeTier(500, "dnt").addDrop("hbm:ingot_starmetal", 1).setDR(0.9F).setDT(20F).setNF().setFP().setDMG(100F).setSP(1F).setFF().setLaser(0.2F, 2F, true);

    public final int id;
    public float dt = 0F;
    public float dr = 0F;
    public float health = 20F;
    public final String name;
    public float damageMod = 1F;
    public float speedMod = 0F;
    public boolean fireProof = false;
    public boolean noFall = false;
    public boolean noFriendlyFire = false;
    /** Registry-independent declarations shared with the generated death tables. */
    public record Drop(String itemId, int count) {}

    private final List<Drop> drops = new ArrayList<>();

    public float laserBreak = 0F;
    public float laserExplosive = 0F;
    public boolean laserIncendiary = false;

    public @Nullable Consumer<LivingEntity> delegate;

    private SiegeTier(float baseHealth, String name) {
        this.id = nextID;
        SiegeTier.tiers[this.id] = this;
        nextID++;

        this.health = baseHealth;
        this.name = name;
    }

    /** The tier saved under {@code name}, or null. */
    public static @Nullable SiegeTier byName(String name) {
        for (int i = 0; i < nextID; i++) if (tiers[i].name.equals(name)) return tiers[i];
        return null;
    }

    private SiegeTier setDT(float dt) {
        this.dt = dt;
        return this;
    }

    private SiegeTier setDR(float dr) {
        this.dr = dr;
        return this;
    }

    private SiegeTier setDMG(float dmg) {
        this.damageMod = dmg;
        return this;
    }

    private SiegeTier setSP(float sp) {
        this.speedMod = sp;
        return this;
    }

    private SiegeTier setFP() {
        this.fireProof = true;
        return this;
    }

    private SiegeTier setNF() {
        this.noFall = true;
        return this;
    }

    private SiegeTier setFF() {
        this.noFriendlyFire = true;
        return this;
    }

    private SiegeTier setLaser(float breaking, float explosive, boolean incendiary) {
        this.laserBreak = breaking;
        this.laserExplosive = explosive;
        this.laserIncendiary = incendiary;
        return this;
    }

    private SiegeTier addDrop(String itemId, int count) {
        this.drops.add(new Drop(itemId, count));
        return this;
    }

    public List<Drop> lootDrops() {
        return List.copyOf(drops);
    }

    /** Fresh copies of this tier's drops, resolved only after item registration. */
    public List<ItemStack> dropItems() {
        var out = new ArrayList<ItemStack>();
        for (var drop : this.drops) out.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse(drop.itemId())), drop.count()));
        return out;
    }

    public void runDelegate(LivingEntity entity) {
        if (this.delegate != null) this.delegate.accept(entity);
    }
}
