package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.ntmspace.NtmSpace;
import dev.golem.core.world.CompatSavedData;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code SolarSystemWorldSavedData} (the trait half; its orbital stations belong to the station port): the
 * trait overrides of every body whose traits changed, kept on the overworld so every body sees them. A body without an
 * entry has its default traits.
 *
 * <p>Saved as {@code bodies: {<body name>: {<trait id>: {...trait fields...}}}} - bodies by name, traits by their 1.7 id,
 * fluids by registry name. As in 1.7 a trait that fails to read is skipped (with one WARN) instead of losing the rest.
 */
public final class SolarSystemSavedData extends CompatSavedData<SolarSystemSavedData> {

    static final String ID = NtmSpace.MOD_ID + ":solar_system";

    private final Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap = new HashMap<>();

    public SolarSystemSavedData() {}

    public static SolarSystemSavedData get(ServerLevel overworld) {
        return CompatSavedData.get(overworld, ID, SolarSystemSavedData::new, CODEC);
    }

    public void setTraits(String bodyName, CelestialBodyTrait... traits) {
        if (traits.length == 0) {
            clearTraits(bodyName);
            return;
        }
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> newTraits = new HashMap<>();
        for (CelestialBodyTrait trait : traits) newTraits.put(trait.getClass(), trait);
        traitMap.put(bodyName, newTraits);
        setDirty();
    }

    public void clearTraits(String bodyName) {
        traitMap.remove(bodyName);
        setDirty();
    }

    /** The live traits of {@code bodyName}, or null when it has none saved (it then has its defaults). */
    public @Nullable Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(String bodyName) {
        return traitMap.get(bodyName);
    }

    /** Every body with saved traits and its live traits. */
    public Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> all() {
        return traitMap;
    }

    /** The whole save as NBT (the form the level storage writes); for tests and commands. */
    public static Tag toTag(SolarSystemSavedData data) {
        return CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
    }

    /** Reads a save written by {@link #toTag}. */
    public static SolarSystemSavedData fromTag(Tag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
    }

    @Override
    protected Codec<SolarSystemSavedData> codec() {
        return CODEC;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static final Codec<Map<String, Map<String, Dynamic<?>>>> RAW =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH));

    private static final Codec<SolarSystemSavedData> CODEC = RAW.fieldOf("bodies").codec()
            .xmap(SolarSystemSavedData::decode, SolarSystemSavedData::encode);

    private static SolarSystemSavedData decode(Map<String, Map<String, Dynamic<?>>> raw) {
        SolarSystemSavedData data = new SolarSystemSavedData();
        for (Map.Entry<String, Map<String, Dynamic<?>>> body : raw.entrySet()) {
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<>();
            for (Map.Entry<String, Dynamic<?>> saved : body.getValue().entrySet()) {
                CelestialBodyTrait trait = read(saved.getKey(), saved.getValue());
                if (trait != null) traits.put(trait.getClass(), trait);
            }
            data.traitMap.put(body.getKey(), traits);
        }
        return data;
    }

    private static Map<String, Map<String, Dynamic<?>>> encode(SolarSystemSavedData data) {
        Map<String, Map<String, Dynamic<?>>> raw = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> body : data.traitMap.entrySet()) {
            Map<String, Dynamic<?>> traits = new LinkedHashMap<>();
            for (CelestialBodyTrait trait : body.getValue().values()) {
                Dynamic<?> written = write(trait);
                if (written != null) traits.put(trait.id(), written);
            }
            raw.put(body.getKey(), traits);
        }
        return raw;
    }

    /** One trait from its saved form, or null (unknown id or unreadable data, one WARN). */
    public static @Nullable CelestialBodyTrait read(String id, Dynamic<?> data) {
        CelestialBodyTrait.Type<?> type = CelestialBodyTrait.byId(id);
        if (type == null) {
            NtmSpace.LOG.warn("Skipping unknown celestial body trait '{}' in the solar-system save", id);
            return null;
        }
        DataResult<? extends CelestialBodyTrait> result = type.codec().parse(data);
        return result.resultOrPartial(error -> NtmSpace.LOG.warn("Skipping unreadable celestial body trait '{}': {}", id, error))
                .orElse(null);
    }

    /** One trait's saved form (NBT). */
    @SuppressWarnings("unchecked")
    public static @Nullable Dynamic<?> write(CelestialBodyTrait trait) {
        CelestialBodyTrait.Type<CelestialBodyTrait> type =
                (CelestialBodyTrait.Type<CelestialBodyTrait>) CelestialBodyTrait.byClass(trait.getClass());
        if (type == null) return null;
        return type.codec().encodeStart(NbtOps.INSTANCE, trait)
                .resultOrPartial(error -> NtmSpace.LOG.warn("Could not save celestial body trait '{}': {}", type.id, error))
                .map(tag -> (Dynamic<?>) new Dynamic<Tag>(NbtOps.INSTANCE, tag))
                .orElse(null);
    }
}
