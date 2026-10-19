package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code CelestialBodyTrait}: one piece of runtime state of a celestial body (its air, its water table, the
 * lights on its night side, an impact, a war...). A body starts with its default traits ({@link CelestialTraits}); once
 * anything changes them, the whole set is kept in the solar-system save ({@link SolarSystemSavedData}) by trait id.
 *
 * <p>Ids are 1.7's ({@code CelestialBodyTrait.registerTrait}): {@code atmosphere, temperature, bees, war, destroyed, water,
 * weather, battle, infected, dyson, impact, lights, invasion}. Each trait is saved by those ids with a DFU codec (fluids by
 * registry name, never 1.7's numeric fluid id) and synced to clients with {@link #writeToBytes}/{@link #readFromBytes},
 * as 1.7's perma-sync packet did. An addon registers its own trait with {@link #register}.
 */
public abstract class CelestialBodyTrait {

    /** A registered trait kind: its save id, class, empty factory and save codec (built lazily: fluid codecs need HBM's fluids). */
    public static final class Type<T extends CelestialBodyTrait> {
        public final String id;
        public final Class<T> type;
        private final Supplier<T> factory;
        private final Supplier<Codec<T>> codecFactory;
        private @Nullable Codec<T> codec;

        private Type(String id, Class<T> type, Supplier<T> factory, Supplier<Codec<T>> codec) {
            this.id = id;
            this.type = type;
            this.factory = factory;
            this.codecFactory = codec;
        }

        public T create() {
            return factory.get();
        }

        public synchronized Codec<T> codec() {
            if (codec == null) codec = codecFactory.get();
            return codec;
        }
    }

    private static final Map<String, Type<?>> BY_ID = new LinkedHashMap<>();
    private static final Map<Class<?>, Type<?>> BY_CLASS = new LinkedHashMap<>();

    /** Registers a trait kind under {@code id} (1.7 {@code registerTrait}); an id or class registered twice is an error. */
    public static synchronized <T extends CelestialBodyTrait> Type<T> register(String id, Class<T> type, Supplier<T> factory,
                                                                              Supplier<Codec<T>> codec) {
        if (BY_ID.containsKey(id) || BY_CLASS.containsKey(type)) {
            throw new IllegalStateException("celestial body trait " + id + " / " + type.getName() + " registered twice");
        }
        Type<T> entry = new Type<>(id, type, factory, codec);
        BY_ID.put(id, entry);
        BY_CLASS.put(type, entry);
        return entry;
    }

    public static synchronized @Nullable Type<?> byId(String id) {
        return BY_ID.get(id);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends CelestialBodyTrait> @Nullable Type<T> byClass(Class<T> type) {
        return (Type<T>) BY_CLASS.get(type);
    }

    /** Every registered kind, in registration order (1.7's {@code traitList}). */
    public static synchronized List<Type<?>> types() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    /** This trait's registered id. */
    public final String id() {
        Type<?> type = BY_CLASS.get(getClass());
        if (type == null) throw new IllegalStateException(getClass().getName() + " is not a registered celestial body trait");
        return type.id;
    }

    /**
     * A copy that shares no mutable state. 1.7 handed out its default trait objects themselves (and mutated them, e.g.
     * {@code emitGas} on a body that had no saved traits); the port copies, so a body's defaults stay the defaults for the
     * next world loaded in the same session.
     */
    public abstract CelestialBodyTrait copy();

    /** Client sync, 1.7's {@code writeToBytes} (the precision loss of floats is 1.7's, intentional). */
    public void writeToBytes(RegistryFriendlyByteBuf buf) {}

    public void readFromBytes(RegistryFriendlyByteBuf buf) {}

    /** 1.7 {@code update(isRemote, body)}: once per server tick (and client tick) for every trait of every body. */
    public void update(boolean remote, String body) {}

    // ---------------------------------------------------------------------------------------------------------------
    // 1.7's two "simple" traits, declared on CelestialBodyTrait itself

    /** 1.7 {@code CBT_BATTLEFIELD} ({@code battle}): a marker, set on Thatmo. */
    public static final class CBT_Battlefield extends CelestialBodyTrait {
        @Override public CelestialBodyTrait copy() { return new CBT_Battlefield(); }
    }

    /** 1.7 {@code CBT_COMPROMISED} ({@code infected}): a marker. */
    public static final class CBT_Compromised extends CelestialBodyTrait {
        @Override public CelestialBodyTrait copy() { return new CBT_Compromised(); }
    }
}
