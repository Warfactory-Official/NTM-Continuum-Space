package com.ntmspace.despaceimport;

import com.hbm.deimport.steps.Steps;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.StackContext;
import com.hbm.delegacy.de7.Tables;
import com.ntmspace.entity.mob.EntityDepthSquid;
import com.ntmspace.entity.mob.EntityScrapFish;
import com.ntmspace.entity.mob.EntityScuttlecrab;
import com.ntmspace.entity.mob.EntitySifterEel;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * NTM DE: Space ({@code hbmspace}) as NTM: Space ({@code ntmspace}): the generated id tables and the fields both ports of
 * ntm-next-space save differently. Ids that keep their path need no row.
 */
public final class SpaceAddon implements DeAddons.Addon {
    static final String SOURCE = "hbmspace", TARGET = "ntmspace";
    /** DE: Space's rocket states by ordinal; NTM: Space saves the name. */
    private static final String[] ROCKET_STATES = {"AWAITING", "LAUNCHING", "LANDING", "LANDED", "TIPPING", "DOCKING",
            "UNDOCKING", "NEEDSFUEL", "TRANSFER"};
    /** DE: Space's siege tiers by registration index; NTM: Space saves the name. */
    private static final String[] SIEGE_TIERS = {"buff", "clay", "stone", "iron", "silver", "gold", "desh", "schrab", "dnt"};
    /** Mobs whose variant DE: Space saves as {@code type}, its ordinal in the same enum NTM: Space saves by name. */
    private static final Map<String, Enum<?>[]> VARIANTS = Map.of("hbmspace:entity_depthsquid", EntityDepthSquid.DepthSquid.values(),
            "hbmspace:entity_scrapfish", EntityScrapFish.ScrapFish.values(), "hbmspace:entity_scuttlecrab", EntityScuttlecrab.Scuttlecrab.values(),
            "hbmspace:entity_siftereel", EntitySifterEel.SifterEel.values());
    /** DE: Space's satellite types, which DE saves by their bare name, as NTM: Space names them. */
    static final Map<String, String> SATELLITES = Map.of("war", "ntmspace:railgun", "dyson_relay", "ntmspace:dyson_relay");
    /** Components NTM: Space spells otherwise; their values are reshaped in {@link #components}. */
    private static final Map<String, String> COMPONENTS = Map.of("hbmspace:rocket_fueled", "ntmspace:rocket_fuelled",
            "hbmspace:satellite_orbit", "ntmspace:sat_orbit", "hbmspace:transporter_links", "ntmspace:transporter_linker");

    /** Called from {@code DeSpaceImport}, which catches an HBM without these classes. */
    static void install() {
        DeAddons.register(new SpaceAddon());
        // before Sable's airships too: every step after this one records NTM: Space's dimension names
        Steps.add("sublevels", new DimensionsStep());
        // after DE's terrain and players, which a resumed import may still be converting
        Steps.add("playerfiles", new LateStep());
        Steps.add("playerfiles", new AirshipsStep());
        Steps.add("radiation", new SpaceDataStep());
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public String target() {
        return TARGET;
    }

    @Override
    public @Nullable InputStream table(String name) {
        return SpaceAddon.class.getResourceAsStream("/ntmspace_deimport/" + name + ".tsv");
    }

    @Override
    public void blockEntity(String deType, CompoundTag be, StackContext ctx, String where) {
        // DE: Space's mixin on DE's barrels; Continuum's barrel keeps the same flag
        rename(be, "hbmspace_overpressured", "pressureRetired");
        // a transporter's link, a teleporter's target: dimensions DE: Space named
        dimension(be);
        if (be.get("target") instanceof CompoundTag target) dimension(target);
        if (!deType.startsWith(SOURCE + ":")) return;
        if (be.contains("fluid", Tag.TAG_STRING)) be.putString("fluid", Tables.get().fluid(be.getString("fluid")));
        switch (be.getString("id")) {
            // a condenser, whose tanks NTM: Space names as DE's condenser mapper does
            case "ntmspace:machine_radiator" -> {
                rename(be, "water", "input");
                rename(be, "steam", "output");
            }
            default -> { }
        }
        switch (deType) {
            case "hbmspace:machine_magma" -> materials(be, "liquids", ctx, where);
            // NTM: Space finds the docked rocket again each tick
            case "hbmspace:gas_dock" -> be.remove("hasRocker");
            default -> { }
        }
    }

    @Override
    public void entity(String deId, CompoundTag tag, StackContext ctx, String where) {
        Enum<?>[] variants = VARIANTS.get(deId);
        if (variants != null && tag.contains("type", Tag.TAG_ANY_NUMERIC)) {
            int type = tag.getByte("type");
            if (type >= 0 && type < variants.length) {
                tag.putString("variant", variants[type].name());
                tag.remove("type");
            }
        }
        switch (deId) {
            case "hbmspace:entity_rideable_rocket" -> {
                if (tag.contains("state", Tag.TAG_ANY_NUMERIC)) {
                    int state = tag.getInt("state");
                    tag.putString("state", ROCKET_STATES[state >= 0 && state < ROCKET_STATES.length ? state : 0]);
                }
                if (tag.get("rocket") instanceof CompoundTag rocket) rocket(rocket);
                if (tag.get("satOrbit") instanceof CompoundTag orbit) orbit(orbit);
                if (tag.get("override") instanceof CompoundTag override) {
                    tag.putBoolean("hasOverride", true);
                    tag.putString("overrideBody", override.getString("body"));
                    tag.putInt("overrideX", override.getInt("x"));
                    tag.putInt("overrideZ", override.getInt("z"));
                    tag.remove("override");
                }
            }
            case "hbmspace:entity_meme_ufo", "hbmspace:entity_micro_ufo" -> {
                if (tag.contains("siegeTier", Tag.TAG_ANY_NUMERIC)) {
                    int tier = tag.getInt("siegeTier");
                    if (tier >= 0 && tier < SIEGE_TIERS.length) tag.putString("siegeTier", SIEGE_TIERS[tier]);
                    else ctx.park("entity_field", where, deId + " siegeTier", IntTag.valueOf(tier));
                }
            }
            default -> { }
        }
    }

    @Override
    public void living(CompoundTag tag, StackContext ctx, String where) {
        CompoundTag attachments = tag.getCompound("neoforge:attachments");
        if (attachments.get("hbmspace:nitan") instanceof CompoundTag nitan) {
            CompoundTag out = new CompoundTag();
            if (nitan.contains("nitan")) out.put("count", nitan.get("nitan"));
            if (nitan.contains("nitan_health")) out.put("health", nitan.get("nitan_health"));
            attachments.put("ntmspace:nitan", out);
            attachments.remove("hbmspace:nitan");
        }
        rename(attachments, "hbmspace:breath", "ntmspace:breath");
        for (Tag t : tag.getList("active_effects", Tag.TAG_COMPOUND)) {
            CompoundTag effect = (CompoundTag) t;
            effect.putString("id", DeAddons.rename(effect.getString("id")));
        }
        for (String key : List.copyOf(attachments.getAllKeys())) {
            if (!key.startsWith(SOURCE + ":")) continue;
            // has_warped: NTM: Space sends a Martian world's players to Duna only on their first join
            ctx.park("attachment", where, key, attachments.get(key));
            attachments.remove(key);
        }
        if (!attachments.isEmpty()) tag.put("neoforge:attachments", attachments);
        else tag.remove("neoforge:attachments");
    }

    @Override
    public void components(String deItem, CompoundTag c, StackContext ctx, String where) {
        if (c.get("hbmspace:drive") instanceof CompoundTag drive) {
            int[] approx = drive.getIntArray("approx");
            if (approx.length == 2) {
                drive.putInt("ax", approx[0]);
                drive.putInt("az", approx[1]);
            }
            drive.remove("approx");
        }
        if (c.get("hbmspace:rocket") instanceof CompoundTag rocket) rocket(rocket);
        if (c.get("hbm:tele_link") instanceof CompoundTag link && link.contains("dim", Tag.TAG_STRING))
            link.putString("dim", DeAddons.rename(link.getString("dim")));
        if (c.contains("hbmspace:rocket_fueled")) c.putBoolean("hbmspace:rocket_fueled", true);
        if (c.get("hbmspace:satellite_orbit") instanceof CompoundTag orbit) orbit(orbit);
        if (c.get("hbmspace:transporter_links") instanceof ListTag links) {
            for (Tag t : links) {
                CompoundTag link = (CompoundTag) t;
                int[] pos = link.getIntArray("pos");
                if (pos.length == 3) {
                    link.putInt("x", pos[0]);
                    link.putInt("y", pos[1]);
                    link.putInt("z", pos[2]);
                }
                link.remove("pos");
                link.putString("dimension", DeAddons.rename(link.getString("dimension")));
            }
        }
        // derived: an infinite container's last groundwater sample, which DE: Space takes again every tick
        c.remove("hbmspace:incompatible_table");
        // the complex capacitor is DE's capacitor in DE: Space; NTM: Space keeps the same state under its own name
        if (deItem.startsWith(SOURCE + ":") || deItem.startsWith(TARGET + ":")) rename(c, "hbm:capacitor_state", TARGET + ":capacitor_state");
        // DE: Space's PLSS keeps its oxygen in a fluid tank, NTM: Space's as a plain amount
        if ((deItem.equals(SOURCE + ":oxy_plss") || deItem.equals(TARGET + ":oxy_plss")) && c.contains("hbm:fluid_amount")) {
            c.putInt(TARGET + ":plss_fuel", c.getInt("hbm:fluid_amount"));
            for (String key : List.of("hbm:fluid", "hbm:fluid_amount", "hbm:fluid_pressure")) c.remove(key);
        }
        for (String key : List.copyOf(c.getAllKeys())) {
            if (!key.startsWith(SOURCE + ":")) continue;
            Tag value = c.get(key);
            c.remove(key);
            c.put(COMPONENTS.getOrDefault(key, DeAddons.rename(key)), value);
        }
    }

    /** DE: Space's per-body bedrock ore ({@code type} {@code <body>_<kind>}) is one NTM: Space item per grade, body and kind. */
    @Override
    public @Nullable String split(String kind, CompoundTag deComponents, CompoundTag components) {
        if (!kind.equals("bedrock_ore")) return null;
        CompoundTag ore = deComponents.getCompound("hbm:bedrock_ore");
        String type = ore.getString("type");
        if (type.indexOf('_') < 0) return null;
        components.remove("hbm:bedrock_ore");
        return TARGET + ":bedrock_ore_" + (ore.getString("grade").isEmpty() ? "base" : ore.getString("grade")) + "_" + type;
    }

    @Override
    public void satellite(CompoundTag sat, CompoundTag components, CompoundTag extra, StackContext ctx, String where) {
        sat.putString("type", SATELLITES.getOrDefault(sat.getString("type"), sat.getString("type")));
        if (components.get("hbmspace:satellite_orbit") instanceof CompoundTag orbit) {
            orbit(orbit);
            extra.put("ntmspace:orbit", orbit);
            components.remove("hbmspace:satellite_orbit");
        }
    }

    private static void dimension(CompoundTag tag) {
        if (tag.contains("dimension", Tag.TAG_STRING)) tag.putString("dimension", DeAddons.rename(tag.getString("dimension")));
    }

    /** DE: Space's satellite orbit as NTM: Space's: 1.7's phase offset and colour names. */
    private static void orbit(CompoundTag orbit) {
        rename(orbit, "phase", "phase_offset");
        rename(orbit, "red", "r");
        rename(orbit, "green", "g");
        rename(orbit, "blue", "b");
    }

    /** A rocket design's parts are item ids: DE's and DE: Space's renamed items become NTM: Space's parts. */
    private static void rocket(CompoundTag rocket) {
        if (rocket.contains("capsule", Tag.TAG_STRING)) rocket.putString("capsule", item(rocket.getString("capsule")));
        for (Tag t : rocket.getList("stages", Tag.TAG_COMPOUND)) {
            CompoundTag stage = (CompoundTag) t;
            for (String part : List.of("fuselage", "fins", "thruster"))
                if (stage.contains(part, Tag.TAG_STRING)) stage.putString(part, item(stage.getString(part)));
        }
    }

    /** A DE or DE: Space item id as its Continuum item; a split or parked item keeps its id. */
    private static String item(String id) {
        String rule = Tables.get().items.get(DeAddons.key(id));
        if (rule == null) return DeAddons.rename(id);
        return DeAddons.isTarget(rule) ? rule.split("\\[")[0] : id;
    }

    /** DE's {@code [material id, amount, ...]} by Continuum's material name; an unknown material is parked. */
    private static void materials(CompoundTag tag, String key, StackContext ctx, String where) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY)) return;
        int[] pairs = tag.getIntArray(key);
        ListTag out = new ListTag();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String name = Tables.get().material(pairs[i]);
            if (name == null) {
                ctx.park("material", where + " " + key, Integer.toString(pairs[i]), IntTag.valueOf(pairs[i + 1]));
                continue;
            }
            CompoundTag stack = new CompoundTag();
            stack.putString("mat", name);
            stack.putInt("amount", pairs[i + 1]);
            out.add(stack);
        }
        tag.put(key, out);
    }

    private static void rename(CompoundTag tag, String from, String to) {
        if (!tag.contains(from)) return;
        tag.put(to, tag.get(from));
        tag.remove(from);
    }
}
