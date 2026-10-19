import dev.golem.harness.api.*;

/**
 * NTM: Space's flora (M3 flora family) on a real client:
 *
 * <ul>
 *   <li>Laythe: a walled water pool on laythe silt holding every Laythe plant (seagrass, glowgrass, saltgrass, kelp,
 *       the five corals, one leaning on a coral block), screenshots 00 (close) and 01 (from above);</li>
 *   <li>Tekto: on rubber grass, the three short rubber plants, the three tall rubber plants (the three-block OBJ drawn
 *       by the block entity renderer), saplings, vinyl and PVC logs with their leaves and vinyl vines, planks, the joke
 *       logs, astroturf, rubber farmland with all five crops ripe, the DCM geyser (its cyan column) and the discharge
 *       geyser; screenshots 02-05;</li>
 *   <li>a hoe used on rubber grass makes rubber farmland; a seed planted by hand on it makes a crop;</li>
 *   <li>a PVC sapling on rubber grass grows its tree with bonemeal (right-clicks until the sapling's cell is a PVC log;
 *       bonemeal works 45 % of the time), screenshot 06.</li>
 * </ul>
 * Positive signals: block queries for the tilled farmland, the planted crop, the grown tree's log and the coral's
 * {@code facing}; a clean log.
 *
 * <p>Independent: the Tekto fixture is removed at the end. Its discharge geyser (1.7 {@code RenderVol2}) beams a hundred
 * blocks and more straight up whenever its charge runs out, so left behind it stood as blue streaks at the zenith of
 * {@code SkiesWeather}'s Tekto shots, six blocks from the camera.
 */
public final class FloraLaytheTekto implements Scenario {

    private static final int Y = 150;

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
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.cmd("effect give @s minecraft:night_vision 1000 0 true");
        c.cmd("time set noon");

        laythe(c);
        tekto(c);

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    private static void laythe(Client c) {
        c.cmd("ntmspace tp laythe");
        c.waitTicks(40);
        Expect.equal("ntmspace:laythe", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp laythe");
        c.cmd("tp @s 0 " + (Y + 2) + " -6");
        c.waitTicks(20);
        // a glass-walled pool, silt floor at Y, water Y+1..Y+4
        c.cmd("fill -6 " + (Y - 1) + " -6 6 " + (Y + 8) + " 6 minecraft:air");
        c.cmd("fill -5 " + (Y - 1) + " -2 5 " + (Y + 4) + " 4 minecraft:glass");
        c.cmd("fill -4 " + Y + " -1 4 " + Y + " 3 ntmspace:laythe_silt");
        c.cmd("fill -4 " + (Y + 1) + " -1 4 " + (Y + 4) + " 3 minecraft:water");
        c.waitTicks(5);
        int y = Y + 1;
        Fixtures.setBlock(c, -3, y, 0, "ntmspace:laythe_seagrass");
        Fixtures.setBlock(c, -2, y, 0, "ntmspace:laythe_glowgrass");
        Fixtures.setBlock(c, -1, y, 0, "ntmspace:plant_tall_laythe[half=lower]");
        Fixtures.setBlock(c, -1, y + 1, 0, "ntmspace:plant_tall_laythe[half=upper]");
        Fixtures.setBlock(c, 0, y, 0, "ntmspace:laythe_kelp[top=false]");
        Fixtures.setBlock(c, 0, y + 1, 0, "ntmspace:laythe_kelp[top=false]");
        Fixtures.setBlock(c, 0, y + 2, 0, "ntmspace:laythe_kelp[top=true]");
        String[] corals = {"tube", "brain", "bubble", "fire", "horn"};
        for (int i = 0; i < corals.length; i++) {
            Fixtures.setBlock(c, -3 + i * 2, y, 2, "ntmspace:laythe_coral[type=" + corals[i] + "]");
        }
        // a coral block with a coral beside it: the plant leans away from the block (placed with an update)
        Fixtures.setBlock(c, 3, y, 0, "ntmspace:laythe_coral_block[type=fire]");
        Fixtures.setBlock(c, 2, y + 1, 0, "minecraft:water");
        Fixtures.setBlock(c, 2, y, 0, "ntmspace:laythe_coral[type=fire]");
        // a neighbour change above: the coral recomputes its lean (east is checked before up)
        Fixtures.setBlock(c, 2, y + 1, 0, "minecraft:stone");
        Fixtures.setBlock(c, 2, y + 1, 0, "minecraft:water");
        c.waitTicks(5);
        String facing = c.block(2, y, 0).getAsJsonObject("properties").get("facing").getAsString();
        Expect.equal("west", facing, "a coral beside a coral block to its east leans west");

        c.teleport(0.5, y + 1, -4.5);
        c.lookAt(0.5, y + 0.5, 1.0);
        c.waitTicks(20);
        c.screenshot("00-laythe-flora-close");
        c.teleport(0.5, Y + 9, -1.5);
        c.look(0, 70);
        c.waitTicks(10);
        c.screenshot("01-laythe-flora-above");
    }

    private static void tekto(Client c) {
        c.cmd("ntmspace tp tekto");
        c.waitTicks(40);
        Expect.equal("ntmspace:tekto", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp tekto");
        c.cmd("tp @s 0 " + (Y + 2) + " -8");
        c.waitTicks(20);
        c.cmd("fill -10 " + (Y - 1) + " -10 10 " + (Y + 16) + " 10 minecraft:air");
        c.cmd("fill -9 " + Y + " -9 9 " + Y + " 9 ntmspace:rubber_grass");
        c.waitTicks(5);
        int y = Y + 1;
        String[] shorts = {"grass", "fern", "shrub"};
        String[] talls = {"cact", "bush", "flower"};
        for (int i = 0; i < 3; i++) {
            Fixtures.setBlock(c, -4 + i, y, -4, "ntmspace:rubber_plant[type=" + shorts[i] + "]");
            Fixtures.setBlock(c, -4 + i * 2, y, -2, "ntmspace:rubber_tall[type=" + talls[i] + "]");
        }
        Fixtures.setBlock(c, 2, y, -4, "ntmspace:sapling[type=vinyl]");
        Fixtures.setBlock(c, 3, y, -4, "ntmspace:sapling[type=pvc]");
        // woods
        String[] logs = {"pvc_log", "vinyl_log", "lattice_log", "bf_log", "primed_log", "eu_log"};
        for (int i = 0; i < logs.length; i++) Fixtures.setBlock(c, -5 + i, y, 2, "ntmspace:" + logs[i]);
        Fixtures.setBlock(c, 1, y, 2, "ntmspace:vinyl_planks");
        Fixtures.setBlock(c, 2, y, 2, "ntmspace:pvc_planks");
        Fixtures.setBlock(c, 3, y, 2, "ntmspace:astroturf");
        Fixtures.setBlock(c, -5, y + 1, 2, "ntmspace:rubber_leaves");
        Fixtures.setBlock(c, -4, y + 1, 2, "ntmspace:pet_leaves");
        Fixtures.setBlock(c, -5, y, 1, "ntmspace:vinyl_vine[south=true]");
        // farmland with the five ripe crops
        String[] crops = {"strawberry", "mint", "coffee", "tea", "paraffin"};
        for (int i = 0; i < crops.length; i++) {
            Fixtures.setBlock(c, -4 + i, Y, 5, "ntmspace:rubber_farmland[moisture=" + (i % 2) + "]");
            Fixtures.setBlock(c, -4 + i, y, 5, "ntmspace:crop_" + crops[i] + "[age=7]");
        }
        // geysers
        Fixtures.setBlock(c, 6, Y, 6, "ntmspace:geysir_chloric");
        Fixtures.setBlock(c, 6, Y, -6, "ntmspace:geysir_electric");
        c.waitTicks(10);

        c.teleport(-2.5, y, -8.5);
        c.lookAt(-2.5, y + 1.0, -2.0);
        c.waitTicks(20);
        c.screenshot("02-tekto-rubber-plants");
        c.teleport(-1.5, y + 1, -1.5);
        c.lookAt(-1.5, y + 0.5, 2.5);
        c.waitTicks(10);
        c.screenshot("03-tekto-woods");
        c.teleport(-2.0, y + 1, 1.5);
        c.lookAt(-2.0, y, 5.5);
        c.waitTicks(10);
        c.screenshot("04-tekto-crops");
        c.teleport(0.5, y + 2, 0.5);
        c.lookAt(6.5, y + 6, 6.5);
        c.waitTicks(40);
        c.screenshot("05-tekto-dcm-geyser");

        // a hoe on rubber grass makes rubber farmland; chlorinated seeds plant on it
        c.cmd("gamemode survival");
        c.cmd("fill 5 " + (y) + " -1 8 " + (y + 3) + " 2 minecraft:air");
        Fixtures.giveOnly(c, "minecraft:iron_hoe", 1);
        c.teleport(7.5, y, -0.5);
        c.lookAt(6.5, Y + 0.98, 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        Expect.block(c, 6, Y, 0, "ntmspace:rubber_farmland");
        Fixtures.giveOnly(c, "ntmspace:paraffin_seeds", 4);
        c.lookAt(6.5, Y + 0.98, 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
        Expect.block(c, 6, y, 0, "ntmspace:crop_paraffin");

        // grow a PVC sapling with bonemeal, in creative: a survival player on Tekto suffocates (no PLSS) and is pushed
        // about by the damage over the 40 tries, off the platform out of reach (bonemeal works the same in creative)
        c.cmd("gamemode creative");
        c.cmd("fill -9 " + y + " -9 -1 " + (y + 16) + " -6 minecraft:air");
        Fixtures.setBlock(c, -6, y, -8, "ntmspace:sapling[type=pvc]");
        Fixtures.giveOnly(c, "minecraft:bone_meal", 64);
        c.teleport(-3.5, y, -7.5);
        c.lookAt(-5.5, y + 0.3, -7.5);
        c.waitTicks(2);
        boolean grown = false;
        for (int i = 0; i < 40 && !grown; i++) {
            c.use(2);
            c.waitTicks(5);
            grown = c.block(-6, y, -8).get("block").getAsString().equals("ntmspace:pvc_log");
            if (!grown) c.lookAt(-5.5, y + 0.3, -7.5);
        }
        Expect.that(grown, "bonemeal grows the PVC sapling into a PVC tree");
        c.cmd("gamemode creative");
        c.teleport(-6.5, y + 1, -0.5);
        c.lookAt(-5.5, y + 6, -7.5);
        c.waitTicks(20);
        c.screenshot("06-tekto-grown-pvc-tree");

        // the fixture goes again (above all the two geysers, see the class comment); the PVC tree reaches about y + 12
        c.cmd("fill -10 " + (Y - 1) + " -10 10 " + (Y + 16) + " 10 minecraft:air");
        c.waitTicks(2);
        Expect.block(c, 6, Y, -6, "minecraft:air");
    }
}
