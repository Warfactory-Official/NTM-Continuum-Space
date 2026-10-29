import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** The solar renderer receives live server capability changes through the real client packet path. */
public final class SolarForeignArms implements Scenario {
    @Override
    public void run(Client client) throws Exception {
        client.waitInWorld(120_000L);
        client.cmd("gamemode creative");
        client.cmd("execute in minecraft:overworld run tp @s 60.5 101 -399.5");
        client.waitTicks(10);
        client.markLog();
        Fixtures.platform(client, 60, 100, -400, 6);
        Fixtures.placeOnTop(client, "ntmspace:machine_solar", 60, 100, -400);
        Expect.block(client, 60, 101, -400, "ntmspace:machine_solar");
        client.teleport(64.5, 104, -395.5);
        client.lookAt(60.5, 101.5, -399.5);
        client.waitTicks(5);
        var at = new JsonObject();
        at.addProperty("x", 60);
        at.addProperty("y", 101);
        at.addProperty("z", -400);
        at.addProperty("server", false);
        expectMask(client, at, 0);
        client.screenshot("00-solar-disconnected");
        try {
            client.cmd("setblock 60 101 -399 hbm:test_fe_cell");
            expectMask(client, at, 8);
            client.screenshot("01-solar-foreign-connected");
            client.cmd("setblock 60 101 -399 minecraft:air");
            expectMask(client, at, 0);
            client.screenshot("02-solar-foreign-removed");
        } finally {
            client.cmd("setblock 60 101 -399 minecraft:air");
        }
        Expect.logCleanSinceMark(client, "(?i)(exception|error).*(ntmspace|golem|solar)");
    }

    private static void expectMask(Client client, JsonObject at, int expected) {
        for (var attempt = 0; attempt < 40; attempt++) {
            var nbt = client.query("block_entity", at).get("nbt").getAsString().replaceAll("\\s+", "");
            if (nbt.matches("(?s).*\\bpower_connections:" + expected + "(?:[,}]).*")) return;
            if (attempt == 39) throw new AssertionError("Client solar mask did not reach " + expected + ": " + nbt);
            client.waitTicks(2);
        }
    }
}
