package com.ntmspace.bedrock;

import static com.hbm.inventory.material.Mats.*;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemBedrockOreNew.CelestialBedrockOre}: the bedrock ore types of each celestial body, which replace
 * HBM's six {@code BedrockOreType}s everywhere hbm-space looped over them (raw ore composition, the ore slopper, the
 * density scanner and the processing chain). One primary output and one byproduct per acid, the 1.7 table verbatim
 * ("primary, sulfuric, solvent, dcm"); hbm-space's commented-out Thatmo table is not registered (Thatmo answers Kerbin,
 * as 1.7's {@code CelestialBody.getEnum} did).
 *
 * <p>Each type is an HBM {@link BedrockOreType} named {@code ntmspace:<body>_<suffix>}, registered at mod construction
 * in hbm-space's registration order, so {@link #index} is 1.7's {@code type.index} (the bedrock ore noise salt).
 * The names and suffixes ({@link #SPECS}) are plain data that a registration class may read without materials; the
 * types themselves exist after {@link #register()}.
 */
public final class CelestialBedrockOre {

    /** One row of the 1.7 table: the body, the type suffix, and the materials by their 1.7 {@code Mats} field name. */
    public record Spec(String body, String suffix, String primary, int primaryAmount, String acid, int acidAmount,
                       String solvent, int solventAmount, String rad, int radAmount) {

        /** {@code ntmspace:<body>_<suffix>}, the {@link BedrockOreType} registry name. */
        public String typeName() {
            return "ntmspace:" + body + "_" + suffix;
        }
    }

    /** hbm-space's table, in its registration order (Kerbin 4, Mun 5, Minmus 5, Duna 6, Moho 6, Dres 6, Eve 6, Ike 4, Laythe 6, Tekto 6). */
    public static final List<Spec> SPECS = List.of(
            // TODO (1.7): rebalance cryolite, Laythe gets regular aluminium for now
            s("kerbin", "light", "IRON", 18, "COPPER", 9, "CRYOLITE", 6, "SODIUM", 3),
            s("kerbin", "heavy", "TUNGSTEN", 18, "TUNGSTEN", 9, "ZINC", 6, "ZINC", 3),
            s("kerbin", "nonmetal", "COAL", 18, "LIGNITE", 9, "SULFUR", 6, "KNO", 3),
            s("kerbin", "crystal", "REDSTONE", 18, "ASBESTOS", 9, "DIAMOND", 6, "EMERALD", 3),

            s("mun", "light", "LITHIUM", 18, "IRON", 9, "SODIUM", 6, "CHLOROCALCITE", 3),
            s("mun", "heavy", "LEAD", 18, "ZINC", 9, "GOLD", 6, "BISMUTH", 3),
            s("mun", "rare", "COBALT", 18, "RAREEARTH", 9, "NEODYMIUM", 6, "STRONTIUM", 3),
            s("mun", "nonmetal", "SULFUR", 18, "FLUORITE", 9, "KNO", 6, "SILICON", 3),
            s("mun", "crystal", "QUARTZ", 18, "LAPIS", 9, "EMERALD", 6, "CINNABAR", 3),

            s("minmus", "light", "COPPER", 18, "TITANIUM", 9, "CHLOROCALCITE", 6, "COPPER", 3),
            s("minmus", "heavy", "LEAD", 18, "GOLD", 9, "TUNGSTEN", 6, "BISMUTH", 3),
            s("minmus", "rare", "ZIRCONIUM", 18, "BORON", 9, "COBALT", 6, "STRONTIUM", 3),
            s("minmus", "nonmetal", "SULFUR", 18, "KNO", 9, "FLUORITE", 6, "SILICON", 3),
            s("minmus", "crystal", "EMERALD", 18, "LAPIS", 9, "DIAMOND", 6, "EMERALD", 3),

            s("duna", "light", "IRON", 18, "NICKEL", 9, "TITANIUM", 6, "CHLOROCALCITE", 3),
            s("duna", "heavy", "BERYLLIUM", 18, "TUNGSTEN", 9, "ZINC", 6, "BISMUTH", 3),
            s("duna", "rare", "RAREEARTH", 18, "BORON", 9, "ZIRCONIUM", 6, "STRONTIUM", 3),
            s("duna", "actinide", "THORIUM", 18, "RADIUM", 9, "POLONIUM", 6, "U233", 3),
            s("duna", "nonmetal", "FLUORITE", 18, "SULFUR", 9, "SILICON", 6, "PHOSPHORUS", 3),
            s("duna", "crystal", "REDSTONE", 18, "CINNABAR", 9, "DIAMOND", 6, "MOLYSITE", 3),

            s("moho", "light", "TITANIUM", 18, "CHLOROCALCITE", 9, "NICKEL", 6, "LITHIUM", 3),
            s("moho", "heavy", "GOLD", 18, "ZINC", 9, "LEAD", 6, "BISMUTH", 3),
            s("moho", "rare", "NEODYMIUM", 18, "ZIRCONIUM", 9, "BROMINE", 6, "STRONTIUM", 3),
            s("moho", "actinide", "AUSTRALIUM", 18, "AUSTRALIUM", 9, "TASMANITE", 6, "AYERITE", 3),
            s("moho", "nonmetal", "GLOWSTONE", 18, "PHOSPHORUS", 9, "SULFUR", 6, "PHOSPHORUS_W", 3),
            s("moho", "crystal", "CINNABAR", 18, "REDSTONE", 9, "QUARTZ", 6, "MOLYSITE", 3),

            s("dres", "light", "NICKEL", 18, "TITANIUM", 9, "CADMIUM", 6, "GALLIUM", 3),
            s("dres", "heavy", "ZINC", 18, "GOLD", 9, "BISMUTH", 6, "ARSENIC", 3),
            s("dres", "rare", "TANTALIUM", 18, "LANTHANIUM", 9, "NIOBIUM", 6, "STRONTIUM", 3),
            s("dres", "actinide", "URANIUM", 18, "RADIUM", 9, "TECHNETIUM", 6, "U238", 3),
            s("dres", "nonmetal", "SILICON", 18, "SILICON", 9, "FLUORITE", 6, "FLUORITE", 3),
            s("dres", "crystal", "DIAMOND", 18, "BORAX", 9, "MOLYSITE", 6, "MOLYSITE", 3),

            s("eve", "light", "SODIUM", 18, "CHLOROCALCITE", 9, "IRON", 6, "CO60", 3),
            s("eve", "heavy", "TUNGSTEN", 18, "LEAD", 9, "ARSENIC", 6, "PB209", 3),
            s("eve", "rare", "NIOBIUM", 18, "STRONTIUM", 9, "IODINE", 6, "AU198", 3),
            s("eve", "actinide", "PLUTONIUM", 18, "POLONIUM", 9, "NEPTUNIUM", 6, "PU239", 3),
            s("eve", "schrabidic", "SCHRABIDIUM", 18, "SOLINIUM", 9, "GHIORSIUM", 6, "SCHRABIDIUM", 3),
            s("eve", "crystal", "SODALITE", 18, "MOLYSITE", 9, "DIAMOND", 6, "BORAX", 3),

            s("ike", "light", "COPPER", 18, "BAUXITE", 9, "NICKEL", 6, "SODIUM", 3),
            s("ike", "heavy", "LEAD", 18, "ZINC", 9, "GOLD", 6, "ARSENIC", 3),
            s("ike", "rare", "BORON", 18, "NEODYMIUM", 9, "STRONTIUM", 6, "LANTHANIUM", 3),
            s("ike", "hazard", "URANIUM", 18, "U238", 9, "PLUTONIUM", 6, "TECHNETIUM", 3),

            s("laythe", "light", "ALUMINIUM", 18, "TITANIUM", 9, "GALLIUM", 6, "HAFNIUM", 3),
            s("laythe", "heavy", "BERYLLIUM", 18, "TUNGSTEN", 9, "LEAD", 6, "ARSENIC", 3),
            s("laythe", "rare", "RAREEARTH", 18, "NEODYMIUM", 9, "STRONTIUM", 6, "NIOBIUM", 3),
            s("laythe", "actinide", "URANIUM", 18, "THORIUM", 9, "POLONIUM", 6, "U235", 3),
            s("laythe", "nonmetal", "CHLOROCALCITE", 18, "COAL", 9, "FLUORITE", 6, "SILICON", 3),
            s("laythe", "crystal", "ASBESTOS", 18, "SODALITE", 9, "DIAMOND", 6, "SODALITE", 3),

            s("tekto", "light", "TITANIUM", 18, "COPPER", 9, "NICKEL", 6, "LITHIUM", 3),
            s("tekto", "heavy", "BERYLLIUM", 18, "LEAD", 9, "ZINC", 6, "ZINC", 3),
            s("tekto", "rare", "BORON", 18, "RAREEARTH", 9, "TANTALIUM", 6, "BISMUTH", 3),
            s("tekto", "actinide", "URANIUM", 18, "NEPTUNIUM", 9, "RADIUM", 6, "TECHNETIUM", 3),
            s("tekto", "crystal", "EMERALD", 18, "SILICON", 9, "MOLYSITE", 6, "BORAX", 3),
            s("tekto", "plastic", "POLYMER", 18, "RUBBER", 9, "SEMTEX", 6, "PVC", 3));

    /** The bodies with a table, in registration order. */
    public static final List<String> BODIES = List.of("kerbin", "mun", "minmus", "duna", "moho", "dres", "eve", "ike",
            "laythe", "tekto");

    private static volatile List<BedrockOreType> types;
    private static final Map<String, List<BedrockOreType>> BY_BODY = new LinkedHashMap<>();
    private static final Map<BedrockOreType, Spec> SPEC_OF = new LinkedHashMap<>();

    private CelestialBedrockOre() {}

    private static Spec s(String body, String suffix, String p, int pa, String a, int aa, String so, int sa, String r, int ra) {
        return new Spec(body, suffix, p, pa, a, aa, so, sa, r, ra);
    }

    /**
     * Registers every type with HBM ({@link BedrockOreType#register}); the materials HBM lacks first. Idempotent; at mod
     * construction (from {@code BedrockOreItems.init()}).
     */
    public static synchronized List<BedrockOreType> register() {
        if (types != null) return types;
        SpaceOreMaterials.register();
        List<BedrockOreType> all = new ArrayList<>();
        for (Spec spec : SPECS) {
            BedrockOreType type = BedrockOreType.byName(spec.typeName());
            if (type == null) {
                type = BedrockOreType.register(new BedrockOreType(spec.typeName(), spec.suffix(),
                        displayName(spec.suffix()),
                        out(spec.primary(), spec.primaryAmount()), out(spec.acid(), spec.acidAmount()),
                        out(spec.solvent(), spec.solventAmount()), out(spec.rad(), spec.radAmount())));
            }
            all.add(type);
            BY_BODY.computeIfAbsent(spec.body(), b -> new ArrayList<>()).add(type);
            SPEC_OF.put(type, spec);
        }
        types = Collections.unmodifiableList(all);
        return types;
    }

    /** 1.7 {@code getAllTypes()}: every type, in registration order (the order every 1.7 recipe loop ran). */
    public static List<BedrockOreType> getAllTypes() {
        return register();
    }

    /** 1.7 {@code get(body).types}: the types of {@code body}, or null where the body has no table. */
    public static @Nullable List<BedrockOreType> get(String body) {
        register();
        List<BedrockOreType> list = BY_BODY.get(body);
        return list == null ? null : Collections.unmodifiableList(list);
    }

    /** 1.7 {@code type.index}: the global registration index, which salts the bedrock ore noise. -1 for no such type. */
    public static int index(BedrockOreType type) {
        return register().indexOf(type);
    }

    /** The registered type of a table row. */
    public static BedrockOreType typeOf(Spec spec) {
        register();
        BedrockOreType type = BedrockOreType.byName(spec.typeName());
        if (type == null) throw new IllegalStateException("Bedrock ore type " + spec.typeName() + " is not registered");
        return type;
    }

    /** The table row of a type, or null for one that is not NTM: Space's. */
    public static @Nullable Spec spec(BedrockOreType type) {
        register();
        return SPEC_OF.get(type);
    }

    /** The body a type belongs to, or null. */
    public static @Nullable String bodyOf(BedrockOreType type) {
        Spec spec = spec(type);
        return spec == null ? null : spec.body();
    }

    /** 1.7 {@code BedrockOreOutput} of a 1.7 {@code Mats} field name. */
    private static BedrockOreOutput out(String field, int amount) {
        return BedrockOreType.o(material(field), amount);
    }

    /** The material of a 1.7 {@code Mats.MAT_<field>} (HBM's, or {@link SpaceOreMaterials}' for the fork's). */
    public static NTMMaterial material(String field) {
        NTMMaterial mat = switch (field) {
            case "IRON" -> MAT_IRON;
            case "COPPER" -> MAT_COPPER;
            case "CRYOLITE" -> MAT_CRYOLITE;
            case "SODIUM" -> MAT_SODIUM;
            case "TUNGSTEN" -> MAT_TUNGSTEN;
            case "COAL" -> MAT_COAL;
            case "LIGNITE" -> MAT_LIGNITE;
            case "SULFUR" -> MAT_SULFUR;
            case "KNO" -> MAT_KNO;
            case "REDSTONE" -> MAT_REDSTONE;
            case "ASBESTOS" -> MAT_ASBESTOS;
            case "DIAMOND" -> MAT_DIAMOND;
            case "EMERALD" -> MAT_EMERALD;
            case "LITHIUM" -> MAT_LITHIUM;
            case "CHLOROCALCITE" -> MAT_CHLOROCALCITE;
            case "LEAD" -> MAT_LEAD;
            case "GOLD" -> MAT_GOLD;
            case "BISMUTH" -> MAT_BISMUTH;
            case "COBALT" -> MAT_COBALT;
            case "RAREEARTH" -> MAT_RAREEARTH;
            case "NEODYMIUM" -> MAT_NEODYMIUM;
            case "STRONTIUM" -> MAT_STRONTIUM;
            case "FLUORITE" -> MAT_FLUORITE;
            case "SILICON" -> MAT_SILICON;
            case "CINNABAR" -> MAT_CINNABAR;
            case "TITANIUM" -> MAT_TITANIUM;
            case "ZIRCONIUM" -> MAT_ZIRCONIUM;
            case "BORON" -> MAT_BORON;
            case "BERYLLIUM" -> MAT_BERYLLIUM;
            case "THORIUM" -> MAT_THORIUM;
            case "RADIUM" -> MAT_RADIUM;
            case "POLONIUM" -> MAT_POLONIUM;
            case "U233" -> MAT_U233;
            case "PHOSPHORUS" -> MAT_PHOSPHORUS;
            case "MOLYSITE" -> MAT_MOLYSITE;
            case "CADMIUM" -> MAT_CADMIUM;
            case "ARSENIC" -> MAT_ARSENIC;
            case "TANTALIUM" -> MAT_TANTALIUM;
            case "LANTHANIUM" -> MAT_LANTHANIUM;
            case "NIOBIUM" -> MAT_NIOBIUM;
            case "URANIUM" -> MAT_URANIUM;
            case "TECHNETIUM" -> MAT_TECHNETIUM;
            case "U238" -> MAT_U238;
            case "BORAX" -> MAT_BORAX;
            case "CO60" -> MAT_CO60;
            case "PB209" -> MAT_PB209;
            case "AU198" -> MAT_AU198;
            case "PLUTONIUM" -> MAT_PLUTONIUM;
            case "NEPTUNIUM" -> MAT_NEPTUNIUM;
            case "PU239" -> MAT_PU239;
            case "SCHRABIDIUM" -> MAT_SCHRABIDIUM;
            case "SOLINIUM" -> MAT_SOLINIUM;
            case "GHIORSIUM" -> MAT_GHIORSIUM;
            case "SODALITE" -> MAT_SODALITE;
            case "BAUXITE" -> MAT_BAUXITE;
            case "ALUMINIUM" -> MAT_ALUMINIUM;
            case "U235" -> MAT_U235;
            case "POLYMER" -> MAT_POLYMER;
            case "RUBBER" -> MAT_RUBBER;
            case "PVC" -> MAT_PVC;
            case "NICKEL" -> SpaceOreMaterials.NICKEL;
            case "ZINC" -> SpaceOreMaterials.ZINC;
            case "GALLIUM" -> SpaceOreMaterials.GALLIUM;
            case "BROMINE" -> SpaceOreMaterials.BROMINE;
            case "IODINE" -> SpaceOreMaterials.IODINE;
            case "HAFNIUM" -> SpaceOreMaterials.HAFNIUM;
            case "LAPIS" -> SpaceOreMaterials.LAPIS;
            case "QUARTZ" -> SpaceOreMaterials.QUARTZ;
            case "GLOWSTONE" -> SpaceOreMaterials.GLOWSTONE;
            case "PHOSPHORUS_W" -> SpaceOreMaterials.WHITE_PHOSPHORUS;
            case "AUSTRALIUM" -> SpaceOreMaterials.AUSTRALIUM;
            case "TASMANITE" -> SpaceOreMaterials.TASMANITE;
            case "AYERITE" -> SpaceOreMaterials.AYERITE;
            case "SEMTEX" -> SpaceOreMaterials.SEMTEX;
            default -> null;
        };
        if (mat == null) throw new IllegalStateException("No material for 1.7 MAT_" + field);
        return mat;
    }

    /** The English type name ({@code item.bedrock_ore.type.<suffix>.name}, 1.7 en_US.lang). */
    private static String displayName(String suffix) {
        return switch (suffix) {
            case "light" -> "Light Metal";
            case "heavy" -> "Heavy Metal";
            case "rare" -> "Rare Earth";
            case "actinide" -> "Actinide";
            case "nonmetal" -> "Non-Metal";
            case "crystal" -> "Crystalline";
            case "schrabidic" -> "Schrabidic";
            case "hazard" -> "Hazardous Waste";
            case "plastic" -> "Plastic";
            default -> suffix;
        };
    }

    /**
     * 1.7 {@code getAverageColor}: the type's light (or dark) band end, the per-channel integer mean of its four outputs'
     * solid colours ("autogenerated because otherwise I'd have to manually specify like 80 different colours").
     */
    public static int averageColor(BedrockOreType type, boolean dark) {
        BedrockOreOutput[] outputs = {type.primary1, type.byproductAcid1, type.byproductSolvent1, type.byproductRad1};
        int r = 0, g = 0, b = 0;
        for (BedrockOreOutput output : outputs) {
            int color = dark ? output.material().solidColorDark : output.material().solidColorLight;
            r += color >> 16 & 255;
            g += color >> 8 & 255;
            b += color & 255;
        }
        r /= outputs.length;
        g /= outputs.length;
        b /= outputs.length;
        return r << 16 | g << 8 | b;
    }
}
