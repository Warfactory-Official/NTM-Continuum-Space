package com.ntmspace.gametest;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.fluid.trait.SpaceFluidTraits;
import com.ntmspace.registry.SpaceFluidBlocks;
import dev.golem.core.gametest.GameTests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NTM: Space's fluids against hbm-space's {@code Fluids.java}. The expected table below was extracted mechanically from
 * hbm-space's source (the constructor table, the later {@code addTraits} lines and the {@code registerCalculatedFuel}
 * spreadsheet evaluated with Java's truncation and rounding), and every new fluid's live colour, temperature and complete trait set, each parameter included,
 * must equal it. Also: each is the vanilla fluid of its {@code ntmspace:} id, the containers data map holds 1.7's
 * canister and gas-tank colours, and the world fluid blocks and buckets exist with their contents.
 */
public final class SpaceFluidGameTests {

    private SpaceFluidGameTests() {
    }

    public static final GameTests.Function PROPERTIES = SpaceGameTests.TESTS.function("space_fluid_properties", 20,
            () -> SpaceFluidGameTests::properties);

    public static final GameTests.Function BLOCKS = SpaceGameTests.TESTS.function("space_fluid_blocks_and_containers",
            20,
            () -> SpaceFluidGameTests::blocksAndContainers);

    public static final GameTests.Function PATCHES = SpaceGameTests.TESTS.function("space_fluid_patches", 20,
            () -> SpaceFluidGameTests::patches);

    public static final GameTests.Function HAZARDS = SpaceGameTests.TESTS.function("space_fluid_hazards", 20,
            () -> SpaceFluidGameTests::hazards);

    public static final GameTests.Function TOOLTIPS = SpaceGameTests.TESTS.function("space_fluid_tooltips", 20,
            () -> SpaceFluidGameTests::tooltips);

    public static final GameTests.Function SKY = SpaceGameTests.TESTS.function("space_sky_owner", 20,
            () -> SpaceFluidGameTests::skyOwner);

    public static void init() {
    }

    /**
     * hbm-space's changes to HBM's own fluids ({@code Fluids.java} lines 347-436 and 843-863), applied by the patches in
     * {@code data/ntmspace/fluid_property_patch/hbm/}: each HBM fluid must carry these trait tokens (and not the ones
     * after {@code !}), next to the traits of its own HBM file.
     */
    static final Map<String, List<String>> PATCHED = new java.util.LinkedHashMap<>();

    static {
        PATCHED.put("hbm:kerosene", List.of("rocket:308:981000", "combustible:AERO:3850000"));
        PATCHED.put("hbm:hydrogen",
                List.of("rocket:380:700000", "heatable:300,1,ntmspace:superheated_hydrogen,1/PWR=1.0"));
        PATCHED.put("hbm:xenon", List.of("rocket:4200:80000"));
        // The modern Space declarations close WASTEFLUID's 1:1, 300-heat heating/cooling pair.
        PATCHED.put("hbm:wastegas", List.of("rocket:900:700000", "coolable:hbm:wastefluid:1:1:300/"));
        PATCHED.put("hbm:plasma_dt", List.of("rocket:12000:700000"));
        PATCHED.put("hbm:plasma_hd", List.of("rocket:7500:700000"));
        PATCHED.put("hbm:plasma_ht", List.of("rocket:10000:700000"));
        PATCHED.put("hbm:plasma_xm", List.of("rocket:25000:700000"));
        PATCHED.put("hbm:plasma_bf", List.of("rocket:50000:700000"));
        PATCHED.put("hbm:plasma_dh3", List.of("rocket:20000:700000"));
        PATCHED.put("hbm:kerosene_reform", List.of("rocket:321:1564000"));
        PATCHED.put("hbm:watz",
                List.of("heatable:300,1,ntmspace:gas_watz,1/PWR=1.0", "pwrmoderator:1.4", "corrosive:60"));
        PATCHED.put("hbm:wastefluid", List.of("heatable:300,1,hbm:wastegas,1/PWR=1.0", "pwrmoderator:1.2"));
        PATCHED.put("hbm:coolant",
                List.of("heatable:300,1,hbm:coolant_hot,1/BOILER=1.0,HEATEXCHANGER=1.0,ICF=1.0,PWR=1.0"));
        PATCHED.put("hbm:nitric_acid", List.of("!polluting", "corrosive:60", "liquid"));
    }

    private static void patches(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        PATCHED.forEach((id, want) -> {
            List<String> have = tokens(Fluids.byRegistryName(id));
            for (String token : want) {
                if (token.startsWith("!")) {
                    if (have.stream().anyMatch(t -> t.startsWith(token.substring(1))))
                        wrong.add(id + " still has " + token.substring(1));
                } else if (!have.contains(token)) {
                    wrong.add(id + " lacks " + token + " (has " + have + ")");
                }
            }
        });
        // no other HBM fluid is a rocket fuel (hbm-space added FT_Rocket to exactly these eleven)
        long rockets = Arrays.stream(Fluids.getAll())
                             .filter(f -> f.getNamespace().equals("hbm") && f.hasTrait(FT_Rocket.class)).count();
        if (rockets != 11) wrong.add(rockets + " HBM rocket fluids, expected 11");
        long patchFiles = com.hbm.inventory.fluid.FluidProperties.patchFiles().keySet().stream()
                                                                 .filter(k -> k.startsWith("ntmspace:hbm/")).count();
        if (patchFiles != PATCHED.size())
            wrong.add(patchFiles + " ntmspace patch files loaded, expected " + PATCHED.size());
        if (!wrong.isEmpty()) {
            helper.fail(wrong.size() + " wrong: " + wrong);
            return;
        }
        helper.succeed();
    }

    /**
     * hbm-space's {@code new FluidType(name, color, p, f, r, EnumSymbol)} diamond values for its 60 new fluids, extracted
     * mechanically from its {@code Fluids.java} ({@code NAME:p:f:r:symbol}, 1.7's spelling {@code CROYGENIC}).
     */
    static final String[] HAZARDS_SPACE = {
            "NITROGEN:1:0:0:CROYGENIC", "AMMONIA:2:0:1:ASPHYXIANT", "HYDRAZINE:2:3:2:NONE", "BLOODGAS:3:1:1:NONE",
            "EARTHAIR:0:0:0:NONE", "HCL:3:0:3:ACID", "MINSOL:3:0:3:ACID", "NEON:0:0:0:CROYGENIC",
            "ARGON:0:0:0:CROYGENIC", "KRYPTON:0:0:0:CROYGENIC", "COFFEE:0:0:0:NONE", "TEA:0:0:0:NONE",
            "HONEY:0:0:0:NONE", "MILK:0:0:0:NONE", "SMILK:0:0:0:NONE", "CCL:0:0:0:NONE", "EVEAIR:4:0:0:OXIDIZER",
            "KMnO4:4:0:0:ACID", "CHLOROMETHANE:2:4:0:NONE", "METHANOL:3:4:0:NONE", "BROMINE:2:0:1:NONE",
            "CHLOROETHANE:2:0:0:NONE", "POLYTHYLENE:1:2:0:NONE", "FLUORINE:4:4:4:OXIDIZER", "TEKTOAIR:4:2:0:OXIDIZER",
            "ELBOWGREASE:1:3:0:NONE", "NMASSTETRANOL:1:3:0:NONE", "NMASS:1:2:0:NONE", "SCUTTERBLOOD:0:0:0:NONE",
            "HTCO4:1:3:0:RADIATION", "JOOLGAS:0:0:0:ASPHYXIANT", "SARNUSGAS:0:0:0:ASPHYXIANT",
            "UGAS:0:0:0:ASPHYXIANT", "NGAS:0:0:0:ASPHYXIANT", "EMILK:0:0:0:NONE", "CMILK:0:0:0:NONE",
            "CREAM:0:0:0:NONE", "MAGMA:3:3:3:NONE", "DICYANOACETYLENE:1:2:1:NONE", "BRINE:3:3:3:NONE",
            "CONGLOMERA:0:0:2:NONE", "DUNAAIR:3:0:0:ASPHYXIANT", "SUPERHEATED_HYDROGEN:0:0:0:NONE",
            "GAS_WATZ:4:0:3:ACID", "URANIUM_BROMIDE:0:0:0:NONE", "AQUEOUS_COPPER:0:0:0:NONE",
            "COPPERSULFATE:0:0:0:NONE", "THORIUM_BROMIDE:0:0:0:NONE", "GASEOUS_URANIUM_BROMIDE:0:0:0:NONE",
            "GASEOUS_PLUTONIUM_BROMIDE:0:0:0:NONE", "GASEOUS_SCHRABIDIUM_BROMIDE:0:0:0:NONE",
            "GASEOUS_THORIUM_BROMIDE:0:0:0:NONE", "HGAS:0:0:0:ACID", "AQUEOUS_NICKEL:0:0:0:NONE", "VINYL:0:0:0:NONE",
            "TCRUDE:0:0:0:NONE", "CBENZ:0:0:0:NONE", "HALOLIGHT:0:0:0:NONE", "LITHYDRO:0:0:0:NONE",
            "LITHCARBONATE:0:0:0:NONE"
    };

    private static void hazards(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (String text : HAZARDS_SPACE) {
            String[] row = text.split(":");
            FluidType fluid = Fluids.byRegistryName("ntmspace:" + row[0].toLowerCase(java.util.Locale.ROOT));
            if (fluid == Fluids.NONE) {
                wrong.add(row[0] + " is not registered");
                continue;
            }
            var expected = new com.hbm.inventory.fluid.FluidHazard(Integer.parseInt(row[1]), Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]), com.hbm.inventory.fluid.FluidHazard.Symbol.valueOf(
                    row[4].equals("CROYGENIC") ? "CRYOGENIC" : row[4]));
            if (!fluid.getHazard().equals(expected))
                wrong.add(row[0] + " " + fluid.getHazard() + ", expected " + expected);
        }
        if (HAZARDS_SPACE.length != EXPECTED.size())
            wrong.add(HAZARDS_SPACE.length + " rows, expected " + EXPECTED.size());
        if (!wrong.isEmpty()) {
            helper.fail(wrong.size() + " wrong: " + wrong);
            return;
        }
        helper.succeed();
    }

    /**
     * hbm-space's rocket trait adds its 1.7 lines to HBM's fluid tooltip, after HBM's own traits.
     */
    private static void tooltips(GameTestHelper helper) {
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        Fluids.byRegistryName("hbm:kerosene").addInfo(lines, true);
        List<String> text = lines.stream().map(net.minecraft.network.chat.Component::getString).toList();
        int n = text.size();
        helper.assertTrue(
                n >= 4 && text.get(n - 4).equals("[Rocket Grade]") && text.get(n - 3).startsWith("Provides 308 ISP")
                        && text.get(n - 2).equals("[Thrust power]") && text.get(n - 1)
                                                                           .contains("N of thrust per bucket"),
                "kerosene's tooltip does not end with the rocket lines: " + text);
        helper.succeed();
    }

    /**
     * NTM: Space draws every body's sky (Kerbin is the overworld), so HBM's overworld sky layers are off it.
     */
    private static void skyOwner(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(com.hbm.api.environment.SkyOwnership.claimed(server.overworld()),
                "the overworld sky is not NTM: Space's");
        var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(nether == null || !com.hbm.api.environment.SkyOwnership.claimed(nether),
                "the Nether sky was claimed");
        helper.succeed();
    }

    private record Expected(String key, int color, int temperature, List<String> traits) {
    }

    private static Expected e(String key, int color, int temperature, String... traits) {
        return new Expected(key, color, temperature, sorted(Arrays.asList(traits)));
    }

    /**
     * hbm-space's 60 new fluids in its declaration order: key, colour, temperature, traits (canonical tokens).
     */
    static final List<Expected> EXPECTED = List.of(
            e("nitrogen", 0xB3C6D2, -90, "liquid", "gaseous_art"),
            e("ammonia", 0x00A0F7, 20, "poison:true:4", "gaseous", "terraformer:DUNA"),
            e("hydrazine", 0x31517D, 20, "flammable:500000", "combustible:HIGH:1250000", "corrosive:30", "liquid",
                    "rocket:210:600000"),
            e("bloodgas", 0x591000, 20, "flammable:2040000", "combustible:AERO:5120000", "liquid"),
            e("earthair", 0xD1CEEE, 20, "gaseous"),
            e("hcl", 0x00D452, 20, "corrosive:30", "liquid"),
            e("minsol", 0xFADF6A, 20, "corrosive:10", "liquid"),
            e("neon", 0xF1F600, 20, "gaseous"),
            e("argon", 0xFD70D0, 20, "gaseous"),
            e("krypton", 0x9AC6E6, 20, "gaseous"),
            e("coffee", 0x57493D, 20, "delicious", "liquid"),
            e("tea", 0x76523C, 20, "delicious", "liquid"),
            e("honey", 0xD99A02, 20, "delicious", "liquid"),
            e("milk", 0xCFCFCF, 20, "delicious", "liquid"),
            e("smilk", 0xF5DEE4, 20, "delicious", "liquid"),
            e("ccl", 0x0C3B2F, 20, "liquid", "corrosive:10"),
            e("eveair", 0xDCABF8, 20, "gaseous", "corrosive:25", "poison:true:1"),
            e("kmno4", 0x560046, 20, "liquid", "corrosive:15", "poison:true:1"),
            e("chloromethane", 0xD3CF9E, 20, "gaseous", "corrosive:15", "flammable:50000"),
            e("methanol", 0x88739F, 20, "gaseous", "flammable:375000", "combustible:HIGH:937000", "liquid"),
            e("bromine", 0xAF2214, 20, "liquid", "viscous", "corrosive:10"),
            e("chloroethane", 0xBBA9A0, 20, "gaseous"),
            e("polythylene", 0x35302E, 20, "liquid", "flammable:50000"),
            e("fluorine", 0xC5C539, 20, "gaseous", "corrosive:40", "poison:true:1", "flammable:10000"),
            e("tektoair", 0x245F46, 20, "gaseous", "poison:true:1", "flammable:30000"),
            e("elbowgrease", 0xCBC433, 20, "flammable:600000", "liquid"),
            e("nmasstetranol", 0xF1DB0F, 20, "flammable:256000000000", "liquid", "corrosive:70", "poison:true:0",
                    "ventradiation:" + 0.01F, "combustible:HIGH:2688000000000"),
            e("nmass", 0x53A9F4, 20, "liquid", "corrosive:10", "poison:true:0", "ventradiation:" + 0.04F),
            e("scutterblood", 0x6C166C, 20, "liquid", "viscous", "delicious"),
            e("htco4", 0x675454, 20, "liquid", "corrosive:10", "ventradiation:" + 0.5F),
            e("joolgas", 0x96B596, 20, "gaseous"),
            e("sarnusgas", 0xE47D5C, 20, "gaseous"),
            e("ugas", 0x718C9A, 20, "gaseous"),
            e("ngas", 0xA37BA3, 20, "gaseous"),
            e("emilk", 0xCFCFCF, 20, "delicious", "liquid"),
            e("cmilk", 0xCFCFCF, 20, "delicious", "liquid"),
            e("cream", 0xCFCFCF, 20, "delicious", "liquid"),
            e("magma", 0xFF3300, 20, "liquid", "viscous"),
            e("dicyanoacetylene", 0x675A9F, 20, "flammable:7320000", "gaseous"),
            e("brine", 0xD1A73E, 20, "liquid", "viscous"),
            e("conglomera", 0x364D47, 20, "liquid", "viscous"),
            e("dunaair", 0xD4704E, 20, "gaseous", "polluting:" + "POISON=" + 2.0000001e-05F + "/" + ""),
            e("superheated_hydrogen", 0xE39393, 2200, "gaseous", "nocontainer", "noid", "rocket:900:700000",
                    "coolable:hbm:hydrogen:1:1:300/" + ""),
            e("gas_watz", 0x86653E, 2500, "gaseous", "nocontainer", "noid",
                    "polluting:" + "POISON=" + 0.0005F + "/" + "", "rocket:1200:700000",
                    "coolable:hbm:watz:1:1:300/" + ""),
            e("uranium_bromide", 0xD1CEBE, 200, "liquid", "viscous", "corrosive:65", "ventradiation:" + 0.1F,
                    "heatable:" + "300,1,ntmspace:gaseous_uranium_bromide,1" + "/" + "PWR=" + 1.0D,
                    "pwrmoderator:" + 1.75D),
            e("aqueous_copper", 0x4CC2A2, 20, "liquid", "viscous"),
            e("coppersulfate", 0x55E5CF, 20, "liquid", "viscous"),
            e("thorium_bromide", 0x7A5542, 200, "liquid", "viscous", "corrosive:65", "ventradiation:" + 0.1F,
                    "heatable:" + "300,1,ntmspace:gaseous_thorium_bromide,1" + "/" + "PWR=" + 1.0D,
                    "pwrmoderator:" + 1.50D),
            e("gaseous_uranium_bromide", 0xD1CEBE, 2500, "gaseous", "nocontainer", "noid", "rocket:1500:700000",
                    "coolable:ntmspace:uranium_bromide:1:1:300/" + ""),
            e("gaseous_plutonium_bromide", 0x4C4C4C, 2600, "gaseous", "nocontainer", "noid", "rocket:2000:700000"),
            e("gaseous_schrabidium_bromide", 0x006B6B, 3000, "gaseous", "nocontainer", "noid", "rocket:3000:700000"),
            e("gaseous_thorium_bromide", 0x7A5542, 2300, "gaseous", "nocontainer", "noid", "rocket:1300:700000",
                    "coolable:ntmspace:thorium_bromide:1:1:300/" + ""),
            e("hgas", 0x999368, 20, "gaseous", "corrosive:120"),
            e("aqueous_nickel", 0xDACEBA, 20, "liquid"),
            e("vinyl", 0xA2A2A2, 20, "liquid"),
            e("tcrude", 0x051914, 20, "liquid"),
            e("cbenz", 0x91C6BB, 20, "liquid"),
            e("halolight", 0xB6F9CF, 20, "liquid"),
            e("lithydro", 0xD1CEBE, 20, "gaseous"),
            e("lithcarbonate", 0xD1CEBE, 20, "gaseous"));

    /**
     * The new fluids hbm-space gave containers ({@code addContainers}).
     */
    static final String[] CONTAINER_KEYS = {"hydrazine", "bloodgas", "earthair", "neon", "argon", "krypton", "eveair", "tektoair", "elbowgrease", "nmasstetranol", "joolgas", "sarnusgas", "ugas", "ngas", "dunaair"};
    /**
     * Their canister colour, gas bottle colour and gas label colour (-1: none).
     */
    static final int[][] CONTAINERS = {
            {0x31517D, -1, -1},
            {0x591000, -1, -1},
            {-1, 0xC3BEE8, 0xFFFFFF},
            {-1, 0xF1F600, 0x303030},
            {-1, 0xFD70D0, 0x303030},
            {-1, 0x9AC6E6, 0x303030},
            {-1, 0x8C21FF, 0xFFFFFF},
            {-1, 0x388C69, 0x303030},
            {0xCBC433, -1, -1},
            {0xF1DB0F, -1, -1},
            {-1, 0x78B878, 0xFF8400},
            {-1, 0xB56847, 0xFF8400},
            {-1, 0x5099BF, 0xFF8400},
            {-1, 0xBE92E0, 0xFF8400},
            {-1, 0xCF5B34, 0xFFFFFF}};

    private static void properties(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (Expected expected : EXPECTED) {
            String id = "ntmspace:" + expected.key();
            FluidType fluid = Fluids.byRegistryName(id);
            if (fluid == Fluids.NONE || !fluid.getRegistryName().equals(id) || !fluid.getNamespace()
                                                                                     .equals("ntmspace")) {
                wrong.add(id + " is not registered");
                continue;
            }
            if ((fluid.getColor() & 0xFFFFFF) != expected.color())
                wrong.add(id + " colour " + Integer.toHexString(fluid.getColor()));
            if (fluid.getTemperature() != expected.temperature())
                wrong.add(id + " temperature " + fluid.getTemperature());
            List<String> actual = tokens(fluid);
            if (!actual.equals(expected.traits()))
                wrong.add(id + " traits " + actual + ", expected " + expected.traits());
            net.minecraft.world.level.material.Fluid vanilla = Fluids.toVanilla(fluid);
            if (!id.equals(String.valueOf(BuiltInRegistries.FLUID.getKey(vanilla))) || Fluids.fromVanilla(
                    vanilla) != fluid) {
                wrong.add(id + " is not the vanilla fluid " + id);
            }
        }
        long registered = Arrays.stream(Fluids.getAll())
                                .filter(f -> f.getNamespace().equals("ntmspace") && !f.isForeign()).count();
        if (registered != EXPECTED.size())
            wrong.add(registered + " ntmspace fluids registered, expected " + EXPECTED.size());
        if (!wrong.isEmpty()) {
            helper.fail(wrong.size() + " wrong: " + wrong);
            return;
        }
        helper.succeed();
    }

    private static void blocksAndContainers(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (int i = 0; i < CONTAINER_KEYS.length; i++) {
            FluidType fluid = Fluids.byRegistryName("ntmspace:" + CONTAINER_KEYS[i]);
            var c = com.hbm.api.data.FluidDataMaps.FLUID_CONTAINERS.get(Fluids.toVanilla(fluid));
            int[] want = CONTAINERS[i];
            if (c == null || c.canister().orElse(-1) != want[0] || c.gasBottle().orElse(-1) != want[1]
                    || c.gasLabel().orElse(-1) != want[2]) {
                wrong.add(CONTAINER_KEYS[i] + " containers " + c);
            }
        }
        for (String block : new String[]{"ntmspace:mercury_block", "ntmspace:bromine_block", "ntmspace:ccl_block"}) {
            if (BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(block)).isEmpty())
                wrong.add(block + " not registered");
        }
        // 1.7 FluidContainerRegistry: the three buckets and the vanilla milk bucket
        Map<String, String> buckets = Map.of("ntmspace:bucket_mercury", "hbm:mercury", "ntmspace:bucket_bromine",
                "ntmspace:bromine",
                "ntmspace:bucket_ccl", "ntmspace:ccl", "minecraft:milk_bucket", "ntmspace:milk");
        var fixed = com.hbm.api.data.FluidDataMaps.FLUID_CONTAINER.byId();
        buckets.forEach((item, fluid) -> {
            var container = fixed.get(item);
            if (container == null || !container.fluid().equals(fluid) || container.amount() != 1000
                    || !container.empty().orElse("").equals("minecraft:bucket")) {
                wrong.add(item + " holds " + container);
            }
        });
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, SpaceFluidBlocks.MERCURY_BLOCK.get().defaultBlockState());
        if (!helper.getBlockState(pos).is(SpaceFluidBlocks.MERCURY_BLOCK.get()))
            wrong.add("mercury_block did not place");
        if (!wrong.isEmpty()) {
            helper.fail(wrong.size() + " wrong: " + wrong);
            return;
        }
        helper.succeed();
    }

    /**
     * A fluid's traits as canonical tokens (the form of {@link #EXPECTED}); a trait not known here is {@code unknown:}.
     */
    static List<String> tokens(FluidType fluid) {
        List<String> out = new ArrayList<>();
        for (Object trait : fluid.traits()) out.add(token(trait));
        return sorted(out);
    }

    private static String token(Object t) {
        if (t instanceof FluidTraitSimple.FT_Liquid) return "liquid";
        if (t instanceof FluidTraitSimple.FT_Gaseous) return "gaseous";
        if (t instanceof FluidTraitSimple.FT_Plasma) return "plasma";
        if (t instanceof FluidTraitSimple.FT_Gaseous_ART) return "gaseous_art";
        if (t instanceof FluidTraitSimple.FT_Viscous) return "viscous";
        if (t instanceof FluidTraitSimple.FT_Amat) return "amat";
        if (t instanceof FluidTraitSimple.FT_LeadContainer) return "leadcontainer";
        if (t instanceof FluidTraitSimple.FT_Delicious) return "delicious";
        if (t instanceof FluidTraitSimple.FT_NoID) return "noid";
        if (t instanceof FluidTraitSimple.FT_NoContainer) return "nocontainer";
        if (t instanceof FluidTraitSimple.FT_Unsiphonable) return "unsiphonable";
        if (t instanceof FT_Corrosive c) return "corrosive:" + c.getRating();
        if (t instanceof FT_Flammable f) return "flammable:" + f.getHeatEnergy();
        if (t instanceof FT_Combustible c) return "combustible:" + c.getGrade().name() + ":" + c.getCombustionEnergy();
        if (t instanceof FT_Poison p) return "poison:" + p.isWithering() + ":" + p.getLevel();
        if (t instanceof FT_Rocket r) return "rocket:" + r.getISP() + ":" + r.getThrust();
        if (t instanceof SpaceFluidTraits.FT_Terraformer terraform) return "terraformer:" + terraform.getBody();
        if (t instanceof FT_PWRModerator m) return "pwrmoderator:" + m.getMultiplier();
        if (t instanceof FT_VentRadiation v) return "ventradiation:" + v.getRadPerMB();
        if (t instanceof FT_Heatable h) {
            List<String> steps = new ArrayList<>();
            for (FT_Heatable.HeatingStep s : h.steps()) {
                steps.add(
                        s.heatReq + "," + s.amountReq + "," + s.typeProduced.getRegistryName() + "," + s.amountProduced);
            }
            return "heatable:" + String.join(";", steps) + "/" + byName(h.getEfficiencies());
        }
        if (t instanceof FT_Coolable c) {
            return "coolable:" + c.coolsTo.getRegistryName() + ":" + c.amountReq + ":" + c.amountProduced + ":" + c.heatEnergy
                    + "/" + byName(c.getEfficiencies());
        }
        if (t instanceof FT_Polluting p) return "polluting:" + byName(p.releaseMap) + "/" + byName(p.burnMap);
        return "unknown:" + t.getClass().getName();
    }

    /**
     * {@code NAME=value,...} sorted by name.
     */
    private static String byName(Map<? extends Enum<?>, ?> map) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        map.forEach((k, v) -> sorted.put(k.name(), v));
        List<String> parts = new ArrayList<>();
        sorted.forEach((k, v) -> parts.add(k + "=" + v));
        return String.join(",", parts);
    }

    private static List<String> sorted(List<String> list) {
        List<String> copy = new ArrayList<>(list);
        copy.sort(null);
        return List.copyOf(copy);
    }
}
