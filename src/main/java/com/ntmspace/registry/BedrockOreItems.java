package com.ntmspace.registry;

import dev.golem.core.dejson.Tags;
import com.hbm.creativetabs.HbmTab;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.ntmspace.NtmSpace;
import com.ntmspace.bedrock.CelestialBedrockOre;
import com.ntmspace.bedrock.CelestialBedrockOre.Spec;
import com.ntmspace.bedrock.ItemCelestialBedrockOre;
import com.ntmspace.bedrock.SpaceBedrockOres;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tint;
import dev.golem.core.dejson.Tints;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.ItemTag;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * hbm-space's per-body bedrock ore items (its {@code ItemBedrockOreNew}, flattened like HBM's own graded ores): one item
 * per grade and {@code CelestialBedrockOre} type, {@code ntmspace:bedrock_ore_<grade>_<body>_<suffix>}, 25 grades by 54
 * types. hbm-space's chain never makes or takes {@code PRIMARY_SECOND} (its single-primary types dropped it), so that
 * grade is not registered. Only the BASE grade is in the creative tab, as 1.7's {@code getSubItems} listed it.
 *
 * <p>The art is 1.7's: hbm-space's greyscale {@code bedrock_ore_<prefix>_<suffix>} per grade prefix, recoloured on the
 * atlas into the type's band (the mean of its four outputs' colours, {@code CelestialBedrockOre.averageColor}; the
 * sprites are computed by {@code BedrockOreClient}), multiplied by the grade tint, one trait overlay per processing step.
 *
 * <p>Also the bedrock ore fragments ({@code bedrock_ore_fragment_<material>}, HBM's {@code ItemAutogen}) of every table
 * material that HBM gives no fragment, registered here through {@code ModAutogenItems.registerAutogen} so the
 * centrifuge and electrolyser rows have them.
 */
public final class BedrockOreItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(ITEMS);

    /** The shredder's input tag: every grade of every type (1.7 {@code bedrock_ore} with the wildcard metadata). */
    public static final ItemTag BEDROCK_ORES = ItemTag.of("ntmspace:bedrock_ores");

    /** The grade prefixes hbm-space ships greyscale art for. */
    public static final String[] PREFIXES = {"base", "primary", "crumbs", "sulfuric", "solvent", "rad"};

    /** The suffixes hbm-space ships art for (1.7 {@code CelestialBedrockOreType.suffix}). */
    public static final String[] SUFFIXES = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal", "schrabidic",
            "hazard", "plastic"};

    /** (grade, type name) -> item. */
    private static final Map<String, RegistryObject<ItemBedrockOreGraded>> GRADED = new LinkedHashMap<>();

    /** Materials whose fragment this class registered (their sprites are this mod's to compute). */
    private static final List<NTMMaterial> FRAGMENTS = new ArrayList<>();

    static {
        for (Grade grade : grades()) {
            for (Spec spec : CelestialBedrockOre.SPECS) {
                Registrar.Handle<ItemBedrockOreGraded> item = REG.<ItemBedrockOreGraded>item(id(grade, spec),
                                p -> new ItemCelestialBedrockOre(p, grade, CelestialBedrockOre.typeOf(spec), spec.body()))
                        .icon(d -> model(d, grade, spec))
                        .tag(BEDROCK_ORES);
                // 1.7 getSubItems: the creative tab lists the BASE grade only, in type order
                if (grade == Grade.BASE) item.tab(HbmTab.MATERIALS);
                GRADED.put(key(grade, spec.typeName()), item);
            }
        }

        // bedrock ore fragments of the table's materials that HBM has none of (ModAutogenItems skips the rest)
        SpaceOreMaterials.register();
        Set<NTMMaterial> materials = new LinkedHashSet<>();
        for (Spec spec : CelestialBedrockOre.SPECS) {
            for (String field : new String[] {spec.primary(), spec.acid(), spec.solvent(), spec.rad()}) {
                materials.add(CelestialBedrockOre.material(field));
            }
        }
        List<String> sprites = new ArrayList<>();
        for (NTMMaterial mat : materials) {
            int before = ModAutogenItems.registeredCount();
            ModAutogenItems.registerAutogen(ITEMS, mat, MaterialShapes.FRAGMENT);
            if (ModAutogenItems.registeredCount() > before) {
                FRAGMENTS.add(mat);
                Tags.member("item", "c", MaterialShapes.FRAGMENT.tagPlural + "/" + mat.name,
                        NtmSpace.MOD_ID + ":" + ModAutogenItems.base(MaterialShapes.FRAGMENT) + "_" + mat.name);
                sprites.add(mat.name);
            }
        }
        Dejson.generatedSprites("hbm:items/bedrock_ore_fragment", "_", sprites);

        // the recoloured grade art: one permutation per body on each (prefix, suffix) greyscale sprite
        for (String prefix : PREFIXES) {
            for (String suffix : SUFFIXES) {
                List<String> bodies = new ArrayList<>();
                for (Spec spec : CelestialBedrockOre.SPECS) if (spec.suffix().equals(suffix)) bodies.add(spec.body());
                if (!bodies.isEmpty()) Dejson.generatedSprites(sprite(prefix, suffix), "_", bodies);
            }
        }
    }

    private BedrockOreItems() {}

    /** Every grade hbm-space's chain uses, in 1.7 order ({@code PRIMARY_SECOND} left out). */
    public static List<Grade> grades() {
        List<Grade> grades = new ArrayList<>();
        for (Grade grade : Grade.values()) if (grade != Grade.PRIMARY_SECOND) grades.add(grade);
        return Collections.unmodifiableList(grades);
    }

    /** {@code bedrock_ore_<grade>_<body>_<suffix>}. */
    public static String id(Grade grade, Spec spec) {
        return "bedrock_ore_" + grade.name().toLowerCase(Locale.ROOT) + "_" + spec.body() + "_" + spec.suffix();
    }

    /** The greyscale source sprite of a grade prefix and type suffix. */
    public static String sprite(String prefix, String suffix) {
        return NtmSpace.MOD_ID + ":items/bedrock_ore/" + prefix + "_" + suffix;
    }

    private static String key(Grade grade, String typeName) {
        return grade.name() + "/" + typeName;
    }

    /** The model: the type's recoloured art for the grade's prefix, tinted by the grade, then the trait overlays. */
    private static Dejson.ItemDeclaration model(Dejson.ItemDeclaration item, Grade grade, Spec spec) {
        List<String> layers = new ArrayList<>();
        List<Tint> tints = new ArrayList<>();
        layers.add(sprite(grade.prefix, spec.suffix()) + "_" + spec.body());
        tints.add(Tints.constant(0xFF000000 | grade.tint));
        for (ItemBedrockOreGraded.ProcessingTrait trait : grade.traits) {
            layers.add("hbm:items/bedrock_ore_overlay." + trait.name().toLowerCase(Locale.ROOT));
            tints.add(Tints.constant(-1));
        }
        return item.models(Models.generated(layers.toArray(new String[0]))).tints(tints.toArray(new Tint[0]));
    }

    /** 1.7 {@code ItemBedrockOreNew.make(grade, type)}'s item. */
    public static ItemBedrockOreGraded get(Grade grade, BedrockOreType type) {
        RegistryObject<ItemBedrockOreGraded> item = GRADED.get(key(grade, type.name()));
        if (item == null) throw new IllegalArgumentException("No bedrock ore " + grade + " of " + type);
        return item.get();
    }

    /** The materials whose fragment item this mod registered. */
    public static List<NTMMaterial> fragmentMaterials() {
        return Collections.unmodifiableList(FRAGMENTS);
    }

    /**
     * At mod construction: registers the types with HBM, maps every graded item into HBM's lookup
     * ({@code ModBedrockOreItems.add}, so the ore slopper pays out BASE ores), and installs the per-body raw ore field.
     */
    public static void init() {
        CelestialBedrockOre.register();
        for (Grade grade : grades()) {
            for (Spec spec : CelestialBedrockOre.SPECS) {
                ModBedrockOreItems.add(grade, CelestialBedrockOre.typeOf(spec), GRADED.get(key(grade, spec.typeName())));
            }
        }
        SpaceBedrockOres.init();
        com.ntmspace.command.BedrockOreCommands.init();
    }
}
