import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A world saved by NTM DE: Space itself (its dev server with Sable and Aeronautics: an airship on Kerbin and one on Duna,
 * a rocket with a design and a processed Mun drive), with satellites and a host player added in DE's shapes, copied into
 * the harness saves as {@code despace_real} and imported when the client joins. The host comes back on the Mun with the
 * PLSS and the linker, the satellites are listed in the levels they were filed in, and the rocket DE: Space built flies
 * from Kerbin to the Mun. {@code DESPACE_WORLD} names the world; 1.21.1 NeoForge only.
 */
public class DeSpaceRealWorld implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        try (Stream<Path> old = Files.list(saves)) {
            for (Path p : old.filter(p -> p.getFileName().toString().startsWith("despace_real")).toList()) delete(p);
        }
        copy(Path.of(System.getenv("DESPACE_WORLD")), saves.resolve("despace_real"));
        int before = c.log(20000, "DE import done").size();
        c.joinWorld("despace_real", 240000);
        c.waitInWorld(240000);
        c.waitTicks(60);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> doneLines = c.log(20000, "DE import done");
        Expect.that(doneLines.size() == before + 1, "this join imported the world once");
        String done = doneLines.get(doneLines.size() - 1);
        System.out.println(done);
        Expect.that(done.contains("dimensions moved 11"), "DE: Space's bodies moved: " + done);
        Expect.that(done.contains("airships converted 2"), "both airships converted: " + done);
        Expect.that(done.contains("satellites converted 4"), "the four satellites converted: " + done);

        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "the host logged out on the Mun and is back there");
        Expect.inventoryHolds(c, "ntmspace:oxy_plss", 1);
        Expect.inventoryHolds(c, "hbm:linker", 1);
        c.screenshot("00-host-on-mun");

        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        satellite(c, "minecraft:overworld", 70);
        satellite(c, "minecraft:overworld", 71);
        satellite(c, "ntmspace:dres", 72);
        satellite(c, "ntmspace:duna", 73);

        c.cmd("execute in minecraft:overworld run tp @s 13.5 -58 40.5");
        c.waitTicks(60);
        Expect.equal("minecraft:overworld", c.status().get("dimension").getAsString(), "on Kerbin beside DE: Space's rocket");
        JsonObject params = new JsonObject();
        params.addProperty("radius", 8);
        params.addProperty("type", "ntmspace:entity_rideable_rocket");
        params.addProperty("server", true);
        JsonArray found = c.query("entities", params).getAsJsonArray("entities");
        int rockets = 0;
        if (found != null) for (JsonElement e : found) if (e.getAsJsonObject().get("type").getAsString().equals("ntmspace:entity_rideable_rocket")) rockets++;
        Expect.that(rockets == 1, "DE: Space's rocket is NTM: Space's: " + found);
        c.screenshot("01-rocket-on-kerbin");

        c.markLog();
        c.hotbar(9);
        c.lookAt(10.5, -56.5, 40.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
        c.keyDown("key.jump");
        c.waitTicks(10);
        c.keyUp("key.jump");
        Expect.that(!c.logSinceMark("Rocket .* -> LAUNCHING").isEmpty(), "jump launches DE: Space's rocket: " + c.logSinceMark("Rocket"));
        c.key("key.togglePerspective");
        c.waitTicks(60);
        c.screenshot("02-launch");
        boolean transfer = false, landed = false;
        for (int i = 0; i < 240 && !landed; i++) {
            c.waitTicks(20);
            if (!transfer && !c.logSinceMark("Rocket .* -> TRANSFER").isEmpty()) {
                transfer = true;
                c.waitTicks(40);
                c.screenshot("03-transfer");
            }
            landed = !c.logSinceMark("Rocket .* -> LANDED in .*ntmspace:mun").isEmpty();
        }
        Expect.that(transfer, "the rocket went into transfer: " + c.logSinceMark("Rocket"));
        Expect.that(landed, "the rocket landed on the Mun, where its drive points: " + c.logSinceMark("Rocket"));
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "the pilot is on the Mun");
        c.screenshot("04-mun-landed");
        c.key("key.togglePerspective");
        c.key("key.togglePerspective");
        System.out.println("[despace rocket] " + String.join(" | ", c.logSinceMark("Rocket .* -> ")));
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|hbm)");
    }

    /** {@code /ntmsatellites list} in the level the satellite was filed in names its frequency. */
    private static void satellite(Client c, String dim, int freq) throws Exception {
        c.markLog();
        c.cmd("execute in " + dim + " run ntmsatellites list");
        c.waitTicks(10);
        Expect.that(!c.logSinceMark(freq + " - ").isEmpty(), "satellite " + freq + " is listed in " + dim + ": " + c.logSinceMark("ntmsatellites|satellite"));
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(dest);
                else if (!p.getFileName().toString().equals("session.lock")) Files.copy(p, dest);
            }
        }
    }

    private static void delete(Path path) throws IOException {
        try (Stream<Path> files = Files.walk(path)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }
}
