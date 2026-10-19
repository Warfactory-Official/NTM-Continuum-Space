package com.ntmspace.gametest;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.atmosphere.AtmosphereBlob;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.atmosphere.AtmosphereProvider;
import com.ntmspace.atmosphere.Sealing;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.breathing.ItemModOxy;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.AtmosphereContent;
import com.ntmspace.tileentity.TileEntityAirPump;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CBT_Destroyed;
import com.ntmspace.trait.CBT_Dyson;
import com.ntmspace.trait.CBT_Impact;
import com.ntmspace.trait.CBT_Invasion;
import com.ntmspace.trait.CBT_Lights;
import com.ntmspace.trait.CBT_Temperature;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CBT_Water;
import com.ntmspace.trait.CBT_Weather;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.trait.SolarSystemSavedData;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Celestial body traits, sealed rooms and breathing against hbm-space's values: the trait save round trip (by trait id
 * and fluid name), the 1.7 default traits, gas emission and capture, which blocks seal, a sealed room's flood fill opening
 * and closing, the air vent filling a room to 0.2 atm of oxygen, suffocation damage and breathing from a sealed suit.
 */
public final class AtmosphereGameTests {

    private AtmosphereGameTests() {}

    public static void init() {}

    public static final GameTests.Function TRAIT_ROUND_TRIP = SpaceGameTests.TESTS.function("trait_save_round_trip", 20,
            () -> AtmosphereGameTests::traitRoundTrip);
    public static final GameTests.Function TRAIT_DEFAULTS = SpaceGameTests.TESTS.function("trait_defaults_17", 20,
            () -> AtmosphereGameTests::traitDefaults);
    public static final GameTests.Function GAS = SpaceGameTests.TESTS.function("trait_gas_emit_consume", 20,
            () -> AtmosphereGameTests::gas);
    public static final GameTests.Function SEALING = SpaceGameTests.TESTS.function("atmosphere_block_sealing", 20,
            () -> AtmosphereGameTests::sealing);
    public static final GameTests.Function FLOOD_FILL = SpaceGameTests.TESTS.function("atmosphere_sealed_room_flood_fill", 40, 12,
            () -> AtmosphereGameTests::floodFill);
    public static final GameTests.Function AIR_VENT = SpaceGameTests.TESTS.function("atmosphere_air_vent_fills_room", 200, 12,
            () -> AtmosphereGameTests::airVent);
    public static final GameTests.Function SUFFOCATION = SpaceGameTests.TESTS.function("breathing_suffocation_17", 40,
            () -> AtmosphereGameTests::suffocation);
    public static final GameTests.Function SUIT = SpaceGameTests.TESTS.function("breathing_sealed_suit_plss", 40,
            () -> AtmosphereGameTests::suit);

    // ---------------------------------------------------------------------------------------------------------------

    private static void traitRoundTrip(GameTestHelper helper) {
        CBT_Atmosphere air = new CBT_Atmosphere(Fluids.OXYGEN, 0.25D).and(SpaceFluids.EARTHAIR, 0.5D);
        CBT_War war = new CBT_War(70, 12);
        war.launchProjectile(30F, 2, 5, 1F, 2D, 3D, CBT_War.ProjectileType.NUCLEAR, "ntmspace:duna");
        CBT_Dyson dyson = new CBT_Dyson();
        CBT_Invasion invasion = new CBT_Invasion(2, 122, true);
        invasion.kills = 17;
        CBT_Weather weather = new CBT_Weather();
        weather.forceThunder(1234);
        CelestialBodyTrait[] traits = {air, new CBT_Temperature(-42.5F), new CBT_Water(SpaceFluids.BROMINE), new CBT_Impact(987654321L),
                new CBT_Lights(2001), new CBT_Destroyed(13.5F), war, dyson, invasion, weather, new CelestialBodyTrait.CBT_Battlefield(),
                new CelestialBodyTrait.CBT_Compromised(), new com.ntmspace.trait.CBT_Bees(3.5F)};

        SolarSystemSavedData data = new SolarSystemSavedData();
        data.setTraits("laythe", traits);
        Tag tag = SolarSystemSavedData.toTag(data);
        String nbt = tag.toString();
        // saved by name: bodies, trait ids and fluids by registry name, never numbers
        for (String expected : new String[] {"laythe", "atmosphere", "\"hbm:oxygen\"", "\"ntmspace:earthair\"", "\"ntmspace:bromine\"",
                "temperature", "water", "impact", "lights", "destroyed", "war", "\"ntmspace:duna\"", "\"NUCLEAR\"", "dyson",
                "invasion", "weather", "battle", "infected", "bees"}) {
            if (!nbt.contains(expected)) {
                fail(helper, "trait save lacks " + expected + ": " + nbt);
                return;
            }
        }

        SolarSystemSavedData read = SolarSystemSavedData.fromTag(tag);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> back = read.getTraits("laythe");
        if (back == null || back.size() != traits.length) {
            fail(helper, "round trip lost traits: " + (back == null ? null : back.keySet()));
            return;
        }
        CBT_Atmosphere air2 = (CBT_Atmosphere) back.get(CBT_Atmosphere.class);
        CBT_War war2 = (CBT_War) back.get(CBT_War.class);
        CBT_Invasion inv2 = (CBT_Invasion) back.get(CBT_Invasion.class);
        CBT_Weather weather2 = (CBT_Weather) back.get(CBT_Weather.class);
        boolean ok = air2.getPressure(Fluids.OXYGEN) == 0.25D && air2.getPressure(SpaceFluids.EARTHAIR) == 0.5D
                && ((CBT_Temperature) back.get(CBT_Temperature.class)).degrees == -42.5F
                && ((CBT_Water) back.get(CBT_Water.class)).fluid == SpaceFluids.BROMINE
                && ((CBT_Impact) back.get(CBT_Impact.class)).time == 987654321L
                && ((CBT_Lights) back.get(CBT_Lights.class)).lights == 2001
                && ((CBT_Destroyed) back.get(CBT_Destroyed.class)).destProgress == 13.5F
                && war2.health == 70 && war2.shield == 12 && war2.projectiles.size() == 1
                && war2.projectiles.get(0).getTarget().equals("ntmspace:duna")
                && war2.projectiles.get(0).getType() == CBT_War.ProjectileType.NUCLEAR
                && inv2.wave == 2 && inv2.waveTime == 122 && inv2.isInvading && inv2.kills == 17
                && weather2.thundering && weather2.raining && weather2.rainTime == 1234
                && ((com.ntmspace.trait.CBT_Bees) back.get(com.ntmspace.trait.CBT_Bees.class)).bees == 3.5F;
        if (!ok) {
            fail(helper, "trait values changed in the round trip: " + SolarSystemSavedData.toTag(read));
            return;
        }

        // 1.7 skipped an unknown trait instead of losing the body
        CompoundTag withUnknown = ((CompoundTag) tag).copy();
        ((CompoundTag) ((CompoundTag) withUnknown.get("bodies")).get("laythe")).put("no_such_trait", new CompoundTag());
        SolarSystemSavedData tolerant = SolarSystemSavedData.fromTag(withUnknown);
        if (tolerant.getTraits("laythe") == null || tolerant.getTraits("laythe").size() != traits.length) {
            fail(helper, "an unknown trait id should be skipped, not lose the body");
            return;
        }

        // client sync bytes (1.7 writeToBytes/readFromBytes; pressure travels as a float)
        RegistryFriendlyByteBuf buf = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        air.writeToBytes(buf);
        CBT_Atmosphere synced = new CBT_Atmosphere();
        synced.readFromBytes(buf);
        if (synced.fluids.size() != 2 || synced.getPressure(Fluids.OXYGEN) != (float) 0.25D) {
            fail(helper, "atmosphere sync bytes differ");
            return;
        }
        helper.succeed();
    }

    private static void traitDefaults(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        expectAir(errors, "kerbin", SpaceFluids.EARTHAIR, 1D);
        expectAir(errors, "eve", SpaceFluids.EVEAIR, 5D);
        expectAir(errors, "duna", SpaceFluids.DUNAAIR, 0.1D);
        expectAir(errors, "laythe", SpaceFluids.EARTHAIR, 0.45D);
        expectAir(errors, "laythe", Fluids.XENON, 0.15D);
        expectAir(errors, "tekto", SpaceFluids.TEKTOAIR, 1.5D);
        expectAir(errors, "thatmo", SpaceFluids.NITROGEN, (double) 0.005F); // hbm-space passed 0.005F
        for (String none : new String[] {"mun", "minmus", "moho", "ike", "dres", "kerbol", "jool"}) {
            if (CelestialTraits.getDefaultTrait(none, CBT_Atmosphere.class) != null) errors.add(none + " should have no air");
        }
        expectWater(errors, "kerbin", Fluids.WATER);
        expectWater(errors, "eve", Fluids.MERCURY);
        expectWater(errors, "minmus", SpaceFluids.MILK);
        expectWater(errors, "ike", SpaceFluids.BROMINE);
        expectWater(errors, "laythe", Fluids.WATER);
        expectWater(errors, "tekto", SpaceFluids.CCL);
        CBT_Temperature moho = CelestialTraits.getDefaultTrait("moho", CBT_Temperature.class);
        CBT_Temperature eve = CelestialTraits.getDefaultTrait("eve", CBT_Temperature.class);
        if (moho == null || moho.degrees != 200) errors.add("moho temperature");
        if (eve == null || eve.degrees != 400) errors.add("eve temperature");
        if (!CelestialTraits.hasDefaultTrait("thatmo", CelestialBodyTrait.CBT_Battlefield.class)) errors.add("thatmo battlefield");
        // a level that is no body is Kerbin (1.7 getBody fallback); the overworld breathes
        if (!CelestialTraits.bodyOf(helper.getLevel()).equals("kerbin")) errors.add("overworld is not kerbin");
        if (!AtmosphereHandler.canBreathe(AtmosphereHandler.getCelestialAtmosphere(helper.getLevel()))) errors.add("kerbin air not breathable");
        if (errors.isEmpty()) helper.succeed();
        else fail(helper, String.join("; ", errors));
    }

    private static void expectAir(List<String> errors, String body, FluidType fluid, double pressure) {
        CBT_Atmosphere air = CelestialTraits.getDefaultTrait(body, CBT_Atmosphere.class);
        if (air == null || air.getPressure(fluid) != pressure) errors.add(body + " air " + fluid.getRegistryName() + " != " + pressure);
    }

    private static void expectWater(List<String> errors, String body, FluidType fluid) {
        CBT_Water water = CelestialTraits.getDefaultTrait(body, CBT_Water.class);
        if (water == null || water.fluid != fluid) errors.add(body + " water != " + fluid.getRegistryName());
    }

    /** emitGas / consumeGas on a body of their own (MB_PER_ATM mB per atmosphere), restored afterwards. */
    private static void gas(GameTestHelper helper) {
        CelestialTraits.attach(helper.getLevel());
        String body = "ntmspace_gametest_body";
        try {
            CelestialTraits.emitGas(body, Fluids.OXYGEN, CelestialTraits.MB_PER_ATM * 0.5D);
            CBT_Atmosphere air = CelestialTraits.getTrait(body, CBT_Atmosphere.class, false);
            if (air == null || air.getPressure(Fluids.OXYGEN) != 0.5D) {
                fail(helper, "emitGas: " + (air == null ? null : air.getPressure()));
                return;
            }
            if (!CelestialTraits.consumeGas(body, Fluids.OXYGEN, CelestialTraits.MB_PER_ATM * 0.25D)
                    || CelestialTraits.getTrait(body, CBT_Atmosphere.class, false).getPressure() != 0.25D) {
                fail(helper, "consumeGas did not take a quarter atmosphere");
                return;
            }
            if (CelestialTraits.consumeGas(body, Fluids.WATER, 1)) {
                fail(helper, "consumeGas took a gas that is not there");
                return;
            }
            CelestialTraits.consumeGas(body, Fluids.OXYGEN, CelestialTraits.MB_PER_ATM);
            if (CelestialTraits.getTrait(body, CBT_Atmosphere.class, false) != null) {
                fail(helper, "an emptied atmosphere should be removed (1.7)");
                return;
            }
            // at most eight gases: the thinnest goes
            for (int i = 0; i < 9; i++) {
                FluidType[] gases = {Fluids.OXYGEN, Fluids.HYDROGEN, Fluids.XENON, Fluids.CHLORINE, Fluids.CARBONDIOXIDE,
                        SpaceFluids.EARTHAIR, SpaceFluids.NITROGEN, SpaceFluids.ARGON, SpaceFluids.NEON};
                CelestialTraits.emitGas(body, gases[i], CelestialTraits.MB_PER_ATM * (i + 1));
            }
            CBT_Atmosphere eight = CelestialTraits.getTrait(body, CBT_Atmosphere.class, false);
            if (eight.fluids.size() != 8 || eight.hasFluid(Fluids.OXYGEN) || !eight.hasFluid(SpaceFluids.NEON)) {
                fail(helper, "the ninth gas should push out the thinnest: " + eight.fluids.size());
                return;
            }
            helper.succeed();
        } finally {
            CelestialTraits.clearTraits(body);
        }
    }

    private static void sealing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Level level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        checkSeal(helper, errors, pos, Blocks.STONE, true);
        checkSeal(helper, errors, pos, Blocks.GLASS, true);
        checkSeal(helper, errors, pos, Blocks.AIR, false);
        checkSeal(helper, errors, pos, Blocks.OAK_FENCE, false);
        checkSeal(helper, errors, pos, Blocks.FARMLAND, false);
        checkSeal(helper, errors, pos, Blocks.OAK_LEAVES, false);
        checkSeal(helper, errors, pos, Blocks.STONE_SLAB, false);
        checkSeal(helper, errors, pos, Blocks.TORCH, false);
        checkSeal(helper, errors, pos, block("hbm:machine_diesel"), false); // #ntmspace:unsealable
        checkSeal(helper, errors, pos, block("hbm:machine_difurnace_off"), false);
        checkSeal(helper, errors, pos, AtmosphereContent.AIR_VENT.get(), false); // air passes through a vent
        checkSeal(helper, errors, pos, block("hbm:fan"), false); // the substituted fan
        if (!BuiltInRegistries.ENTITY_TYPE.getOptional(Ids.parse("hbm:entity_cyber_crab")).orElseThrow().builtInRegistryHolder().is(Sealing.SUFFOCATION_IMMUNE)) {
            errors.add("cyber crab not suffocation immune");
        }
        if (!new ItemStack(item("hbm:hev_helmet")).is(Sealing.SEALED_ARMOR)) errors.add("HEV helmet not sealed armour");
        if (new ItemStack(item("hbm:steel_helmet")).is(Sealing.SEALED_ARMOR)) errors.add("steel helmet is sealed armour");
        if (errors.isEmpty()) helper.succeed();
        else fail(helper, String.join("; ", errors));
    }

    private static void checkSeal(GameTestHelper helper, List<String> errors, BlockPos rel, net.minecraft.world.level.block.Block block, boolean sealed) {
        helper.setBlock(rel, block.defaultBlockState());
        boolean actual = AtmosphereBlob.isBlockSealed(helper.getLevel(), helper.absolutePos(rel));
        if (actual != sealed) errors.add(BuiltInRegistries.BLOCK.getKey(block) + " sealed=" + actual);
    }

    private static net.minecraft.world.level.block.Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElseThrow();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    /** A hollow 5 x 5 x 5 stone box around {@code centre}: 27 air blocks inside. */
    private static void buildBox(GameTestHelper helper, BlockPos centre) {
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            boolean wall = Math.abs(x) == 2 || Math.abs(y) == 2 || Math.abs(z) == 2;
            helper.setBlock(centre.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    /** A provider with a fixed gas and pressure, like a vent that is always full. */
    private static final class TestProvider implements AtmosphereProvider {
        final Level level;
        final BlockPos root;
        @Nullable AtmosphereBlob blob;
        int consumed;

        TestProvider(Level level, BlockPos root) {
            this.level = level;
            this.root = root.immutable();
        }

        @Override public Level getLevel() { return level; }
        @Override public int getMaxBlobRadius() { return 12; }
        @Override public BlockPos getRootPosition() { return root; }
        @Override public FluidType getFluidType() { return Fluids.OXYGEN; }
        @Override public double getFluidPressure() { return blob == null || blob.getBlobSize() == 0 ? 0 : 0.2D; }
        @Override public void consume(int amount) { consumed += amount; }
        @Override public void produce(int amount) {}
        @Override public void onBlobCreated(AtmosphereBlob blob) { this.blob = blob; }
    }

    private static void floodFill(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(12, 4);
        buildBox(helper, centre);
        Level level = helper.getLevel();
        BlockPos root = helper.absolutePos(centre);
        TestProvider provider = new TestProvider(level, root);
        try {
            AtmosphereHandler.registerAtmosphere(provider);
            if (provider.blob == null || provider.blob.getBlobSize() != 27) {
                fail(helper, "sealed 3x3x3 room should hold 27 blocks, got " + (provider.blob == null ? null : provider.blob.getBlobSize()));
                return;
            }
            // vacuum outside, 0.2 atm of oxygen inside: breathable (0.09) only in the room
            CBT_Atmosphere inside = AtmosphereHandler.combine(new CBT_Atmosphere(), level, root.above(), null);
            CBT_Atmosphere outside = AtmosphereHandler.combine(new CBT_Atmosphere(), level, root.above(3), null);
            if (inside == null || inside.getPressure(Fluids.OXYGEN) != 0.2D || !AtmosphereHandler.canBreathe(inside) || outside != null) {
                fail(helper, "room air wrong: inside " + (inside == null ? null : inside.getPressure()) + ", outside " + outside);
                return;
            }
            if (!AtmosphereHandler.hasAtmosphere(level, root.east()) || AtmosphereHandler.hasAtmosphere(level, root.east(2))) {
                fail(helper, "hasAtmosphere should be true in the room and false in its wall");
                return;
            }

            // break a wall: the room joins the open air, the fill reaches past the radius and the room is voided
            BlockPos wall = root.east(2);
            AtmosphereHandler.notifyRemoved(level, wall);
            level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
            // 1.7 order: the break event runs before the block is gone, so the fill starts at the hole itself
            if (provider.blob.getBlobSize() != 0) {
                fail(helper, "an opened room should be voided, still " + provider.blob.getBlobSize());
                return;
            }
            // close it again: the room re-forms from its root
            level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
            AtmosphereHandler.notifyPlaced(level, wall);
            if (provider.blob.getBlobSize() != 27) {
                fail(helper, "a re-sealed room should hold 27 blocks again, got " + provider.blob.getBlobSize());
                return;
            }
            // placing a block inside shrinks the room
            BlockPos inner = root.west();
            level.setBlock(inner, Blocks.STONE.defaultBlockState(), 3);
            AtmosphereHandler.notifyPlaced(level, inner);
            if (provider.blob.getBlobSize() != 26) {
                fail(helper, "a block placed inside should leave 26, got " + provider.blob.getBlobSize());
                return;
            }
            // machines breathing in the room consume from it
            if (!com.ntmspace.atmosphere.AtmosphereHooks.breatheAir(level, root, 3)) {
                fail(helper, "a fire in the room should breathe");
                return;
            }
            helper.succeed();
        } finally {
            AtmosphereHandler.unregisterAtmosphere(provider);
        }
    }

    private static void airVent(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(12, 4);
        buildBox(helper, centre);
        helper.setBlock(centre, AtmosphereContent.AIR_VENT.get().defaultBlockState());
        TileEntityAirPump vent = GameTestHelpers.blockEntity(helper, centre, TileEntityAirPump.class);
        vent.tank.setFill(16000);
        BlockPos above = helper.absolutePos(centre.above());
        helper.succeedWhen(() -> {
            if (vent.blob() == null || vent.blob().getBlobSize() != 27) helper.fail("vent room not formed: " + (vent.blob() == null ? null : vent.blob().getBlobSize()));
            // 27 blocks filled 20 + 7 mB: 0.2 atm of oxygen (1.7 airFill / size * 0.2)
            if (vent.getFluidPressure() != 0.2D || !vent.hasSeal()) helper.fail("vent pressure " + vent.getFluidPressure());
            if (vent.tank.getFill() > 16000 - 27) helper.fail("vent tank " + vent.tank.getFill());
            CBT_Atmosphere room = AtmosphereHandler.combine(new CBT_Atmosphere(), helper.getLevel(), above, null);
            if (room == null || !AtmosphereHandler.canBreathe(room)) helper.fail("vent room not breathable");
        });
    }

    private static void suffocation(GameTestHelper helper) {
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        float health = pig.getHealth();
        Breathing.setOxy(pig, 100);
        for (int i = 0; i < 50; i++) Breathing.handleOxy(pig, null);
        if (Breathing.getOxy(pig) != 50) {
            fail(helper, "50 ticks without air should leave 50 oxygen, got " + Breathing.getOxy(pig));
            return;
        }
        if (pig.getHealth() != health) {
            fail(helper, "no damage before the oxygen runs out");
            return;
        }
        Breathing.handleOxy(pig, new CBT_Atmosphere(SpaceFluids.EARTHAIR, 0.21D));
        if (Breathing.getOxy(pig) != 100) {
            fail(helper, "breathable air (0.21 atm earth air) should refill to 100");
            return;
        }
        Breathing.handleOxy(pig, new CBT_Atmosphere(SpaceFluids.EARTHAIR, 0.2D));
        if (Breathing.getOxy(pig) != 99) {
            fail(helper, "0.2 atm earth air is not breathable (1.7: 0.21)");
            return;
        }
        // 1.7 setOxy: at 0 and below, damage max(1, -oxy/40 - 10) every clamp(oxy/60 + 8, 1, 8) ticks
        pig.tickCount = 40;
        Breathing.setOxy(pig, -1);
        if (pig.getHealth() != health - 1) {
            fail(helper, "oxyprime should deal 1 at oxygen -1, health " + pig.getHealth());
            return;
        }
        // an immune mob never loses oxygen
        EntityType<?> crabType = BuiltInRegistries.ENTITY_TYPE.getOptional(Ids.parse("hbm:entity_cyber_crab")).orElseThrow();
        if (crabType.builtInRegistryHolder().is(Sealing.SUFFOCATION_IMMUNE) && helper.spawn(crabType, new BlockPos(1, 2, 1)) instanceof LivingEntity crab) {
            Breathing.handleOxy(crab, null);
            if (Breathing.getOxy(crab) != 100) {
                fail(helper, "a suffocation-immune mob lost oxygen");
                return;
            }
        }
        helper.succeed();
    }

    private static void suit(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack helmet = new ItemStack(item("hbm:hev_helmet"));
        ItemStack plss = new ItemStack(AtmosphereContent.OXY_PLSS.get());
        ItemModOxy.setFuel(plss, 1000);
        ArmorModHandler.applyMod(helmet, plss);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(item("hbm:hev_plate")));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(item("hbm:hev_legs")));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(item("hbm:hev_boots")));

        player.tickCount = 10; // 1.7: one consumption every rate (10) ticks
        if (!Breathing.checkForOxy(player, null)) {
            fail(helper, "a sealed suit with a filled PLSS should breathe in vacuum");
            return;
        }
        int fuel = ItemModOxy.getFuel(ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.HEAD), ArmorModHandler.plate_only));
        if (fuel != 999) {
            fail(helper, "the PLSS should have used 1 mB (written back to the helmet), has " + fuel);
            return;
        }
        player.tickCount = 11;
        Breathing.checkForOxy(player, null);
        if (ItemModOxy.getFuel(ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.HEAD), ArmorModHandler.plate_only)) != 999) {
            fail(helper, "the PLSS uses oxygen only every 10 ticks");
            return;
        }
        // breathable air: the tank is not used
        player.tickCount = 20;
        Breathing.checkForOxy(player, new CBT_Atmosphere(Fluids.OXYGEN, 0.09D));
        if (ItemModOxy.getFuel(ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.HEAD), ArmorModHandler.plate_only)) != 999) {
            fail(helper, "the PLSS should not be used in breathable air");
            return;
        }
        // one unsealed piece: no tank
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(item("hbm:steel_boots")));
        if (Breathing.checkForOxy(player, null)) {
            fail(helper, "an unsealed suit should not breathe in vacuum");
            return;
        }
        Player creative = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        if (!Breathing.checkForOxy(creative, null)) {
            fail(helper, "creative players always breathe");
            return;
        }
        helper.succeed();
    }

    private static void fail(GameTestHelper helper, String message) {
        com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
