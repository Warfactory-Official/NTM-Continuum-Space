import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The M3 leftovers that are seen, on a real client, on a stone platform at y 100 (no gravity or orbit needed):
 * <ul>
 *   <li>the orrery ({@code ntmspace:orrery}, hbm-space {@code RenderOrrery}): the star cube over the block and the
 *       planets with their coloured orbit loops, up to ~49 blocks out (00-orrery-near from beside it, 01-orrery-above
 *       from 40 blocks up looking down);</li>
 *   <li>the vocaloid plushies, placed by hand from the item (Teto, Miku, Neru) and a fourth made FAT TETO with butter
 *       (02-plushies; the block-entity NBT says {@code "FATO"});</li>
 *   <li>the wild strawberries and wild mint on grass, the chain-link fence gate (one closed, one opened by hand) between
 *       HBM's chain-link fences (which join it), the oil barrel (HBM's barrel drum in {@code barrel_oily}) and the algae
 *       film's OBJ (03-flowers-gate-barrel-film);</li>
 *   <li>the plushie, orrery, algae film and barrel items in the hotbar (04-items).</li>
 * </ul>
 * Positive signals: the blocks are there, the opened gate reports {@code open=true}, the plushie's saved type is
 * {@code FATO}, and no exception names ntmspace, golem or {@code hbm:custom_item} in the log.
 */
public final class LeftoverVisuals implements Scenario {

    private static final int Y = 100, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // independent of the scenarios before it: alive, creative, in the overworld, no invasion left running on the Mun
        if (c.screenClass() != null && c.screenClass().contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { // a screen an earlier failure left open
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("ntmwar invasion stop mun");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 18000");
        c.cmd("gamerule doDaylightCycle false");

        int x = 0, z = 0;
        // radius 16: placeOnTop stands 2 blocks east of the fourth plushie (x 14.5); on a radius-12 platform that is over
        // the void of a freshly reset world and the player falls before placing it
        Fixtures.platform(c, x, Y, z, 16);

        // ---------------------------------------------------------------- the orrery
        Fixtures.setBlock(c, x, L, z, "ntmspace:orrery");
        Expect.block(c, x, L, z, "ntmspace:orrery");
        c.teleport(x + 6.5, L + 1, z + 6.5);
        c.lookAt(x + 0.5, L + 0.5, z + 0.5);
        c.waitTicks(20);
        c.screenshot("00-orrery-near");
        c.teleport(x + 0.5, L + 40, z + 0.5);
        c.lookAt(x + 0.5, L, z + 0.6);
        c.waitTicks(20);
        c.screenshot("01-orrery-above");

        // ---------------------------------------------------------------- plushies, placed by hand
        String[] plushies = {"ntmspace:plushie_teto", "ntmspace:plushie_miku", "ntmspace:plushie_neru", "ntmspace:plushie_teto"};
        int px = x + 6, pz = z - 6;
        for (int i = 0; i < plushies.length; i++) {
            Fixtures.placeOnTop(c, plushies[i], px + 2 * i, Y, pz);
            Expect.block(c, px + 2 * i, L, pz, "ntmspace:plushie");
        }
        // the fourth Teto eats butter: FAT TETO
        Fixtures.giveOnly(c, "ntmspace:butter", 1);
        c.teleport(px + 6.5, L + 1, pz + 2.5);
        c.waitTicks(4);
        // with the butter in hand (Fixtures.interact would switch to the empty slot 9)
        c.hotbar(1);
        c.lookAt(px + 6.5, L + 0.4, pz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
        String nbt = nbt(c, px + 6, L, pz);
        // the plushie saves its type by name, lower case ("teto" -> "fato")
        Expect.that(nbt.toLowerCase(java.util.Locale.ROOT).contains("type: \"fato\""), "butter made FAT TETO; nbt " + nbt);
        c.teleport(px + 3.5, L + 1.5, pz + 4.5);
        c.lookAt(px + 3.5, L + 0.3, pz + 0.5);
        c.waitTicks(20);
        c.screenshot("02-plushies");

        // ---------------------------------------------------------------- flowers, fence gate, barrel, algae film
        int fx = x - 8, fz = z + 6;
        for (int dx = 0; dx < 3; dx++) {
            Fixtures.setBlock(c, fx + dx, Y, fz, "minecraft:grass_block");
        }
        Fixtures.setBlock(c, fx, L, fz, "ntmspace:plant_flower_strawberry");
        Fixtures.setBlock(c, fx + 2, L, fz, "ntmspace:plant_flower_mint");
        Expect.block(c, fx, L, fz, "ntmspace:plant_flower_strawberry");
        Expect.block(c, fx + 2, L, fz, "ntmspace:plant_flower_mint");

        // a fence line along x with two gates (facing south: the panel runs along x)
        int gz = z + 9;
        Fixtures.setBlock(c, fx - 1, L, gz, "hbm:fence_metal");
        // placed by hand from the north, looking south (placeOnTop would place from the east, facing west)
        for (int dx = 0; dx < 2; dx++) {
            Fixtures.giveOnly(c, "ntmspace:fence_gate", 1);
            c.teleport(fx + dx + 0.5, L, gz - 2.5);
            c.waitTicks(2);
            c.lookAt(fx + dx + 0.5, Y + 0.98, gz + 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(3);
        }
        Fixtures.setBlock(c, fx + 2, L, gz, "hbm:fence_metal");
        Expect.block(c, fx, L, gz, "ntmspace:fence_gate");
        Expect.that(state(c, fx + 1, L, gz).contains("facing=south"), "the gates face south: " + state(c, fx + 1, L, gz));
        // opened empty-handed from the north (from the east the fence post at fx + 2 is in the way)
        c.teleport(fx + 1.5, L, gz - 2.5);
        c.waitTicks(2);
        c.hotbar(9);
        c.lookAt(fx + 1.5, L + 0.5, gz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        String gate = state(c, fx + 1, L, gz);
        Expect.that(gate.contains("open=true"), "the second gate opened by hand: " + gate);

        Fixtures.setBlock(c, fx + 4, L, fz, "ntmspace:oil_barrel");
        Fixtures.placeOnTop(c, "ntmspace:algae_film", fx + 6, Y, fz);
        Expect.block(c, fx + 4, L, fz, "ntmspace:oil_barrel");
        Expect.block(c, fx + 6, L, fz, "ntmspace:algae_film");

        c.teleport(fx + 2.5, L + 2, fz - 4.5);
        c.lookAt(fx + 2.5, L + 0.5, fz + 1.5);
        c.waitTicks(20);
        c.screenshot("03-flowers-gate-barrel-film");

        // ---------------------------------------------------------------- items
        c.cmd("clear");
        c.cmd("give @s ntmspace:plushie_teto");
        c.cmd("give @s ntmspace:plushie_fato");
        c.cmd("give @s ntmspace:orrery");
        c.cmd("give @s ntmspace:algae_film");
        c.cmd("give @s ntmspace:oil_barrel");
        c.cmd("give @s ntmspace:fence_gate");
        c.cmd("give @s ntmspace:plant_flower_strawberry");
        c.waitTicks(10);
        c.screenshot("04-items");

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|custom_item)");
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString() : "";
    }

    private static String state(Client c, int x, int y, int z) {
        JsonObject block = c.block(x, y, z);
        return block == null ? "" : block.toString();
    }
}
