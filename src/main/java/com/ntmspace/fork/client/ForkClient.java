package com.ntmspace.fork.client;

import dev.golem.core.Ids;
import dev.golem.core.client.Capes;

/**
 * Client side of hbm-space's non-space fork content: the two capes hbm-space's {@code RenderAccessoryUtility} added,
 * for {@code DUODEC_} (Gwen, {@code CapeGwen}) and {@code MellowRPG8} ({@code CapeMellow}). Registered after HBM's
 * provider, which names neither player, so 1.7's order (every HBM cape first) holds.
 *
 * <p>Not reproduced: hbm-space also dropped HBM's "Player*" test cape; that only dresses development clients and HBM's
 * port keeps it (it also covers the {@code Dev} client name), so it stays.
 */
public final class ForkClient {

    /** hbm-space {@code ShadyUtil.DUODEC_} and {@code ShadyUtil.MellowRPG8}. */
    public static final String DUODEC_ = "85d54b8c-1862-4c86-b351-5ef1b998aa32";
    public static final String MELLOW_RPG8 = "99b9efa5-8c38-4b22-86c2-baf97789d2fb";

    private ForkClient() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        var gwen = Ids.of("ntmspace", "textures/models/capes/capegwen.png");
        var mellow = Ids.of("ntmspace", "textures/models/capes/capemellow.png");
        Capes.register((id, name) -> {
            String uuid = id.toString();
            if (uuid.equals(DUODEC_)) return gwen;
            if (uuid.equals(MELLOW_RPG8)) return mellow;
            return null;
        });
        // hbm-space RenderChungus / RenderCondenser: a damaged machine draws its wreck (chungus_destroyed with the
        // turbine's own placement, culled; condenser_damaged turned another 180 degrees, unculled)
        com.hbm.client.render.RenderChungus.DAMAGED = new com.hbm.client.render.DamagedView(
                be -> be instanceof com.ntmspace.fork.machine.DamagedMachines.Damageable d && d.isDamaged(),
                "ntmspace:models/machines/chungus_destroyed.obj", "ntmspace:textures/models/machines/chungus_destroyed.png", 0F, true);
        com.hbm.client.render.CondenserRenderer.DAMAGED = new com.hbm.client.render.DamagedView(
                be -> be instanceof com.ntmspace.fork.machine.DamagedMachines.Damageable d && d.isDamaged(),
                "ntmspace:models/machines/condenser_damaged.obj", "ntmspace:textures/models/machines/condenser_damaged.png", 180F, false);
        // hbm-space's flashbang / nitan overlays and the slippery player's slide (both tick phases, as 1.7's player tick)
        ForkHud.init();
        // part 3: wings fold while riding, the refueler's tank overlay, the compressor tube's gas line
        ForkBaseClient.init();
        dev.golem.core.client.ClientTicks.onStartTick(ForkClient::slide);
        dev.golem.core.client.ClientTicks.onEndTick(ForkClient::slide);
        // hbm-space's airliner and B2X bombers
        ForkBomberSkins.init();
        // hbm-space's bobbleheads: HBM's commemorative panel, HBM's figure
        com.ntmspace.fork.bobble.BlockSpaceBobble.OPEN_SCREEN = type -> net.minecraft.client.Minecraft.getInstance()
                .setScreen(new com.hbm.inventory.gui.GUIScreenBobble(type.name, type.contribution, type.inscription));
        com.hbm.client.render.item.CustomItemRenderers.register("ntmspace:bobblehead", RenderSpaceBobble.Item::new);
        dev.golem.core.client.ClientRegistrations.renderers(renderers -> com.hbm.client.render.HbmBlockEntityRenderers.regGem(
                com.ntmspace.client.render.machine.MachineRenderers.sink(renderers),
                com.ntmspace.fork.ForkContent.BOBBLEHEAD_BE.get(), RenderSpaceBobble::new));
        // hbm-space's antimatter bomb: GUINukeAntimatter + RenderAntimatter
        dev.golem.core.client.ClientRegistrations.screens(screens ->
                screens.register(com.ntmspace.fork.ForkContent.NUKE_ANTIMATTER_MENU.get(), GUINukeAntimatter::new));
        dev.golem.core.client.ClientRegistrations.renderers(renderers -> com.hbm.client.render.HbmBlockEntityRenderers.regGem(
                com.ntmspace.client.render.machine.MachineRenderers.sink(renderers),
                com.ntmspace.fork.ForkContent.NUKE_ANTIMATTER_BE.get(), RenderAntimatter::new));
    }

    private static void slide() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) com.ntmspace.fork.Slippery.playerTick(player);
    }
}
