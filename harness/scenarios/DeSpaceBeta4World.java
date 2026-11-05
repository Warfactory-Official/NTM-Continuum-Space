import com.google.gson.JsonObject;
import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A world DE beta.14's server wrote with the DE: Space beta.4 jar: every DE and DE: Space block, item and entity, and a
 * chest of fuses and Create motors on the Mun at (8, 70, 8), copied in as {@code despace_beta4} from
 * {@code DESPACE_WORLD_4}. Both mods' data must import in one run. 1.21.1 NeoForge only.
 */
public class DeSpaceBeta4World implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        Path world = saves.resolve("despace_beta4");
        if (Files.exists(world)) delete(world);
        copy(Path.of(System.getenv("DESPACE_WORLD_4")), world);
        int before = c.log(20000, "DE import done").size();
        c.markLog();
        c.joinWorld("despace_beta4", 600000);
        c.waitInWorld(600000);
        c.waitTicks(100);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> done = c.log(20000, "DE import done");
        Expect.that(done.size() == before + 1, "this join imported the world once");
        String summary = done.get(done.size() - 1);
        System.out.println(summary);
        Expect.that(summary.contains("dimensions moved 11"), "DE: Space's bodies moved: " + summary);
        Expect.logCleanSinceMark(c, "/(ERROR|WARN)[]].*(Failed to load|Skipping BlockEntity|Unknown structure|invalid item|Tried to load)");

        c.cmd("gamemode creative");
        c.cmd("execute in ntmspace:mun run tp @s 8.5 72 8.5");
        c.waitTicks(60);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "DE: Space's Mun is NTM: Space's");
        JsonObject be = c.blockEntity(8, 70, 8);
        String chest = be.has("nbt") ? be.get("nbt").getAsString() : "";
        Expect.that(chest.contains("\"hbm:unstable_timer\": 42"), "the Mun's chest moved with its fuse: " + chest);
        Expect.that(!chest.contains("unstable_at") && !chest.contains("create_motor"), "its stacks converted: " + chest);
        c.cmd("execute in minecraft:overworld run tp @s 0 -50 0");
        c.leaveWorld(60000);
        c.joinWorld("harness", 180000);
        c.waitInWorld(180000);
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
    }

    private static void delete(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
