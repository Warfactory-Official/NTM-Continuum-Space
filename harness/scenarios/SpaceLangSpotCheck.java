import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Raw keys among NTM: Space's items and registries in the client's language, plus a sample of rendered names. */
public final class SpaceLangSpotCheck implements Scenario {

    private static final Set<String> RENAMED = Set.of("hbm:refueler", "hbm:machine_lpw2", "hbm:moon_turf", "hbm:fissure_bomb");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.waitTicks(10);
        List<String> out = new ArrayList<>();
        JsonObject reg = c.query("hbm_lang_audit", obj("registries", "ntmspace"));
        int raw = reg.getAsJsonArray("missing").size();
        out.add("registries checked=" + reg.get("checked") + " missing=" + reg.getAsJsonArray("missing"));
        for (String ns : new String[] {"ntmspace", "hbm"}) {
            for (int from = 0; ; from += 700) {
                JsonObject p = obj("stacks", ns);
                p.addProperty("from", from);
                p.addProperty("to", from + 700);
                p.addProperty("dump", true);
                JsonObject r = c.query("hbm_lang_audit", p);
                if (ns.equals("ntmspace")) {
                    for (JsonElement e : r.getAsJsonArray("flagged")) {
                        raw++;
                        out.add("RAW " + e);
                    }
                }
                int i = 0;
                for (JsonElement e : r.getAsJsonArray("dump")) {
                    JsonObject o = e.getAsJsonObject();
                    String id = o.get("id").getAsString();
                    if (ns.equals("ntmspace") ? i++ % 25 == 0 : RENAMED.contains(id)) out.add(id + " -> " + o.get("lines"));
                }
                if (from + 700 >= r.get("total").getAsInt()) break;
            }
        }
        Files.write(c.screenshotDir().resolve("space-lang.txt"), out, StandardCharsets.UTF_8);
        for (String s : out) System.out.println("[lang] " + s);
        Expect.that(raw <= 5, raw + " raw translation key(s), see space-lang.txt");
    }

    private static JsonObject obj(String mode, String namespace) {
        JsonObject o = new JsonObject();
        o.addProperty("mode", mode);
        o.addProperty("namespace", namespace);
        return o;
    }
}
