package com.ntmspace.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.golem.core.dejson.Data;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Each body's own clock, and vanilla's {@code day} and {@code villager_schedule} timelines stretched to the body's solar
 * day ({@link SolarSystem#dayLength}, rounded), every marker and keyframe scaled to it. The dimension types name them.
 * Declared where the build's Minecraft has timelines; other nodes drop them from the dimension types.
 */
public final class BodyClockData {

    /** The bodies with their own clock ({@code SpaceSkies}); Thatmo keeps Kerbin's day. */
    private static final List<String> BODIES =
            List.of("mun", "minmus", "duna", "ike", "dres", "eve", "moho", "laythe", "tekto", "thatmo");

    private BodyClockData() {}

    public static void declare() {
        var day = vanilla("day");
        var schedule = vanilla("villager_schedule");
        if (day == null || schedule == null) return;
        for (String body : BODIES) {
            int period = (int) Math.floor(SolarSystem.dayLength(
                    SolarSystem.getBody(body.equals("thatmo") ? "kerbin" : body), 1) + 0.5D);
            Data.declare("data/ntmspace/world_clock/" + body + ".json", new JsonObject());
            Data.declare("data/ntmspace/timeline/" + body + "_day.json", stretch(day, body, period));
            Data.declare("data/ntmspace/timeline/" + body + "_villager_schedule.json", stretch(schedule, body, period));
        }
    }

    private static JsonObject vanilla(String name) {
        var stream = BodyClockData.class.getClassLoader().getResourceAsStream("data/minecraft/timeline/" + name + ".json");
        if (stream == null) return null;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static JsonObject stretch(JsonObject vanilla, String body, int period) {
        var json = vanilla.deepCopy();
        int previous = json.get("period_ticks").getAsInt();
        json.addProperty("clock", "ntmspace:" + body);
        json.addProperty("period_ticks", period);
        if (json.has("time_markers")) {
            for (var marker : json.getAsJsonObject("time_markers").entrySet()) {
                if (marker.getValue().isJsonObject()) {
                    var info = marker.getValue().getAsJsonObject();
                    info.addProperty("ticks", scale(info.get("ticks").getAsInt(), period, previous));
                } else {
                    marker.setValue(new JsonPrimitive(scale(marker.getValue().getAsInt(), period, previous)));
                }
            }
        }
        if (json.has("tracks")) {
            for (var track : json.getAsJsonObject("tracks").entrySet()) {
                for (var frame : track.getValue().getAsJsonObject().getAsJsonArray("keyframes")) {
                    var keyframe = frame.getAsJsonObject();
                    keyframe.addProperty("ticks", scale(keyframe.get("ticks").getAsInt(), period, previous));
                }
            }
        }
        return sorted(json).getAsJsonObject();
    }

    private static int scale(int ticks, int period, int previous) {
        return Math.min(period - 1, (int) Math.floor((double) ticks * period / previous + 0.5D));
    }

    /** Keys in order, as vanilla writes its files. */
    private static JsonElement sorted(JsonElement element) {
        if (element.isJsonArray()) {
            var array = new JsonArray();
            for (var each : element.getAsJsonArray()) array.add(sorted(each));
            return array;
        }
        if (!element.isJsonObject()) return element;
        Map<String, JsonElement> keys = new TreeMap<>();
        for (var entry : element.getAsJsonObject().entrySet()) keys.put(entry.getKey(), sorted(entry.getValue()));
        var object = new JsonObject();
        keys.forEach(object::add);
        return object;
    }
}
