package com.ntmspace.trait;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.world.CelestialBodies;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceDimensions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code CelestialBody} trait methods: every body's traits at runtime. A body's defaults are hbm-space's
 * {@code SolarSystem.withTraits(...)} values; the first change stores the body's full trait set in
 * {@link SolarSystemSavedData}, which from then on is the body's truth (1.7's "trait overrides"). The server reads the
 * save; the client reads what the server last synced ({@link TraitSync}), falling back to the defaults, as 1.7's
 * {@code getTraitsUnsafe} did.
 *
 * <p>Bodies are named (1.7's {@code CelestialBody.name}). A level that is no body is Kerbin, as in 1.7
 * ({@code getBody(dimensionId)} fell back to dimension 0); Thatmo's level is Kerbin too (hbm-space gave Thatmo's body no
 * dimension, see {@link CelestialBodies#THATMO}).
 */
public final class CelestialTraits {

    private CelestialTraits() {}

    // ---------------------------------------------------------------------------------------------------------------
    // Trait kinds, 1.7 order (CelestialBodyTrait's static block)

    public static final CelestialBodyTrait.Type<CBT_Atmosphere> ATMOSPHERE =
            CelestialBodyTrait.register("atmosphere", CBT_Atmosphere.class, CBT_Atmosphere::new, CBT_Atmosphere::codec);
    public static final CelestialBodyTrait.Type<CBT_Temperature> TEMPERATURE =
            CelestialBodyTrait.register("temperature", CBT_Temperature.class, CBT_Temperature::new, CBT_Temperature::codec);
    public static final CelestialBodyTrait.Type<CBT_Bees> BEES =
            CelestialBodyTrait.register("bees", CBT_Bees.class, CBT_Bees::new, CBT_Bees::codec);
    public static final CelestialBodyTrait.Type<CBT_War> WAR =
            CelestialBodyTrait.register("war", CBT_War.class, CBT_War::new, CBT_War::codec);
    public static final CelestialBodyTrait.Type<CBT_Destroyed> DESTROYED =
            CelestialBodyTrait.register("destroyed", CBT_Destroyed.class, CBT_Destroyed::new, CBT_Destroyed::codec);
    public static final CelestialBodyTrait.Type<CBT_Water> WATER =
            CelestialBodyTrait.register("water", CBT_Water.class, CBT_Water::new, CBT_Water::codec);
    public static final CelestialBodyTrait.Type<CBT_Weather> WEATHER =
            CelestialBodyTrait.register("weather", CBT_Weather.class, CBT_Weather::new, CBT_Weather::codec);
    public static final CelestialBodyTrait.Type<CelestialBodyTrait.CBT_Battlefield> BATTLE =
            CelestialBodyTrait.register("battle", CelestialBodyTrait.CBT_Battlefield.class, CelestialBodyTrait.CBT_Battlefield::new,
                    () -> marker(CelestialBodyTrait.CBT_Battlefield::new));
    public static final CelestialBodyTrait.Type<CelestialBodyTrait.CBT_Compromised> INFECTED =
            CelestialBodyTrait.register("infected", CelestialBodyTrait.CBT_Compromised.class, CelestialBodyTrait.CBT_Compromised::new,
                    () -> marker(CelestialBodyTrait.CBT_Compromised::new));
    public static final CelestialBodyTrait.Type<CBT_Dyson> DYSON =
            CelestialBodyTrait.register("dyson", CBT_Dyson.class, CBT_Dyson::new, CBT_Dyson::codec);
    public static final CelestialBodyTrait.Type<CBT_Impact> IMPACT =
            CelestialBodyTrait.register("impact", CBT_Impact.class, CBT_Impact::new, CBT_Impact::codec);
    public static final CelestialBodyTrait.Type<CBT_Lights> LIGHTS =
            CelestialBodyTrait.register("lights", CBT_Lights.class, CBT_Lights::new, CBT_Lights::codec);
    public static final CelestialBodyTrait.Type<CBT_Invasion> INVASION =
            CelestialBodyTrait.register("invasion", CBT_Invasion.class, CBT_Invasion::new, CBT_Invasion::codec);

    private static <T extends CelestialBodyTrait> Codec<T> marker(java.util.function.Supplier<T> factory) {
        return MapCodec.unit(factory).codec();
    }

    /** hbm-space's {@code AstronomyUtil.MB_PER_ATM}: millibuckets of gas per atmosphere of pressure. */
    public static final double MB_PER_ATM = 1_000_000_000D * 1_000D;

    // ---------------------------------------------------------------------------------------------------------------
    // Bodies

    /** The body every level that is no body falls back to (1.7: dimension 0). */
    public static final String KERBIN = "kerbin";
    /** The star (1.7 {@code getStar}: the root of the tree). */
    public static final String KERBOL = "kerbol";

    private static final Map<String, ResourceKey<Level>> LEVELS = new LinkedHashMap<>();

    static {
        LEVELS.put("kerbin", Level.OVERWORLD);
        LEVELS.put("mun", SpaceDimensions.MUN);
        LEVELS.put("minmus", SpaceDimensions.MINMUS);
        LEVELS.put("moho", SpaceDimensions.MOHO);
        LEVELS.put("eve", SpaceDimensions.EVE);
        LEVELS.put("duna", SpaceDimensions.DUNA);
        LEVELS.put("ike", SpaceDimensions.IKE);
        LEVELS.put("dres", SpaceDimensions.DRES);
        LEVELS.put("laythe", SpaceDimensions.LAYTHE);
        LEVELS.put("tekto", SpaceDimensions.TEKTO);
        // thatmo: no level of its own in hbm-space (its dimension resolves to Kerbin)
    }

    /** The body {@code level} is (Kerbin for any level that is no body). */
    public static String bodyOf(Level level) {
        CelestialBodies.Body body = CelestialBodies.of(level);
        return body == null ? KERBIN : body.name();
    }

    /** The body a level id ({@code "ntmspace:duna"}) is. */
    public static String bodyOfLevelId(String levelId) {
        for (Map.Entry<String, ResourceKey<Level>> entry : LEVELS.entrySet()) {
            if (dev.golem.core.Ids.keyId(entry.getValue()).toString().equals(levelId)) return entry.getKey();
        }
        return KERBIN;
    }

    /** The level of {@code body} if it has one and it is loaded, else null. */
    public static @Nullable ServerLevel levelOf(String body) {
        ResourceKey<Level> key = LEVELS.get(body);
        MinecraftServer server = CelestialTraits.server;
        return key == null || server == null ? null : server.getLevel(key);
    }

    /** The level key of {@code body}, or null for a body without a level. */
    public static @Nullable ResourceKey<Level> levelKeyOf(String body) {
        return LEVELS.get(body);
    }

    /** 1.7 {@code getStar}: the root of the body's tree. */
    public static String starOf(Level level) {
        SolarSystem.Body body = SolarSystem.body(bodyOf(level));
        return body == null ? KERBOL : body.getStar().name;
    }

    /** 1.7 {@code getPlanet}: the body itself for a planet, its planet for a moon. */
    public static String planetOf(String body) {
        SolarSystem.Body b = SolarSystem.body(body);
        return b == null ? body : b.getPlanet().name;
    }

    /** Every body of the solar system (1.7 {@code getAllBodies}). */
    public static List<String> allBodies() {
        List<String> names = new ArrayList<>();
        collect(SolarSystem.KERBOL, names);
        return names;
    }

    private static void collect(SolarSystem.Body body, List<String> into) {
        into.add(body.name);
        for (SolarSystem.Body satellite : body.satellites) collect(satellite, into);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Defaults (hbm-space SolarSystem.withTraits)

    private static volatile Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> defaults;

    private static Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> defaults() {
        Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> map = defaults;
        if (map != null) return map;
        map = new HashMap<>();
        put(map, "moho", new CBT_Temperature(200));
        put(map, "eve", new CBT_Atmosphere(SpaceFluids.EVEAIR, 5D), new CBT_Temperature(400), new CBT_Water(Fluids.MERCURY));
        put(map, "kerbin", new CBT_Atmosphere(SpaceFluids.EARTHAIR, 1D), new CBT_Water());
        put(map, "minmus", new CBT_Water(SpaceFluids.MILK));
        put(map, "duna", new CBT_Atmosphere(SpaceFluids.DUNAAIR, 0.1D));
        put(map, "ike", new CBT_Water(SpaceFluids.BROMINE));
        put(map, "laythe", new CBT_Atmosphere(SpaceFluids.EARTHAIR, 0.45D).and(Fluids.XENON, 0.15D), new CBT_Water());
        // 1.5F / 0.005F: hbm-space passed float literals to the double constructor; widened exactly as there
        put(map, "tekto", new CBT_Atmosphere(SpaceFluids.TEKTOAIR, 1.5F), new CBT_Water(SpaceFluids.CCL));
        put(map, "thatmo", new CBT_Atmosphere(SpaceFluids.NITROGEN, 0.005F), new CelestialBodyTrait.CBT_Battlefield());
        defaults = map;
        return map;
    }

    private static void put(Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> map, String body,
                            CelestialBodyTrait... traits) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> set = new HashMap<>();
        for (CelestialBodyTrait trait : traits) set.put(trait.getClass(), trait);
        map.put(body, Collections.unmodifiableMap(set));
    }

    /** 1.7 {@code hasDefaultTrait}. */
    public static boolean hasDefaultTrait(String body, Class<? extends CelestialBodyTrait> trait) {
        return defaults().getOrDefault(body, Map.of()).containsKey(trait);
    }

    /** 1.7 {@code getDefaultTrait}: a copy of the body's default trait, or null. */
    @SuppressWarnings("unchecked")
    public static <T extends CelestialBodyTrait> @Nullable T getDefaultTrait(String body, Class<T> trait) {
        CelestialBodyTrait value = defaults().getOrDefault(body, Map.of()).get(trait);
        return value == null ? null : (T) value.copy();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Server and client stores

    private static volatile @Nullable MinecraftServer server;
    private static volatile @Nullable SolarSystemSavedData data;
    private static volatile Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> client = Map.of();
    private static volatile boolean changed;

    static void serverStarted(MinecraftServer started) {
        server = started;
        data = null;
        changed = true;
    }

    static void serverStopping() {
        server = null;
        data = null;
    }

    /** The server whose solar system is current (null on a client without one). */
    public static @Nullable MinecraftServer server() {
        return server;
    }

    /** Makes {@code level}'s server current if none is yet (a GameTest or a hook before the start notification). */
    public static void attach(Level level) {
        if (server == null && level instanceof ServerLevel serverLevel) server = serverLevel.getServer();
    }

    private static @Nullable SolarSystemSavedData data() {
        SolarSystemSavedData current = data;
        if (current != null) return current;
        MinecraftServer s = server;
        if (s == null) return null;
        current = SolarSystemSavedData.get(s.overworld());
        data = current;
        return current;
    }

    /** Client: what the server synced (1.7 {@code updateClientTraits}). */
    static void receive(Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits) {
        client = traits == null ? Map.of() : traits;
    }

    /**
     * Client: the synced air of {@code body} as {@link com.ntmspace.world.Atmospheres} entries (registry names), or null
     * when the server synced nothing for the body (it is on its defaults).
     */
    public static @Nullable List<com.ntmspace.world.Atmospheres.Entry> clientAtmosphere(String body) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = client.get(body);
        if (traits == null) return null;
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) traits.get(CBT_Atmosphere.class);
        List<com.ntmspace.world.Atmospheres.Entry> entries = new ArrayList<>();
        if (atmosphere != null) {
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                entries.add(new com.ntmspace.world.Atmospheres.Entry(entry.fluid.getRegistryName(), entry.pressure));
            }
        }
        return entries;
    }

    /** Something changed: save it and sync it to the clients at the end of the tick. */
    public static void markChanged() {
        changed = true;
        SolarSystemSavedData current = data;
        if (current != null) current.setDirty();
    }

    static boolean takeChanged() {
        boolean was = changed;
        changed = false;
        return was;
    }

    /** The saved traits of every body that has any (server). */
    static Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> savedTraits() {
        SolarSystemSavedData current = data();
        return current == null ? Map.of() : current.all();
    }

    /** Don't modify traits returned from this (1.7 {@code getTraitsUnsafe}). */
    private static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traitsUnsafe(String body, boolean clientSide) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits;
        if (clientSide) {
            traits = client.get(body);
        } else {
            SolarSystemSavedData current = data();
            traits = current == null ? null : current.getTraits(body);
        }
        if (traits != null) return traits;
        return defaults().getOrDefault(body, Map.of());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 1.7 CelestialBody trait API

    public static boolean hasTrait(String body, Class<? extends CelestialBodyTrait> trait, boolean clientSide) {
        return traitsUnsafe(body, clientSide).containsKey(trait);
    }

    @SuppressWarnings("unchecked")
    public static <T extends CelestialBodyTrait> @Nullable T getTrait(String body, Class<T> trait, boolean clientSide) {
        return (T) traitsUnsafe(body, clientSide).get(trait);
    }

    /** 1.7 {@code CelestialBody.hasTrait(world, trait)}. */
    public static boolean hasTrait(Level level, Class<? extends CelestialBodyTrait> trait) {
        attach(level);
        return hasTrait(bodyOf(level), trait, level.isClientSide());
    }

    /** 1.7 {@code CelestialBody.getTrait(world, trait)}: the live trait; don't modify it, use {@link #modifyTraits}. */
    public static <T extends CelestialBodyTrait> @Nullable T getTrait(Level level, Class<T> trait) {
        attach(level);
        return getTrait(bodyOf(level), trait, level.isClientSide());
    }

    /**
     * 1.7 {@code getTraits}: the body's traits, safe to add to and remove from (then pass to {@link #setTraits}). The trait
     * objects are the live saved ones when the body has saved traits, copies of its defaults otherwise.
     */
    public static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(String body) {
        SolarSystemSavedData current = data();
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> saved = current == null ? null : current.getTraits(body);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<>();
        if (saved != null) {
            traits.putAll(saved);
        } else {
            for (CelestialBodyTrait trait : defaults().getOrDefault(body, Map.of()).values()) traits.put(trait.getClass(), trait.copy());
        }
        return traits;
    }

    public static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(Level level) {
        attach(level);
        return getTraits(bodyOf(level));
    }

    public static void setTraits(String body, CelestialBodyTrait... traits) {
        SolarSystemSavedData current = data();
        if (current == null) return;
        current.setTraits(body, traits);
        markChanged();
    }

    public static void setTraits(String body, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        setTraits(body, traits.values().toArray(new CelestialBodyTrait[0]));
    }

    public static void setTraits(Level level, CelestialBodyTrait... traits) {
        attach(level);
        setTraits(bodyOf(level), traits);
    }

    public static void modifyTraits(String body, CelestialBodyTrait... traits) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        for (CelestialBodyTrait trait : traits) currentTraits.put(trait.getClass(), trait);
        setTraits(body, currentTraits);
    }

    public static void modifyTraits(Level level, CelestialBodyTrait... traits) {
        attach(level);
        modifyTraits(bodyOf(level), traits);
    }

    public static void clearTraits(String body) {
        SolarSystemSavedData current = data();
        if (current == null) return;
        current.clearTraits(body);
        markChanged();
    }

    /** "he has ceased to be": the body loses its atmosphere. */
    public static void degas(String body) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        currentTraits.remove(CBT_Atmosphere.class);
        setTraits(body, currentTraits);
    }

    /** 1.7 {@code consumeGas}: takes {@code amount} mB of {@code fluid} out of the body's air; false if it had none. */
    public static boolean consumeGas(String body, FluidType fluid, double amount) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        if (atmosphere == null) return false;

        boolean didConsume = false;
        int emptyIndex = -1;
        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            CBT_Atmosphere.FluidEntry entry = atmosphere.fluids.get(i);
            if (entry.fluid == fluid) {
                entry.pressure -= amount / MB_PER_ATM;
                didConsume = true;
                emptyIndex = entry.pressure <= 0 ? i : -1;
                break;
            }
        }
        if (emptyIndex >= 0) {
            atmosphere.fluids.remove(emptyIndex);
            if (atmosphere.fluids.isEmpty()) currentTraits.remove(CBT_Atmosphere.class);
        }
        setTraits(body, currentTraits);
        return didConsume;
    }

    /** 1.7 {@code emitGas}: adds {@code amount} mB of {@code fluid} to the body's air (at most eight gases, the thinnest goes). */
    public static void emitGas(String body, FluidType fluid, double amount) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        if (atmosphere == null) {
            atmosphere = new CBT_Atmosphere();
            currentTraits.put(CBT_Atmosphere.class, atmosphere);
        }
        boolean hasFluid = false;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry.fluid == fluid) {
                entry.pressure += amount / MB_PER_ATM;
                hasFluid = true;
                break;
            }
        }
        if (!hasFluid) {
            // Sort existing fluids and remove the lowest fraction
            if (atmosphere.fluids.size() >= 8) {
                atmosphere.sortDescending();
                atmosphere.fluids.remove(atmosphere.fluids.size() - 1);
            }
            atmosphere.fluids.add(new CBT_Atmosphere.FluidEntry(fluid, amount / MB_PER_ATM));
        }
        setTraits(body, currentTraits);
    }

    /** 1.7 {@code FT_Gaseous.release}: a gas vented into the air of {@code level}'s body (gases only). */
    public static void release(Level level, FluidType type, double mB) {
        if (level.isClientSide() || !isGas(type)) return;
        attach(level);
        emitGas(bodyOf(level), type, mB);
    }

    /** 1.7 {@code FT_Gaseous.capture}: a gas taken out of the air of {@code level}'s body (gases only). */
    public static void capture(Level level, FluidType type, double mB) {
        if (level.isClientSide() || !isGas(type)) return;
        attach(level);
        consumeGas(bodyOf(level), type, mB);
    }

    public static boolean isGas(FluidType type) {
        return type.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous.class)
                || type.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART.class);
    }

    /** 1.7 {@code CelestialBody.damage}: war damage to a body, shield first. */
    public static void damage(int dmg, String body) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        CBT_War war = (CBT_War) currentTraits.get(CBT_War.class);
        if (war == null) {
            war = new CBT_War();
            currentTraits.put(CBT_War.class, war);
        }
        if (war.shield > 0) {
            war.shield -= dmg;
        } else {
            war.health -= dmg;
        }
        setTraits(body, currentTraits);
    }

    /**
     * 1.7 {@code updateChemistry}, once per second per level: steam in the air above 0.2 atm in total makes a water table,
     * and the air reacts through the {@code ntmspace:atmosphere} table ({@link com.ntmspace.recipe.AtmosphereRecipes}).
     *
     * <p>Odd but 1.7's: the trait map is read once before the reactions, and when anything happened it is written back
     * afterwards. On a body whose traits were never saved that map holds copies of the defaults, so the write-back undoes
     * that first reaction's gas change (the reaction itself is then saved and runs again a second later); on a saved body
     * the map holds the live traits the reactions changed, and nothing is lost.
     */
    public static void updateChemistry(Level level) {
        attach(level);
        String body = bodyOf(level);
        boolean hasUpdated = false;
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(body);
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        CBT_Water water = (CBT_Water) currentTraits.get(CBT_Water.class);
        if (water == null && atmosphere != null) {
            double pressure = 0;
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                if (entry.fluid == Fluids.STEAM || entry.fluid == Fluids.HOTSTEAM || entry.fluid == Fluids.SUPERHOTSTEAM
                        || entry.fluid == Fluids.ULTRAHOTSTEAM || entry.fluid == Fluids.SPENTSTEAM) {
                    pressure += entry.pressure;
                }
            }
            if (pressure > 0.2D) {
                currentTraits.put(CBT_Water.class, new CBT_Water());
                hasUpdated = true;
            }
        }
        if (atmosphere != null) {
            for (com.ntmspace.recipe.AtmosphereRecipes.Reaction reaction : com.ntmspace.recipe.AtmosphereRecipes.INSTANCE.reactions()) {
                if (reactAtmosphere(level, body, reaction)) hasUpdated = true;
            }
        }
        if (hasUpdated) setTraits(body, currentTraits);
    }

    /**
     * 1.7 {@code reactAtmosphere}: because the chemistry runs only once a second, a reaction runs 64 times at once, when
     * the air holds 64 times each input; the inputs are captured and the product released (gases only).
     */
    public static boolean reactAtmosphere(Level level, String body, com.ntmspace.recipe.AtmosphereRecipes.Reaction reaction) {
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) getTraits(body).get(CBT_Atmosphere.class);
        if (atmosphere == null) return false; // 1.7 would fail here after a reaction emptied the air; none can
        int scale = 64;
        for (com.hbm.inventory.fluid.FluidStack input : reaction.inputs()) {
            boolean hasInput = false;
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                if (entry.fluid == input.type && entry.pressure * MB_PER_ATM >= (double) input.fill * scale) hasInput = true;
            }
            if (!hasInput) return false;
        }
        // 1.7 FT_Gaseous.capture / release on the world, i.e. on {@code body} (named here so a test can use any body)
        if (level.isClientSide()) return true;
        for (com.hbm.inventory.fluid.FluidStack input : reaction.inputs()) {
            if (isGas(input.type)) consumeGas(body, input.type, (double) input.fill * scale);
        }
        if (isGas(reaction.output().type)) emitGas(body, reaction.output().type, (double) reaction.output().fill * scale);
        return true;
    }

    /** 1.7 {@code updateSwarms}: once per tick, the star's Dyson swarms decay. */
    public static void updateSwarms() {
        CBT_Dyson dyson = getTrait(KERBOL, CBT_Dyson.class, false);
        if (dyson == null) return;
        // 1.7 decayed the live trait and resent every trait each tick; the port saves and syncs on a change
        if (dyson.attenuate()) markChanged();
    }

    /** Server tick (1.7 {@code ModEventHandler.onServerTick}, phase START): weather, every trait's update, swarms. */
    static void serverTick(MinecraftServer tickServer) {
        if (server != tickServer) serverStarted(tickServer);
        CBT_Weather.updateGlobalWeather(tickServer.getTickCount());
        for (String body : allBodies()) {
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> saved = savedTraits().get(body);
            if (saved == null) continue; // a body on its defaults has nothing that ticks
            for (CelestialBodyTrait trait : new ArrayList<>(saved.values())) trait.update(false, body);
        }
        updateSwarms();
    }

    /** Client tick: 1.7 ran {@code update(true, body)} on the synced traits. */
    static void clientTick() {
        for (Map.Entry<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> body : client.entrySet()) {
            for (CelestialBodyTrait trait : new ArrayList<>(body.getValue().values())) trait.update(true, body.getKey());
        }
    }
}
