package com.ntmspace.client.render.mob;

import com.hbm.client.model.WavefrontObj;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import net.minecraft.resources.Identifier;

/**
 * The OBJ meshes and skins of hbm-space's mobs ({@code ResourceManager} there), copied byte for byte from hbm-space
 * into {@code ntmspace:models/mobs} and {@code ntmspace:textures/entity}. Meshes load on first use (the client
 * resource manager must exist), as HBM's {@code NtmEntityModels}.
 */
public final class SpaceMobModels {

    private SpaceMobModels() {}

    private static WavefrontObj load(String path) {
        return WavefrontObj.load(Ids.of(NtmSpace.MOD_ID, "models/mobs/" + path));
    }

    public static Identifier texture(String name) {
        return Ids.of(NtmSpace.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static WavefrontObj scutterfish;
    private static WavefrontObj scuttlecrab;
    private static WavefrontObj scrapfish;
    private static WavefrontObj depthsquid;
    private static WavefrontObj siftereel;
    private static WavefrontObj tankbot;
    private static WavefrontObj behemoth;
    private static WavefrontObj bfangel;
    private static WavefrontObj miniUfo;

    public static WavefrontObj scutterfish() {
        if (scutterfish == null) scutterfish = load("scutterfish.obj");
        return scutterfish;
    }

    public static WavefrontObj scuttlecrab() {
        if (scuttlecrab == null) scuttlecrab = load("scuttlecrab.obj");
        return scuttlecrab;
    }

    public static WavefrontObj scrapfish() {
        if (scrapfish == null) scrapfish = load("scrapfish.obj");
        return scrapfish;
    }

    public static WavefrontObj depthsquid() {
        if (depthsquid == null) depthsquid = load("depthsquid.obj");
        return depthsquid;
    }

    public static WavefrontObj siftereel() {
        if (siftereel == null) siftereel = load("siftereel.obj");
        return siftereel;
    }

    public static WavefrontObj tankbot() {
        if (tankbot == null) tankbot = load("tankbot.obj");
        return tankbot;
    }

    /** hbm-space {@code ResourceManager.behemoth}: {@code war_machine.obj}. */
    public static WavefrontObj behemoth() {
        if (behemoth == null) behemoth = load("war_machine.obj");
        return behemoth;
    }

    public static WavefrontObj bfangel() {
        if (bfangel == null) bfangel = load("balefire_angel.obj");
        return bfangel;
    }

    public static WavefrontObj miniUfo() {
        if (miniUfo == null) miniUfo = load("mini_ufo.obj");
        return miniUfo;
    }

    public static final Identifier MOON_COW = texture("moon_cow");
    public static final Identifier SCUTTERFISH = texture("scutterfish");
    public static final Identifier TANKBOT = texture("robtex");
    public static final Identifier BEHEMOTH_BODY = texture("wm_body");
    public static final Identifier BEHEMOTH_EYE = texture("wm_eye");
    public static final Identifier BEHEMOTH_HELMET = texture("wm_helmet");
    public static final Identifier BEHEMOTH_LEG = texture("wm_leg");
    public static final Identifier BEHEMOTH_KNEE = texture("wm_knee");
    public static final Identifier BEHEMOTH_HIP = texture("wm_hip");
    public static final Identifier BFANGEL = texture("bfa_body");
    public static final Identifier CREEPER_FLESH = texture("creeper_flesh");
    /** hbm-space's {@code ModelMoonCow} helmet: HBM's boron glass. */
    public static final Identifier GLASS_BORON = Ids.of("hbm", "textures/blocks/glass_boron.png");
}
