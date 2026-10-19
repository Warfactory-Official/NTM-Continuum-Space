package com.ntmspace.probe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What the probe answers, and where. Every probe listener passes (answers nothing, changes nothing) unless a test or
 * the harness has marked the position, level, entity or the hook itself here, so a dev run with the probe installed
 * plays as one without it. Also counts calls, the positive signal that a probe listener ran.
 */
public final class ProbeMarks {

    private static final Map<String, Object> MARKS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> CALLS = new ConcurrentHashMap<>();

    private ProbeMarks() {}

    private static String dim(Level level) {
        return dev.golem.core.Ids.keyId(level.dimension()).toString();
    }

    // ------------------------------------------------------------------------------------------------ marks

    public static void at(String hook, Level level, BlockPos pos, Object answer) {
        MARKS.put(hook + "|pos|" + dim(level) + "@" + pos.asLong(), answer);
    }

    public static @Nullable Object at(String hook, Level level, BlockPos pos) {
        return MARKS.isEmpty() ? null : MARKS.get(hook + "|pos|" + dim(level) + "@" + pos.asLong());
    }

    public static void level(String hook, ResourceKey<Level> dimension, Object answer) {
        MARKS.put(hook + "|level|" + dev.golem.core.Ids.keyId(dimension), answer);
    }

    public static @Nullable Object level(String hook, Level level) {
        return MARKS.isEmpty() ? null : MARKS.get(hook + "|level|" + dim(level));
    }

    public static void entity(String hook, Entity entity, Object answer) {
        MARKS.put(hook + "|entity|" + entity.getUUID(), answer);
    }

    public static @Nullable Object entity(String hook, Entity entity) {
        return MARKS.isEmpty() ? null : MARKS.get(hook + "|entity|" + entity.getUUID());
    }

    /** A mark for the whole hook (no position). */
    public static void flag(String hook, Object answer) {
        MARKS.put(hook + "|flag", answer);
    }

    public static @Nullable Object flag(String hook) {
        return MARKS.isEmpty() ? null : MARKS.get(hook + "|flag");
    }

    /** Forgets every mark of {@code hook}. */
    public static void clear(String hook) {
        MARKS.keySet().removeIf(key -> key.startsWith(hook + "|"));
    }

    // ------------------------------------------------------------------------------------------------ calls

    public static void call(String hook) {
        CALLS.computeIfAbsent(hook, k -> new AtomicLong()).incrementAndGet();
    }

    public static long calls(String hook) {
        AtomicLong count = CALLS.get(hook);
        return count == null ? 0 : count.get();
    }
}
