package com.ntmspace.probe;

import static com.ntmspace.probe.ProbeHookGameTests.expect;
import static com.ntmspace.probe.ProbeHookGameTests.finish;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Two addons in one registry, one data map, one fluid, one recipe table, one dimension list (scratchpad
 * m2/probe-hook-families.md): both contributions are there, in the documented order, and a second claim on what one
 * already owns is refused or loses by the documented rule.
 */
public final class ProbeRegistryGameTests {

    private ProbeRegistryGameTests() {}

    public static void init() {}

    public static final GameTests.Function NAMED = ProbeGameTests.TESTS.function("registries_both_addons", 20,
            () -> ProbeRegistryGameTests::named);
    public static final GameTests.Function FLUIDS = ProbeGameTests.TESTS.function("fluids_traits_patches_both", 20,
            () -> ProbeRegistryGameTests::fluids);
    public static final GameTests.Function DATA = ProbeGameTests.TESTS.function("data_maps_recipes_fuels_both", 20,
            () -> ProbeRegistryGameTests::data);
    public static final GameTests.Function WORLD = ProbeGameTests.TESTS.function("world_types_dimensions_both", 20,
            () -> ProbeRegistryGameTests::world);
    public static final GameTests.Function SUBSTITUTIONS = ProbeGameTests.TESTS.function("substitutions_switches_both", 20,
            () -> ProbeRegistryGameTests::substitutions);

    // ---------------------------------------------------------------------------------------------- registries

    /** Both names present, NTM: Space's registered first, and a second registration of NTM: Space's name refused. */
    private static <T> void named(List<String> errors, String kind, NamedRegistry<T> registry, String space, String probe) {
        T own = registry.get(space);
        List<T> values = registry.values();
        if (own == null || registry.get(probe) == null) {
            errors.add(kind + ": " + space + "=" + own + ", " + probe + "=" + registry.get(probe));
            return;
        }
        if (values.indexOf(own) >= values.indexOf(registry.get(probe))) errors.add(kind + ": the probe's entry precedes NTM: Space's");
        try {
            registry.register(own);
            errors.add(kind + ": " + space + " registered twice without an error");
        } catch (IllegalStateException expected) {
            // the documented rule: a duplicate name is a start-up error
        }
    }

    private static void refused(List<String> errors, String what, Runnable registration) {
        try {
            registration.run();
            errors.add(what + " was accepted twice");
        } catch (IllegalStateException | IllegalArgumentException expected) {
            // refused, as documented
        }
    }

    private static void named(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        named(errors, "satellite type", com.hbm.saveddata.satellites.SatelliteType.REGISTRY, "ntmspace:dyson_relay", ProbeRecords.NAME);
        named(errors, "bomber payload", com.hbm.entity.logic.BomberPayload.REGISTRY, "ntmspace:airliner", ProbeRecords.NAME);
        named(errors, "bomblet kind", com.hbm.entity.projectile.EntityBombletZeta.KINDS, "ntmspace:jet", ProbeRecords.NAME);
        named(errors, "warhead type", com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.REGISTRY, "ntmspace:apollo", ProbeRecords.NAME);
        named(errors, "missile fuel", com.hbm.items.weapon.ItemCustomMissilePart.FuelType.REGISTRY, "ntmspace:hydrazine", ProbeRecords.NAME);
        named(errors, "FEL wavelength", com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.REGISTRY, "ntmspace:xray", "ntmprobe:far_uv");

        // the two spectra interleave by strength (NTM: Space moved GAMMA and DRX up for its X-ray; the probe's sits below)
        var w = com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.class;
        double uv = com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.UV.strength();
        double probe = ProbeRecords.WAVELENGTH.strength();
        double xray = com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.byName("ntmspace:xray").strength();
        double gamma = com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.GAMMA.strength();
        expect(errors, uv < probe && probe < xray && xray < gamma && gamma == 5D, w.getSimpleName() + " strengths " + uv + " < "
                + probe + " < " + xray + " < " + gamma);

        // name-keyed tables of their own kind
        expect(errors, com.hbm.items.machine.ItemCassette.TrackType.byName("ntmspace:DISEMBODIED") != null
                && com.hbm.items.machine.ItemCassette.TrackType.byName("ntmprobe:PROBE") == ProbeRecords.TRACK, "siren tracks");
        refused(errors, "siren track ntmspace:DISEMBODIED", () -> com.hbm.items.machine.ItemCassette.TrackType.register(
                "ntmspace:DISEMBODIED", "x", null, com.hbm.items.machine.ItemCassette.SoundType.LOOP, 0, 1));
        expect(errors, com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc.values().stream().anyMatch(f -> f.name().equals("SLOW_LINEAR"))
                && com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc.values().contains(ProbeRecords.BURN), "RBMK burn functions");
        refused(errors, "RBMK burn function SLOW_LINEAR", () -> com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc.register(
                "SLOW_LINEAR", "x", null, (a, b, c, d) -> 0));
        expect(errors, com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("CF_SLOPE")).result().isPresent()
                && com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("ntmprobe:FLAT")).result().orElse(null) == ProbeRecords.DEPLETE, "RBMK depletion functions");
        refused(errors, "RBMK depletion function CF_SLOPE", () -> com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc.register("CF_SLOPE", e -> e));
        expect(errors, com.hbm.inventory.material.Mats.matByName.containsKey("rich_magma")
                && com.hbm.inventory.material.Mats.matByName.get("probium") == ProbeRecords.MATERIAL, "materials");
        refused(errors, "material rich_magma", () -> com.hbm.inventory.material.Mats.register(ProbeRecords.MATERIAL_ID + 1, "rich_magma"));
        refused(errors, "material id " + ProbeRecords.MATERIAL_ID, () -> com.hbm.inventory.material.Mats.register(ProbeRecords.MATERIAL_ID, "probium2"));
        expect(errors, com.hbm.hazard.HazardRegistry.getType(dev.golem.core.Ids.of("ntmspace", "neutron")) != null
                && com.hbm.hazard.HazardRegistry.getType(dev.golem.core.Ids.of("ntmprobe", "probe")) == ProbeRecords.HAZARD, "hazard types");
        refused(errors, "hazard type ntmspace:neutron", () -> com.hbm.hazard.HazardRegistry.registerType(
                dev.golem.core.Ids.of("ntmspace", "neutron"), new ProbeRecords.ProbeHazard()));
        expect(errors, com.ntmspace.tileentity.dyson.TileEntityDysonLauncher.SPINLAUNCH == com.hbm.particle.helper.HbmEffectNT.byName("ntmspace:spinlaunch")
                && com.hbm.particle.helper.HbmEffectNT.byName(ProbeRecords.NAME) == ProbeRecords.PARTICLE, "HBM particle effects");
        refused(errors, "particle effect ntmspace:spinlaunch", () -> com.hbm.particle.helper.HbmEffectNT.register("ntmspace:spinlaunch"));

        // computer components: one per block entity class, both served
        var all = com.hbm.api.computer.ComputerComponents.all();
        expect(errors, all.stream().anyMatch(c -> c.type() == com.ntmspace.tileentity.machine.TileEntityMachineStardar.class)
                && all.contains(ProbeRecords.COMPONENT), "computer components: " + all.size());
        refused(errors, "computer component for the StarDar", () -> com.hbm.api.computer.ComputerComponents.register(
                com.ntmspace.tileentity.machine.TileEntityMachineStardar.class, "probe", java.util.Map.of()));

        // vanilla registries through Golem: both mods' entity types and mob effects
        expect(errors, BuiltInRegistries.MOB_EFFECT.getOptional(dev.golem.core.Ids.of("ntmspace", "nitan")).isPresent()
                && BuiltInRegistries.MOB_EFFECT.getOptional(dev.golem.core.Ids.of("ntmprobe", "probe")).isPresent(), "mob effects");
        expect(errors, BuiltInRegistries.ENTITY_TYPE.getOptional(dev.golem.core.Ids.of("ntmspace", "entity_moon_cow")).isPresent()
                && BuiltInRegistries.ENTITY_TYPE.getOptional(dev.golem.core.Ids.of("ntmprobe", "probe_cow")).isPresent(), "entity types");
        finish(helper, errors, "registries");
    }

    // ---------------------------------------------------------------------------------------------- fluids

    private static void fluids(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        FluidType space = com.ntmspace.fluid.SpaceFluids.EARTHAIR;
        FluidType probe = ProbeRecords.PROBE_FUEL;
        expect(errors, Fluids.byRegistryName(space.getRegistryName()) == space && Fluids.byRegistryName("ntmprobe:probe_fuel") == probe
                && Fluids.networkId(space) != Fluids.networkId(probe) && !probe.isForeign(), "Fluids.register: both addons' fluids");
        // the probe's property file names a trait and a flag the probe registered
        ProbeRecords.ProbeTrait marker = probe.getTrait(ProbeRecords.ProbeTrait.class);
        expect(errors, probe.temperature == 77 && marker != null && marker.level == 3 && probe.hasTrait(ProbeRecords.ProbeFlag.class),
                "probe_fuel properties: " + com.hbm.inventory.fluid.FluidProperties.write(probe));
        refused(errors, "fluid trait ntmspace:rocket", () -> com.hbm.inventory.fluid.FluidProperties.registerTrait("ntmspace:rocket",
                Object.class, json -> new Object(), trait -> new com.google.gson.JsonObject()));
        refused(errors, "fluid flag ntmspace:uk", () -> com.hbm.inventory.fluid.FluidProperties.registerFlag("ntmspace:uk", new Object()));

        // both patches of HBM's kerosene, in patch-namespace order: ntmprobe's, then ntmspace's (whose rocket trait wins)
        checkKerosene(errors, "after the datapack load");
        // the same state from the jars' bundled patches before any world, whichever addon declared first
        var loaded = com.hbm.inventory.fluid.FluidProperties.patchFiles();
        try {
            com.hbm.inventory.fluid.FluidProperties.applyBundledPatches(ProbeRecords.class, ProbeEntry.MOD_ID);
            checkKerosene(errors, "from the bundled patches");
        } finally {
            com.hbm.inventory.fluid.FluidProperties.applyPatches(loaded);
        }
        checkKerosene(errors, "after the datapack patches were put back");
        finish(helper, errors, "fluids");
    }

    private static void checkKerosene(List<String> errors, String when) {
        FluidType kerosene = Fluids.KEROSENE;
        var rocket = kerosene.getTrait(com.ntmspace.fluid.trait.FT_Rocket.class);
        var marker = kerosene.getTrait(ProbeRecords.ProbeTrait.class);
        expect(errors, rocket != null && rocket.getISP() == 308 && marker != null && marker.level == 5
                && kerosene.hasTrait(ProbeRecords.ProbeFlag.class), "kerosene " + when + ": "
                + com.hbm.inventory.fluid.FluidProperties.write(kerosene));
    }

    // ---------------------------------------------------------------------------------------------- data

    private static void data(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        // value map patches: applied after HBM's file, by patch namespace (ntmprobe, then ntmspace)
        var fuel = com.hbm.api.data.DiFurnaceDataMaps.DIFURNACE_FUEL;
        Integer coke = fuel.get(item("ntmspace", "powder_coke_coal"));
        Integer feather = fuel.get(Items.FEATHER);
        Integer briquette = fuel.get(item("hbm", "briquette_wood"));
        Integer coal = fuel.get(Items.COAL);
        expect(errors, Integer.valueOf(400).equals(coke) && Integer.valueOf(50).equals(feather) && briquette == null
                && Integer.valueOf(200).equals(coal), "hbm:difurnace_fuel: NTM: Space's coke " + coke + " (400), probe's feather "
                + feather + " (50), removed briquette " + briquette + ", HBM's coal " + coal);

        // recipe removals of both addons, and the recipes each left alone
        expect(errors, !dev.golem.core.world.Recipes.hasRecipe(level, "hbm", "chemical_plant/biogas"), "NTM: Space's removal of hbm:chemical_plant/biogas");
        expect(errors, !dev.golem.core.world.Recipes.hasRecipe(level, "hbm", "chemical_plant/cobble"), "the probe's removal of hbm:chemical_plant/cobble");
        expect(errors, !dev.golem.core.world.Recipes.hasRecipe(level, "ntmspace", "crafting/billet_bk247_from_nuggets_shapeless"),
                "the probe's removal of an NTM: Space recipe");
        expect(errors, dev.golem.core.world.Recipes.hasRecipe(level, "ntmspace", "crafting/billet_bk247_from_nuggets"),
                "an NTM: Space recipe the probe did not remove is gone");
        // one HBM table with both addons' rows
        var plant = com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE;
        plant.registerRecipes();
        expect(errors, plant.getRecipe("chem.ammonia") != null && plant.getRecipe("chem.probe_gas") != null, "chemical plant rows of both addons");
        // NTM: Space's own table with the probe's row
        var alkylation = com.ntmspace.recipe.AlkylationRecipes.INSTANCE;
        FluidType gas = Fluids.fromVanilla(BuiltInRegistries.FLUID.getOptional(dev.golem.core.Ids.of("ntmprobe", "probe_gas")).orElseThrow());
        var probeRow = alkylation.getOutput(gas);
        expect(errors, alkylation.getOutput(Fluids.AROMATICS) != null && probeRow != null && probeRow.out2().type == ProbeRecords.PROBE_FUEL,
                "ntmspace:alkylation: NTM: Space's aromatics row and the probe's row " + probeRow);

        // furnace fuels: the probe's feather beside NTM: Space's flora
        String floraId = com.ntmspace.registry.FloraBlocks.BURN_TIMES.keySet().iterator().next();
        int flora = dev.golem.core.world.Compat.burnDuration(level, new ItemStack(item(floraId)));
        int probeFuel = dev.golem.core.world.Compat.burnDuration(level, new ItemStack(Items.FEATHER));
        expect(errors, flora == com.ntmspace.registry.FloraBlocks.BURN_TIMES.get(floraId) && probeFuel == ProbeWorld.FEATHER_BURN,
                "furnace fuels: " + floraId + " " + flora + ", feather " + probeFuel);
        finish(helper, errors, "data");
    }

    private static net.minecraft.world.item.Item item(String namespace, String path) {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.of(namespace, path)).orElseThrow();
    }

    private static net.minecraft.world.item.Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse(id)).orElseThrow();
    }

    // ---------------------------------------------------------------------------------------------- world

    private static void world(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var server = helper.getLevel().getServer();
        var mun = com.ntmspace.world.SpaceDimensions.MUN;
        var probe = ProbeWorld.LEVEL;
        expect(errors, dev.golem.core.world.Dimensions.isDeclared(mun) && dev.golem.core.world.Dimensions.isDeclared(probe),
                "Dimensions.dimension: both declared");
        // the GameTest world has the vanilla levels only; the harness scenario ProbeCoexistence visits both addons' levels
        expect(errors, dev.golem.core.world.TimeOfDay.hasOwnClock(mun) && dev.golem.core.world.TimeOfDay.hasOwnClock(probe)
                && !dev.golem.core.world.TimeOfDay.hasOwnClock(net.minecraft.world.level.Level.OVERWORLD), "TimeOfDay.ownClock");
        // world types: an overworld built from either addon's parameter list is stable, vanilla's preset is not a world type
        expect(errors, stable(server, "ntmspace:martian") && stable(server, "ntmprobe:probe") && !stable(server, "minecraft:overworld"),
                "WorldTypes: martian " + stable(server, "ntmspace:martian") + ", probe " + stable(server, "ntmprobe:probe"));
        finish(helper, errors, "world");
    }

    private static boolean stable(net.minecraft.server.MinecraftServer server, String preset) {
        com.google.gson.JsonObject stem = com.google.gson.JsonParser.parseString("""
                {"type": "minecraft:overworld", "generator": {"type": "minecraft:noise", "settings": "minecraft:overworld",
                 "biome_source": {"type": "minecraft:multi_noise", "preset": "%s"}}}""".formatted(preset)).getAsJsonObject();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        var parsed = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, stem).result();
        return parsed.isPresent() && dev.golem.core.world.WorldTypes.isStableOverworld(
                net.minecraft.world.level.dimension.LevelStem.OVERWORLD, parsed.get());
    }

    // ---------------------------------------------------------------------------------------------- substitutions

    private static void substitutions(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // each addon substitutes its own HBM ids, built from HBM's registration properties
        expect(errors, item(ProbeEntry.SUBSTITUTED_ITEM) instanceof ProbeEntry.ProbeSubstitutedItem, ProbeEntry.SUBSTITUTED_ITEM + " "
                + item(ProbeEntry.SUBSTITUTED_ITEM).getClass().getName());
        expect(errors, item("hbm:oil_detector") instanceof com.ntmspace.fork.SpaceOilDetector, "hbm:oil_detector "
                + item("hbm:oil_detector").getClass().getName());
        var fan = BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse("hbm:fan")).orElseThrow();
        expect(errors, fan instanceof com.ntmspace.block.SpaceMachineFan, "hbm:fan " + fan.getClass().getName());

        // HBM's switches: the vein miner's stop predicate widened by both addons (HBM stone, NTM: Space cobblestone,
        // the probe calcite), NTM: Space's plain switches as it set them
        var stop = com.hbm.handler.ability.IToolAreaAbility.RECURSION_STONE.get();
        expect(errors, stop.test(Blocks.STONE.defaultBlockState()) && stop.test(Blocks.COBBLESTONE.defaultBlockState())
                && stop.test(Blocks.CALCITE.defaultBlockState()) && !stop.test(Blocks.DIRT.defaultBlockState()),
                "IToolAreaAbility.RECURSION_STONE is not the union of both widenings");
        expect(errors, com.hbm.tileentity.network.TileEntityCranePartitioner.inputCount == 96
                && com.hbm.blocks.multiblock.BlockMultiblockCore.wallPlacement, "NTM: Space's switches");
        finish(helper, errors, "substitutions");
    }
}
