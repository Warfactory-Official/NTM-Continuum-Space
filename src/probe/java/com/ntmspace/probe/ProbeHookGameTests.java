package com.ntmspace.probe;

import com.hbm.api.environment.Environment;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import dev.golem.core.world.Weathers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Two addons on one hook (scratchpad m2/probe-hook-families.md): every hook NTM: Space listens to has the probe's
 * listener too, in the documented order ({@link #ORDER}), and the combined answers are what the hook's rule says for
 * both listeners together (the other tests): a probe answer in the probe's earlier phase overrides NTM: Space's, a probe
 * answer in the default phase applies only where NTM: Space passes, summing and "anyone may" hooks add both, notified
 * hooks call both.
 */
public final class ProbeHookGameTests {

    private ProbeHookGameTests() {}

    public static void init() {}

    /** Every hook NTM: Space uses carries both listeners, the probe's before NTM: Space's or after, as registered. */
    public static final GameTests.Function ORDER = ProbeGameTests.TESTS.function("hooks_listener_order", 20,
            () -> ProbeHookGameTests::order);
    /** First-answer hooks with the probe's phase first: a marked probe answer beats NTM: Space's on the Mun. */
    public static final GameTests.Function FIRST = ProbeGameTests.TESTS.function("hooks_probe_phase_first", 20,
            () -> ProbeHookGameTests::probeFirst);
    /** First-answer hooks with the probe after: NTM: Space's answer stands, the probe's applies where NTM: Space passes. */
    public static final GameTests.Function AFTER = ProbeGameTests.TESTS.function("hooks_space_answers_first", 20,
            () -> ProbeHookGameTests::spaceFirst);
    /** HBM's own hooks: summed, "anyone may", first product, every line, every offer. */
    public static final GameTests.Function HBM = ProbeGameTests.TESTS.function("hbm_hooks_combined", 20,
            () -> ProbeHookGameTests::hbm);
    /** Notified hooks and Golem's lifecycle events reach the probe beside NTM: Space (a probe cow, ticks). */
    public static final GameTests.Function NOTIFIED = ProbeGameTests.TESTS.function("hooks_notified_both", 100,
            () -> ProbeHookGameTests::notified);

    // ---------------------------------------------------------------------------------------------- order

    private record Row(String name, Hook<?> hook, boolean probeFirst) {}

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        // the probe's phase before the default phase
        rows.add(new Row("Environment.BREATHING", Environment.BREATHING, true));
        rows.add(new Row("Environment.PRESSURE", Environment.PRESSURE, true));
        rows.add(new Row("Environment.COMPRESSION", Environment.COMPRESSION, true));
        rows.add(new Row("Environment.GROWTH", Environment.GROWTH, true));
        rows.add(new Row("Environment.GROUNDWATER", Environment.GROUNDWATER, true));
        rows.add(new Row("Environment.GRAVITY", Environment.GRAVITY, true));
        rows.add(new Row("Environment.SUNLIGHT", Environment.SUNLIGHT, true));
        rows.add(new Row("Furnaces.IGNITION", dev.golem.core.world.Furnaces.IGNITION, true));
        rows.add(new Row("ContaminationUtil.RAD_IMMUNE", com.hbm.util.ContaminationUtil.RAD_IMMUNE, true));
        // the default phase, after NTM: Space (its declare() runs first)
        rows.add(new Row("Environment.SURFACE", Environment.SURFACE, false));
        rows.add(new Row("Environment.ORBIT", Environment.ORBIT, false));
        rows.add(new Row("Environment.SATELLITES", Environment.SATELLITES, false));
        rows.add(new Row("Environment.LIVING_TICK", Environment.LIVING_TICK, false));
        rows.add(new Row("Environment.GAS_RELEASE", Environment.GAS_RELEASE, false));
        rows.add(new Row("Environment.IMPACT", Environment.IMPACT, false));
        rows.add(new Row("Environment.LIGHT_ON", Environment.LIGHT_ON, false));
        rows.add(new Row("Environment.NUCLEAR_BLAST", Environment.NUCLEAR_BLAST, false));
        rows.add(new Row("SkyOwnership.OWNERS", com.hbm.api.environment.SkyOwnership.OWNERS, false));
        rows.add(new Row("Weathers.PROVIDERS", Weathers.PROVIDERS, false));
        rows.add(new Row("Skies.MODIFIERS", dev.golem.core.world.Skies.MODIFIERS, false));
        rows.add(new Row("ArmorUtil.HAZMAT", com.hbm.handler.ArmorUtil.HAZMAT, false));
        rows.add(new Row("HazardTransformerRadiationNBT.COMPONENT_RADIATION",
                HazardTransformerRadiationNBT.COMPONENT_RADIATION, false));
        rows.add(new Row("ItemRag.USE", com.hbm.items.special.ItemRag.USE, false));
        rows.add(new Row("ItemEnergy.EMPTY", com.hbm.items.special.ItemEnergy.EMPTY, false));
        rows.add(new Row("ContaminationUtil.GEIGER_LINES", com.hbm.util.ContaminationUtil.GEIGER_LINES, false));
        rows.add(new Row("BobmazonOfferFactory.OFFERS", com.hbm.handler.BobmazonOfferFactory.OFFERS, false));
        rows.add(new Row("ModCustomMissileItems.PART_PROPERTIES", com.hbm.items.ModCustomMissileItems.PART_PROPERTIES, false));
        rows.add(new Row("ItemBedrockOreBase.SOURCE", com.hbm.items.special.ItemBedrockOreBase.SOURCE, false));
        rows.add(new Row("XSatelliteRegistry.ORBITED", com.hbm.saveddata.satellites.XSatelliteRegistry.ORBITED, false));
        rows.add(new Row("TileEntityMachineSatLinker.CHIP_COPY", com.hbm.tileentity.machine.TileEntityMachineSatLinker.CHIP_COPY, false));
        rows.add(new Row("TileEntityDoorGeneric.MOVED", com.hbm.tileentity.TileEntityDoorGeneric.MOVED, false));
        rows.add(new Row("BlockEntityDecon.DECONTAMINATED", com.hbm.tileentity.machine.BlockEntityDecon.DECONTAMINATED, false));
        rows.add(new Row("TileEntityStorageDrum.SLOT_TICK", com.hbm.tileentity.machine.TileEntityStorageDrum.SLOT_TICK, false));
        rows.add(new Row("TileEntityMachineShredder.INTERCEPT", com.hbm.tileentity.machine.TileEntityMachineShredder.INTERCEPT, false));
        rows.add(new Row("TileEntityReactorResearch.RADIATING", com.hbm.tileentity.machine.TileEntityReactorResearch.RADIATING, false));
        rows.add(new Row("DemonLamp.IRRADIATED", com.hbm.blocks.machine.DemonLamp.TileEntityDemonLamp.IRRADIATED, false));
        rows.add(new Row("EntityBalefire.TICK", com.hbm.entity.logic.EntityBalefire.TICK, false));
        rows.add(new Row("ExplosionNukeCustom.ANTIMATTER", com.hbm.explosion.ExplosionNukeCustom.ANTIMATTER, false));
        rows.add(new Row("TileEntityBarrel.ANTIMATTER_BLAST", com.hbm.tileentity.machine.storage.TileEntityBarrel.ANTIMATTER_BLAST, false));
        rows.add(new Row("EntityBomber.LOOP_SOUND", com.hbm.entity.logic.EntityBomber.LOOP_SOUND, false));
        rows.add(new Row("EntityPlaneBase.CRASH", com.hbm.entity.logic.EntityPlaneBase.CRASH, false));
        rows.add(new Row("TileEntityFileCabinet.TICK", com.hbm.tileentity.machine.storage.TileEntityFileCabinet.TICK, false));
        rows.add(new Row("ItemReactorSensor.USE", com.hbm.items.machine.ItemReactorSensor.USE, false));
        rows.add(new Row("TileEntityMachineMiningLaser.PICKUP", com.hbm.tileentity.machine.TileEntityMachineMiningLaser.PICKUP, false));
        rows.add(new Row("BlockPlaces.HANDLERS", dev.golem.core.world.BlockPlaces.HANDLERS, false));
        rows.add(new Row("BlockBreaks.HANDLERS", dev.golem.core.world.BlockBreaks.HANDLERS, false));
        rows.add(new Row("Deaths.AFTER", dev.golem.core.world.Deaths.AFTER, false));
        rows.add(new Row("ItemUses.FINISH", dev.golem.core.world.ItemUses.FINISH, false));
        rows.add(new Row("Lifecycle.PLAYER_JOIN", dev.golem.core.platform.Lifecycle.PLAYER_JOIN, false));
        rows.add(new Row("Lifecycle.ENTITY_LOAD", dev.golem.core.platform.Lifecycle.ENTITY_LOAD, false));
        return rows;
    }

    /** {@code null} when the order is right, else what is wrong. */
    static String checkOrder(String name, Hook<?> hook, boolean probeFirst) {
        int firstSpace = Integer.MAX_VALUE, lastSpace = -1, firstProbe = Integer.MAX_VALUE, lastProbe = -1;
        List<?> listeners = hook.listeners();
        for (int i = 0; i < listeners.size(); i++) {
            var listener = listeners.get(i);
            var owner = listener.getClass().getName();
            if (owner.startsWith("com.ntmspace.probe.")) {
                firstProbe = Math.min(firstProbe, i);
                lastProbe = i;
            } else if (spaceListener(listener)) {
                firstSpace = Math.min(firstSpace, i);
                lastSpace = i;
            }
        }
        if (lastSpace < 0) return name + ": no NTM: Space listener";
        if (lastProbe < 0) return name + ": no probe listener";
        boolean ok = probeFirst ? lastProbe < firstSpace : lastSpace < firstProbe;
        return ok ? null : name + ": probe listeners at " + firstProbe + ".." + lastProbe + ", NTM: Space's at "
                + firstSpace + ".." + lastSpace + " (probe " + (probeFirst ? "first" : "after") + " expected)";
    }

    private static boolean spaceListener(Object listener) {
        var owner = listener.getClass().getName();
        return owner.startsWith("com.ntmspace.") && !owner.startsWith("com.ntmspace.probe.")
                && !owner.startsWith("com.ntmspace.gametest.")
                || listener == ArmorUtil.effectHazmat().get("ntmspace:nitan");
    }

    private static void order(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (Row row : rows()) {
            String error = checkOrder(row.name(), row.hook(), row.probeFirst());
            if (error != null) errors.add(error);
        }
        finish(helper, errors, rows().size() + " hooks");
    }

    // ---------------------------------------------------------------------------------------------- probe first

    /** NTM: Space's own listeners of {@code hook}, in order (its answer with the probe left out). */
    static <T> List<T> space(Hook<T> hook) {
        List<T> own = new ArrayList<>();
        for (T listener : hook.listeners()) {
            if (spaceListener(listener)) own.add(listener);
        }
        return own;
    }

    private static double firstNumber(List<Double> answers) {
        for (Double d : answers) if (!Double.isNaN(d)) return d;
        return Double.NaN;
    }

    /**
     * Kerbin (the overworld, NTM: Space's home body; the GameTest world has no other body level): for every hook the
     * probe answers first, the combined answer is NTM: Space's while the probe passes, and the probe's marked answer
     * (chosen unlike NTM: Space's) once marked.
     */
    private static void probeFirst(GameTestHelper helper) {
        ServerLevel kerbin = helper.getLevel();
        List<String> errors = new ArrayList<>();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        Entity cow = dev.golem.core.registry.Entities.createDetached(EntityType.COW, kerbin);
        try {
            // NTM: Space's own answers on Kerbin, asked of its listeners alone
            Verdict spaceBreath = Verdict.first(space(Environment.BREATHING), l -> l.breathe(kerbin, at, 0));
            double spacePressure = firstNumber(space(Environment.PRESSURE).stream().map(l -> l.pressure(kerbin, at)).toList());
            Verdict spaceCompress = Verdict.first(space(Environment.COMPRESSION), l -> l.compress(kerbin, at, 0));
            var spaceWater = space(Environment.GROUNDWATER).stream().map(l -> l.groundwater(kerbin, at))
                    .filter(Objects::nonNull).findFirst().orElse(null);
            double spaceGravity = firstNumber(space(Environment.GRAVITY).stream().map(l -> l.gravity(cow)).toList());
            double spaceSun = firstNumber(space(Environment.SUNLIGHT).stream().map(l -> l.sunlight(kerbin, at)).toList());
            expect(errors, spaceBreath == Verdict.ALLOW && spaceCompress == Verdict.ALLOW && !Double.isNaN(spacePressure)
                    && spaceWater != null && !Double.isNaN(spaceGravity) && !Double.isNaN(spaceSun),
                    "NTM: Space does not answer on Kerbin: " + spaceBreath + " " + spaceCompress + " " + spacePressure + " "
                    + spaceWater + " " + spaceGravity + " " + spaceSun);
            // ... which is the combined answer while the probe passes
            expect(errors, Environment.breatheAir(kerbin, at, 0) && Environment.pressure(kerbin, at) == spacePressure
                    && Environment.compressAir(kerbin, at, 0) && Environment.groundwater(kerbin, at) == spaceWater
                    && Environment.gravity(cow) == spaceGravity && Environment.sunlight(kerbin, at) == spaceSun,
                    "the combined answers differ from NTM: Space's while the probe passes");

            // the probe's marked answers, in its phase before NTM: Space's: they decide
            ProbeMarks.at("breathing", kerbin, at, Verdict.DENY);
            ProbeMarks.at("pressure", kerbin, at, 0.25D);
            ProbeMarks.at("compression", kerbin, at, Verdict.DENY);
            ProbeMarks.at("growth", kerbin, at, Verdict.DENY);
            ProbeMarks.at("groundwater", kerbin, at, Fluids.OIL);
            ProbeMarks.at("sunlight", kerbin, at, 0.125D);
            ProbeMarks.entity("gravity", cow, 3.0D);
            expect(errors, !Environment.breatheAir(kerbin, at, 0), "BREATHING: the probe's DENY lost to NTM: Space's ALLOW");
            expect(errors, Environment.pressure(kerbin, at) == 0.25D, "PRESSURE: " + Environment.pressure(kerbin, at));
            expect(errors, !Environment.compressAir(kerbin, at, 0), "COMPRESSION: the probe's DENY lost");
            // NTM: Space would let grass grow under Kerbin's air (and convert CO2): the probe's DENY comes first
            expect(errors, !Environment.canGrow(kerbin, at), "GROWTH: the probe's DENY lost");
            expect(errors, Environment.groundwater(kerbin, at) == Fluids.OIL, "GROUNDWATER: " + Environment.groundwater(kerbin, at));
            expect(errors, Environment.sunlight(kerbin, at) == 0.125D, "SUNLIGHT: " + Environment.sunlight(kerbin, at));
            expect(errors, Environment.gravity(cow) == 3.0D, "GRAVITY: " + Environment.gravity(cow));
            // the probe's sealed spot (ProbeContent, the same phase) breathes at 1 atm
            BlockPos sealed = at.above();
            ProbeContent.seal(kerbin, sealed);
            expect(errors, Environment.breatheAir(kerbin, sealed, 0) && Environment.pressure(kerbin, sealed) == Environment.EARTH_PRESSURE,
                    "the probe's sealed spot");

            // vanilla furnaces (Golem Furnaces.IGNITION): NTM: Space passes on Kerbin, the probe's DENY at its spot decides
            helper.setBlock(new BlockPos(1, 2, 1), net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
            ItemStack coal = new ItemStack(Items.COAL);
            // NTM: Space's furnace rule asks BREATHING, so the probe's DENY there (still marked) puts the furnace out too
            int noAir = dev.golem.core.world.Furnaces.burnDuration(kerbin, at, coal, 1600);
            ProbeMarks.clear("breathing");
            int lit = dev.golem.core.world.Furnaces.burnDuration(kerbin, at, coal, 1600);
            ProbeMarks.at("ignition", kerbin, at, Verdict.DENY);
            int denied = dev.golem.core.world.Furnaces.burnDuration(kerbin, at, coal, 1600);
            expect(errors, noAir == 0 && lit == 1600 && denied == 0, "IGNITION: probe-unbreathable " + noAir + ", Kerbin " + lit
                    + ", probe-denied " + denied);
        } finally {
            for (String hook : List.of("breathing", "pressure", "compression", "growth", "groundwater", "sunlight", "gravity", "ignition")) {
                ProbeMarks.clear(hook);
            }
        }
        finish(helper, errors, "");
    }

    // ---------------------------------------------------------------------------------------------- space first

    /**
     * The hooks the probe listens to after NTM: Space: NTM: Space's answer for its body (Kerbin) stands, the probe's
     * applies where NTM: Space passes (the nether); "any owner" hooks take both claims.
     */
    private static void spaceFirst(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerLevel kerbin = server.getLevel(Level.OVERWORLD);
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel end = server.getLevel(Level.END);
        List<String> errors = new ArrayList<>();
        if (nether == null || end == null || kerbin == null) {
            finish(helper, List.of("a vanilla level is missing"), "");
            return;
        }
        try {
            // SURFACE: NTM: Space says Kerbin is a body's surface; the probe's DENY there comes too late
            Verdict spaceSurface = Verdict.first(space(Environment.SURFACE), l -> l.surface(kerbin));
            ProbeMarks.level("surface", Level.OVERWORLD, Verdict.DENY);
            ProbeMarks.level("surface", Level.NETHER, Verdict.ALLOW);
            expect(errors, spaceSurface == Verdict.ALLOW && Environment.isSurface(kerbin), "SURFACE: the probe overrode NTM: Space on Kerbin");
            expect(errors, Environment.isSurface(nether), "SURFACE: the probe's answer where NTM: Space passes was lost");
            // ORBIT: NTM: Space passes off its orbit level; the probe's ALLOW applies
            boolean netherOrbit = Environment.inOrbit(nether);
            ProbeMarks.level("orbit", Level.NETHER, Verdict.ALLOW);
            expect(errors, !netherOrbit && Environment.inOrbit(nether) && !Environment.inOrbit(kerbin), "ORBIT: nether/kerbin");
            // SATELLITES: nobody answers for the nether until the probe does
            ServerLevel before = Environment.satelliteLevel(nether, null, ItemStack.EMPTY);
            ProbeMarks.level("satellites", Level.NETHER, Level.END);
            ServerLevel after = Environment.satelliteLevel(nether, null, ItemStack.EMPTY);
            expect(errors, before == nether && after == end, "SATELLITES: " + before.dimension() + " then " + after.dimension());
            // SKY OWNERSHIP: any owner claims; NTM: Space claims Kerbin, the probe the marked nether
            boolean netherBefore = com.hbm.api.environment.SkyOwnership.claimed(nether);
            ProbeMarks.level("sky_owner", Level.NETHER, true);
            expect(errors, !netherBefore && com.hbm.api.environment.SkyOwnership.claimed(nether)
                    && com.hbm.api.environment.SkyOwnership.claimed(kerbin) && !com.hbm.api.environment.SkyOwnership.claimed(end),
                    "SkyOwnership: both claims must hold, the end stays unclaimed");
            // WEATHER: first non-null; an earlier answer (NTM: Space's, or a Space weather test's fixture running in the
            // same batch) stands, the probe's provider after them answers otherwise
            var probeWeather = new Weathers.State(0.7F, 0.3F);
            for (ServerLevel weatherLevel : List.of(kerbin, nether)) {
                var earlier = Weathers.weather(weatherLevel);
                ProbeMarks.level("weather", weatherLevel.dimension(), probeWeather);
                var combined = Weathers.weather(weatherLevel);
                expect(errors, Objects.equals(combined, earlier != null ? earlier : probeWeather),
                        "Weathers in " + weatherLevel.dimension() + ": earlier " + earlier + ", combined " + combined);
            }
            // SKIES: the probe's modifier receives what the earlier phases made of the daylight (NTM: Space's celestial
            // phase is active on its other bodies; the harness checks that chain on the Mun) and halves it
            float daylight = dev.golem.core.world.Skies.daylight(kerbin, 1.0F);
            ProbeMarks.level("skies", Level.OVERWORLD, 0.5D);
            float halved = dev.golem.core.world.Skies.daylight(kerbin, 1.0F);
            Object input = ProbeMarks.flag("skies_input");
            expect(errors, input instanceof Float f && f == daylight && Math.abs(halved - daylight * 0.5F) < 1e-6F,
                    "Skies: before " + daylight + ", probe received " + input + ", gave " + halved);
        } finally {
            for (String hook : List.of("surface", "orbit", "satellites", "sky_owner", "weather", "skies", "skies_input")) ProbeMarks.clear(hook);
        }
        finish(helper, errors, "");
    }

    // ---------------------------------------------------------------------------------------------- HBM's hooks

    private static void hbm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        try {
            // RAD_IMMUNE: NTM: Space took the ocelot's immunity (DENY); the probe's ALLOW in its earlier phase gives it back
            Entity ocelot = dev.golem.core.registry.Entities.createDetached(EntityType.OCELOT, level);
            Entity zombie = dev.golem.core.registry.Entities.createDetached(EntityType.ZOMBIE, level);
            boolean before = com.hbm.util.ContaminationUtil.isRadImmune(ocelot);
            ProbeMarks.entity("rad_immune", ocelot, Verdict.ALLOW);
            expect(errors, !before && com.hbm.util.ContaminationUtil.isRadImmune(ocelot)
                    && com.hbm.util.ContaminationUtil.isRadImmune(zombie), "RAD_IMMUNE: ocelot " + before + " / marked / zombie");
            // HAZMAT: anyone may say yes
            var pig = (net.minecraft.world.entity.LivingEntity) dev.golem.core.registry.Entities.createDetached(EntityType.PIG, level);
            boolean bare = com.hbm.handler.ArmorUtil.checkForHazmat(pig);
            ProbeMarks.entity("hazmat", pig, true);
            expect(errors, !bare && com.hbm.handler.ArmorUtil.checkForHazmat(pig), "HAZMAT: " + bare);
            // STACK_RADIATION: summed over the listeners
            ItemStack feather = new ItemStack(Items.FEATHER);
            double r0 = com.hbm.hazard.type.HazardTypeRadiation.stackRadiation(feather);
            ProbeMarks.flag("stack_radiation", 1.5D);
            double r1 = com.hbm.hazard.type.HazardTypeRadiation.stackRadiation(feather);
            expect(errors, Math.abs(r1 - (r0 + 1.5D)) < 1e-9, "STACK_RADIATION: " + r0 + " then " + r1);
            // RAG: the first non-empty product; NTM: Space's blood rag above 500 RAD comes before the probe's feather
            var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
            String plain = id(com.hbm.items.special.ItemRag.product(player));
            ProbeMarks.entity("rag", player, true);
            String probe = id(com.hbm.items.special.ItemRag.product(player));
            com.hbm.capability.HbmLivingProps.setRadiation(player, 600);
            String space = id(com.hbm.items.special.ItemRag.product(player));
            com.hbm.capability.HbmLivingProps.setRadiation(player, 0);
            expect(errors, plain.equals("hbm:rag_piss") && probe.equals("minecraft:feather") && space.equals("ntmspace:rag_blood"),
                    "ItemRag.USE: " + plain + " / " + probe + " / " + space);
            // GEIGER_LINES: every listener prints, NTM: Space's line before the probe's
            ProbeMarks.flag("geiger", true);
            List<Component> lines = com.hbm.util.ContaminationUtil.geigerData(player);
            int spaceLine = -1, probeLine = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        && t.getKey().equals("geiger.ntmspace.playerAct")) spaceLine = i;
                if (lines.get(i).getString().equals("ntmprobe geiger line")) probeLine = i;
            }
            expect(errors, spaceLine >= 0 && probeLine > spaceLine, "GEIGER_LINES: NTM: Space at " + spaceLine + ", probe at " + probeLine);
            // OFFERS: both addons' offers, the probe's after NTM: Space's
            List<com.hbm.handler.BobmazonOfferFactory.Offer> special = com.hbm.handler.BobmazonOfferFactory.special();
            int pellet = -1, offer = -1;
            for (int i = 0; i < special.size(); i++) {
                String item = id(special.get(i).offer);
                if (item.equals("hbm:pellet_charged")) pellet = i;
                if (item.equals("minecraft:feather")) offer = i;
            }
            expect(errors, pellet >= 0 && offer > pellet, "OFFERS: NTM: Space's pellet at " + pellet + ", probe's feather at " + offer);
            // PART_PROPERTIES: the probe was asked about every missile part, after NTM: Space
            expect(errors, !ProbeHooks.MISSILE_PARTS_SEEN.isEmpty(), "PART_PROPERTIES never asked the probe");
        } finally {
            for (String hook : List.of("rad_immune", "hazmat", "stack_radiation", "rag", "geiger")) ProbeMarks.clear(hook);
        }
        finish(helper, errors, "");
    }

    private static String id(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ---------------------------------------------------------------------------------------------- notified

    private static void notified(GameTestHelper helper) {
        long ticks = ProbeMarks.calls("server_tick");
        long loads = ProbeMarks.calls("probe_cow_load");
        long livingTicks = ProbeMarks.calls("living_tick");
        if (ProbeMarks.calls("server_started") < 1) {
            fail(helper, "Lifecycle.onServerStarted never reached the probe");
            return;
        }
        // the probe's entity type: attributes (Golem LivingAttributes) or spawning it fails; its load event and its
        // living ticks reach the probe's listeners beside NTM: Space's (which take this cow's gravity and air)
        var cow = helper.spawn(ProbeRecords.PROBE_COW.get(), new BlockPos(2, 2, 2));
        var moonCow = helper.spawn(com.ntmspace.registry.SpaceMobs.MOON_COW.get(), new BlockPos(4, 2, 2));
        ProbeMarks.entity("living_tick", cow, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.getMaxHealth() == moonCow.getMaxHealth() && cow.getMaxHealth() > 0,
                    "attributes: probe cow " + cow.getMaxHealth() + ", moon cow " + moonCow.getMaxHealth());
            helper.assertTrue(ProbeMarks.calls("probe_cow_load") > loads, "Lifecycle.onEntityLoad did not reach the probe");
            helper.assertTrue(ProbeMarks.calls("living_tick") > livingTicks, "Environment.LIVING_TICK did not reach the probe");
            helper.assertTrue(ProbeMarks.calls("server_tick") > ticks + 2, "Lifecycle.onServerTick did not reach the probe");
            ProbeMarks.clear("living_tick");
        });
    }

    // ---------------------------------------------------------------------------------------------- helpers

    static void expect(List<String> errors, boolean ok, String message) {
        if (!ok) errors.add(message);
    }

    static void finish(GameTestHelper helper, List<String> errors, String what) {
        if (errors.isEmpty()) {
            if (!what.isEmpty()) ProbeEntry.LOG.info("probe coexistence ok: {}", what);
            helper.succeed();
        } else {
            fail(helper, String.join("; ", errors));
        }
    }

    static void fail(GameTestHelper helper, String message) {
        ProbeEntry.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
