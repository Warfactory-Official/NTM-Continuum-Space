import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * hbm-space's small changes to base-HBM classes (fork content part 3, {@code ForkBaseChanges}) on a real client:
 *
 * <ul>
 *   <li>wall placement: a chemical plant placed against the north face of a stone pillar stands against it, facing
 *       north, its core one block in front of the clicked face (00-wall-placement); positive signal: the core block.</li>
 *   <li>ceiling placement: a chemical plant placed under a stone slab hangs from it, top at the placed block, facing the
 *       player (01-ceiling-placement); positive signal: the core block.</li>
 *   <li>the refueler's look overlay: its title and the tank's fluid name in the fluid's colour (02-refueler-overlay).</li>
 *   <li>a primed charge tumbling (03/04-tnt-tumble, two frames ten ticks apart).</li>
 *   <li>{@code ntmspace:capacitor_complex} on the floor, cadmium-plated (05-capacitor-complex). Its tooltip order is
 *       covered by the GameTest {@code fork_base_machines}.</li>
 *   <li>neutron activation, off and on (06/07 tooltips, {@code space_neutron_check} log lines); see
 *       {@link #neutronActivation}.</li>
 *   <li>hbm-space's dosimeter click gate and ash-glasses cloud rate (08 radon through the glasses); see
 *       {@link #dosimeterAndGlasses}.</li>
 * </ul>
 */
public final class ForkContent3 implements Scenario {

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
        c.cmd("effect clear @s");
        c.cmd("time set day");
        c.cmd("gamerule doDaylightCycle false");

        JsonObject pos = c.player();
        int x = pos.get("block_x").getAsInt() + 8, y = pos.get("block_y").getAsInt() + 24, z = pos.get("block_z").getAsInt();
        Fixtures.platform(c, x, y, z, 14);

        // ---------------------------------------------------------------- wall
        for (int dy = 1; dy <= 4; dy++) Fixtures.setBlock(c, x, y + dy, z + 4, "minecraft:stone");
        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.teleport(x + 2.5, y + 1, z + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, y + 2.5, z + 3.98); // the pillar's north face
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        Expect.block(c, x, y + 2, z + 2, "hbm:machine_chemical_plant");
        c.teleport(x + 6.5, y + 3, z - 3.5);
        c.lookAt(x + 0.5, y + 3, z + 2.5);
        c.waitTicks(20);
        c.screenshot("00-wall-placement");

        // ---------------------------------------------------------------- ceiling
        int cx = x + 10;
        Fixtures.setBlock(c, cx, y + 5, z, "minecraft:stone");
        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.teleport(cx + 2.5, y + 1, z + 0.5);
        c.waitTicks(2);
        c.lookAt(cx + 0.5, y + 5.02, z + 0.5); // just inside the stone at y + 5, entering through its bottom face
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        // facing the player (east), core one block west of the placed block, top under the stone
        Expect.block(c, cx - 1, y + 2, z, "hbm:machine_chemical_plant");
        c.teleport(cx + 6.5, y + 2, z + 6.5);
        c.lookAt(cx - 0.5, y + 3, z + 0.5);
        c.waitTicks(20);
        c.screenshot("01-ceiling-placement");

        // ---------------------------------------------------------------- refueler overlay
        int rx = x - 6, rz = z - 6;
        Fixtures.setBlock(c, rx, y + 1, rz, "hbm:refueler");
        c.cmd("clear @s");
        c.hotbar(9);
        c.teleport(rx + 2.5, y + 1, rz + 0.5);
        c.lookAt(rx + 0.5, y + 1.4, rz + 0.5);
        c.waitTicks(10);
        c.screenshot("02-refueler-overlay");

        // ---------------------------------------------------------------- tumbling charge
        int tx = x - 6, tz = z + 6;
        c.teleport(tx + 4.5, y + 2, tz + 0.5);
        c.lookAt(tx + 0.5, y + 2, tz + 0.5);
        c.cmd("summon hbm:tnt_ntm " + (tx + 0.5) + " " + (y + 1) + " " + (tz + 0.5));
        c.waitTicks(10);
        c.screenshot("03-tnt-tumble");
        c.waitTicks(10);
        c.screenshot("04-tnt-tumble");
        c.cmd("kill @e[type=hbm:tnt_ntm]");

        // ---------------------------------------------------------------- capacitor_complex
        JsonObject cap = Fixtures.placeOnTop(c, "ntmspace:capacitor_complex", x - 3, y, z);
        System.out.println("[fork3] capacitor: " + cap);
        Expect.block(c, x - 3, y + 1, z, "ntmspace:capacitor_complex");
        c.teleport(x - 0.5, y + 2, z + 2.5);
        c.lookAt(x - 2.5, y + 1.5, z + 0.5);
        c.waitTicks(10);
        c.screenshot("05-capacitor-complex");

        neutronActivation(c);
        dosimeterAndGlasses(c, x, y, z);

        java.util.List<String> errors = c.logSinceMark("Exception|ERROR");
        System.out.println("[fork3] log lines matching Exception/ERROR: " + errors.size());
    }

    /**
     * hbm-space's neutron activation ({@code com.ntmspace.fork.neutron.NeutronActivation}, switch
     * {@code HAZ_01_disableNeutrons} in {@code config/ntmspace.cfg}, flipped here for the running game with
     * {@code /ntmspace neutron on|off}): off (the default) a carried Po-210/Be billet activates nothing; on, every stack
     * in the inventory gains activation and turns radioactive. Positive signals: the {@code space_neutron_check} log lines
     * (stone activation exactly 0.0 off, above 0 on); screenshots of the activated stone's tooltip ("[Radioactive]",
     * twice its activation in RAD/s, 06) and the billet's ("[Neutron Radiation]", "11.25RAD/s^2", 07). Creative mode keeps
     * the player alive; items are activated in any game mode, as in hbm-space. The switch is left off.
     */
    private static void neutronActivation(Client c) throws Exception {
        c.cmd("ntmspace neutron off");
        Fixtures.giveOnly(c, "hbm:billet_po210be", 1); // hotbar 0 = inventory menu slot 36
        c.cmd("give @s minecraft:stone 16");             // hotbar 1 = menu slot 37
        c.waitTicks(40);
        c.markLog();
        c.cmd("ntmspace neutron check");
        c.waitTicks(5);
        String off = String.join("\n", c.logSinceMark("space_neutron_check"));
        System.out.println("[fork3] neutron off: " + off);
        Expect.that(off.contains("enabled=false") && off.contains("minecraft:stonex16=0.0"),
                "with the switch off nothing is activated: " + off);

        c.cmd("ntmspace neutron on");
        c.waitTicks(60); // the carried scan runs every hazardRate (5) ticks
        c.markLog();
        c.cmd("ntmspace neutron check");
        c.waitTicks(5);
        String on = String.join("\n", c.logSinceMark("space_neutron_check"));
        System.out.println("[fork3] neutron on: " + on);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("minecraft:stonex16=([0-9.Ee-]+)").matcher(on);
        Expect.that(on.contains("enabled=true") && m.find() && Double.parseDouble(m.group(1)) > 0,
                "with the switch on the carried stone is activated: " + on);

        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(5);
        JsonObject stone = c.slot(37), billet = c.slot(36);
        Expect.that(stone != null && billet != null, "inventory slots 36/37 not found");
        c.mouseMove(stone.get("center_x").getAsDouble(), stone.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.screenshot("06-neutron-stone-tooltip");
        c.mouseMove(billet.get("center_x").getAsDouble(), billet.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.screenshot("07-neutron-billet-tooltip");
        c.key("escape");
        c.waitTicks(5);

        c.cmd("ntmspace neutron off");
        c.cmd("clear @s");
    }

    private static JsonObject dosimeterGas(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) p.addProperty((String) kv[i], (Number) kv[i + 1]);
        return c.query("hbm_dosimeter_gas", p);
    }

    /** Resets the player's contamination (HBM's {@code hbm_props} query). */
    private static void clearRads(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("set_rads", 0.0);
        c.query("hbm_props", p);
    }

    /**
     * Waits {@code ticks}, resetting the player's contamination every 20 ticks: 500 RAD/s kills a survival player in about
     * two seconds, and a dead player's dropped dosimeter stops clicking (the 4..5 clicks of the first six-node run).
     */
    private static void waitAlive(Client c, int ticks) {
        for (int left = ticks; left > 0; left -= 20) {
            c.waitTicks(Math.min(20, left));
            clearRads(c);
        }
    }

    private static void setRad(Client c, double rad) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "fog");
        p.addProperty("set_rad", rad);
        c.query("hbm_small", p);
    }

    /**
     * hbm-space {@code ItemDosimeter} and {@code BlockGasBase} (HBM switches V-186, V-187):
     * <ul>
     *   <li>both switches are set ({@code hbm_dosimeter_gas}: {@code server.gate} true, {@code chance} 9);</li>
     *   <li>the dosimeter in strong radiation (rad buffer >= 2) still clicks geiger3 on every fifth tick (the gate only
     *       acts below 1); at a weak dose (rad buffer printed, wanted below 0.3) it clicks on far fewer click ticks than
     *       1.7's half: at most 22 clicks in 80 click ticks (1.7 would average 40);</li>
     *   <li>radon seen through the ash glasses: clouds built in 100 ticks at chance 9 against the same 100 ticks at 1.7's
     *       every-tick rate ({@code set_chance 1}, then 9 again): about one ninth (ratio 3..30), screenshot 08.</li>
     * </ul>
     */
    private static void dosimeterAndGlasses(Client c, int x, int y, int z) {
        JsonObject state = dosimeterGas(c);
        System.out.println("[fork3] dosimeter/gas state: " + state);
        Expect.that(state.has("server") && state.getAsJsonObject("server").get("gate").getAsBoolean(), "dosimeter click gate on: " + state);
        Expect.that(state.get("chance").getAsInt() == 9, "ash-glasses cloud chance 1 in 9: " + state);

        c.setOption("sound_capture", "true");
        try {
            c.cmd("clear @s");
            c.cmd("ntmrad clear");
            clearRads(c); // contamination left by an earlier run would kill the player as soon as it is in survival
            c.cmd("gamemode survival");
            Fixtures.giveOnly(c, "hbm:dosimeter", 1);
            c.hotbar(1); // the harness numbers hotbar slots 1..9
            c.teleport(x + 0.5, y + 1, z + 0.5);
            clearRads(c);
            setRad(c, 500.0);
            waitAlive(c, 30);
            c.markLog();
            waitAlive(c, 80);
            java.util.List<String> strong = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
            System.out.println("[fork3] dosimeter strong clicks in 80 ticks: " + strong.size());
            Expect.that(strong.size() >= 8 && strong.stream().allMatch(l -> l.contains("id=hbm:item.geiger3")),
                    "above 1 RAD/s the gated dosimeter still clicks geiger3 every fifth tick: " + strong.size());

            // 500 RAD/s has spread into the neighbouring pockets (RadiationSystemNT); clear them all, then set the weak dose
            c.cmd("ntmrad clear");
            clearRads(c);
            setRad(c, 0.2);
            c.waitTicks(60);
            double radBuf = dosimeterGas(c).getAsJsonObject("server").get("rad_buf").getAsDouble();
            c.markLog();
            c.waitTicks(400);
            java.util.List<String> weak = c.logSinceMark("harness-sound.*id=hbm:item.geiger[1-6]");
            System.out.println("[fork3] dosimeter weak dose rad_buf=" + radBuf + " clicks in 400 ticks: " + weak.size());
            if (radBuf > 1E-5 && radBuf < 0.3) {
                Expect.that(weak.size() <= 22, "a weak dose is gated (1.7 would average 40 clicks): " + weak.size());
            } else {
                System.out.println("[fork3] WARN rad buffer " + radBuf + " outside (0, 0.3): weak-dose gate not judged");
            }
        } finally {
            setRad(c, 0.0);
            c.cmd("ntmrad clear");
            clearRads(c);
            c.setOption("sound_capture", "false");
            c.cmd("clear @s");
            c.cmd("gamemode creative");
        }

        int gx = x - 6, gz = z - 12;
        c.cmd("gamerule randomTickSpeed 0");
        c.cmd("gamerule random_tick_speed 0"); // 26.1's name
        c.cmd("fill " + (gx - 2) + " " + (y + 1) + " " + (gz - 2) + " " + (gx + 2) + " " + (y + 3) + " " + (gz + 2) + " hbm:gas_radon");
        c.cmd("item replace entity @s armor.head with hbm:ashglasses");
        c.teleport(gx + 5.5, y + 1, gz + 0.5); // east of the gas, clear of the refueler
        c.lookAt(gx + 0.5, y + 2, gz + 0.5);
        c.waitTicks(20);
        try {
            long before = dosimeterGas(c).get("revealed").getAsLong();
            c.waitTicks(100);
            long gated = dosimeterGas(c).get("revealed").getAsLong() - before;
            c.screenshot("08-radon-ash-glasses");
            before = dosimeterGas(c, "set_chance", 1).get("revealed").getAsLong();
            c.waitTicks(100);
            long every = dosimeterGas(c, "set_chance", 9).get("revealed").getAsLong() - before;
            System.out.println("[fork3] ash-glasses clouds in 100 ticks: 1 in 9 = " + gated + ", every tick = " + every);
            Expect.that(gated > 0, "the glasses still show radon clouds: " + gated);
            Expect.that(every >= 3 * gated && every <= 30 * gated, "1 in 9 shows about a ninth of 1.7's clouds: " + gated + " vs " + every);
        } finally {
            dosimeterGas(c, "set_chance", 9);
            c.cmd("fill " + (gx - 2) + " " + (y + 1) + " " + (gz - 2) + " " + (gx + 2) + " " + (y + 3) + " " + (gz + 2) + " minecraft:air");
            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.cmd("gamerule randomTickSpeed 3");
            c.cmd("gamerule random_tick_speed 3");
        }
    }
}
