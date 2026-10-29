import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** Heavy boots worn on their own: right-click equips and swaps them, and the worn pair draws on the legs. */
public final class HeavyBoots implements Scenario {
    private static final String BOOTS = "ntmspace:heavy_boots";
    private static final int FEET_SLOT = 36;

    @Override
    public void run(Client client) throws Exception {
        client.waitInWorld(120_000L);
        client.markLog();
        Fixtures.platform(client, 300, 100, 300, 6);
        client.cmd("gamemode survival");
        Fixtures.giveOnly(client, BOOTS, 1);
        client.use();
        client.waitTicks(5);
        expectFeet(client, BOOTS);
        Expect.that(client.player().getAsJsonObject("main_hand").get("empty").getAsBoolean(),
                "the equipped boots left the hand");

        Fixtures.give(client, BOOTS, 1);
        client.use();
        client.waitTicks(5);
        expectFeet(client, BOOTS);
        Expect.that(BOOTS.equals(client.player().getAsJsonObject("main_hand").get("id").getAsString()),
                "a second pair swaps with the worn one, which lands in the hand");

        String previous = client.setOption("camera", "third_person_front");
        try {
            client.cmd("tp @s 300.5 101 300.5 0 0");
            client.waitTicks(10);
            client.screenshot("00-worn-third-person-front");
        } finally {
            client.setOption("camera", previous);
        }

        client.cmd("summon minecraft:armor_stand 300.5 101 303.5 {Rotation:[180f,0f]}");
        client.cmd("item replace entity @e[type=minecraft:armor_stand,limit=1,sort=nearest] armor.feet with " + BOOTS);
        client.cmd("tp @s 300.5 101 300.5 0 20");
        client.waitTicks(10);
        client.screenshot("01-armor-stand");
        client.cmd("kill @e[type=minecraft:armor_stand]");
        Expect.logCleanSinceMark(client, "(?i)(exception|error).*(ntmspace|golem|heavy|boots|armor)");
    }

    private static void expectFeet(Client client, String id) {
        for (var slot : client.inventory().getAsJsonArray("slots")) {
            JsonObject s = slot.getAsJsonObject();
            if (s.get("slot").getAsInt() == FEET_SLOT && id.equals(s.get("id").getAsString())) return;
        }
        throw new AssertionError("Feet slot " + FEET_SLOT + " does not hold " + id + ": " + client.inventory());
    }
}
