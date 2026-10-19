package com.ntmspace.fork;

import com.hbm.entity.logic.BomberPayload;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's two bombers ({@code EntityBomber.statFacCV}, {@code statFacCarpetJet}) as HBM bomber payloads
 * (ADDON-API-INVENTORY V-101) with named airframes ({@code EntityBomber.setSkin}, H-72/C-25):
 * <ul>
 *   <li>{@code ntmspace:airliner} - the "Civilian Airliner" designator ({@code bomb_caller} metadata 8, here
 *       {@code ntmspace:bomb_caller_airliner}, "Rerouted Civilian Traffic!"): 1.7 type 8, airframe 9 (the 737, texture
 *       {@code airlinerplaceholder}), timer 200, sixteen blocks higher than the target, a drop window 75..125 at a rate
 *       of 90000 - it never drops anything (kept: the airliner is a joke, not a bomber).</li>
 *   <li>{@code ntmspace:carpet_jet} - the B2X (airframe 10, {@code b2.obj}, {@code b_2.png}, two exhaust plumes): 1.7
 *       type 9, timer 150, drops 10..70 every 3 ticks, half as fast again; each bomblet ({@code ntmspace:jet}, 1.7
 *       {@code EntityBombletZeta} type 9) is an {@code ExplosionVNT} of 4 half a block east/south and one and a half up
 *       of its impact, with {@code EntityProcessorCrossSmooth(1, 100)}, the standard player processor and the weapon
 *       effect {@code (15, 3.5, 1.25)}. No designator: Thatmo's battlefield calls it ({@link #carpetJet}).</li>
 * </ul>
 * Both play {@code entity.jetloopbomb} as their engine loop (1.7: airframe 9 and above).
 */
public final class ForkBombers {

    private ForkBombers() {}

    public static final String AIRLINER_SKIN = NtmSpace.MOD_ID + ":airliner";
    public static final String B2X_SKIN = NtmSpace.MOD_ID + ":b2x";

    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(SOUNDS);
    /** hbm-space {@code entity.jetloopbomb} (file {@code entity/jetbomb2}). */
    public static final RegistryObject<SoundEvent> JET_LOOP = REG.sound("entity.jetloopbomb");

    /** hbm-space {@code EntityBombletZeta} type 9. */
    public static final EntityBombletZeta.Kind JET = EntityBombletZeta.KINDS.register(new EntityBombletZeta.Kind(
            NtmSpace.MOD_ID + ":jet", zeta -> {
                ExplosionVNT vnt = new ExplosionVNT(zeta.level(), zeta.getX() + 0.5F, zeta.getY() + 1.5F, zeta.getZ() + 0.5F, 4F);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 100));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
                vnt.explode();
            }));

    /** hbm-space {@code statFacCV}. */
    public static final BomberPayload AIRLINER = BomberPayload.register(NtmSpace.MOD_ID + ":airliner",
            (level, x, y, z) -> {
                // 1.7: fac(world, x, y + 16, z) - "adding the y value breaks things, sorry bluehat"
                EntityBomber bomber = EntityBomber.create(level, x, y + 16, z, 75, 125, 90000, NtmSpace.MOD_ID + ":airliner");
                bomber.timer = 200;
                return bomber.setSkin(AIRLINER_SKIN);
            },
            // 1.7 type 8 fell into the bomblet branch, but tickCount % 90000 never hits inside 75..125
            bomber -> {});

    /** hbm-space {@code statFacCarpetJet}. */
    public static final BomberPayload CARPET_JET = BomberPayload.register(NtmSpace.MOD_ID + ":carpet_jet",
            (level, x, y, z) -> {
                EntityBomber bomber = EntityBomber.create(level, x, y, z, 10, 70, 3, NtmSpace.MOD_ID + ":carpet_jet");
                bomber.timer = 150;
                // 1.7 fac(): type 9 flies at 1.5 times the heading
                Vec3 m = bomber.getDeltaMovement();
                bomber.setDeltaMovement(m.x * 1.5, m.y, m.z * 1.5);
                return bomber.setSkin(B2X_SKIN);
            },
            bomber -> bomber.dropBomblet(JET, true));

    /**
     * hbm-space {@code WorldProviderThatmo.updateWeather}'s battlefield strike: a carpet jet at the target, 20 blocks above
     * it (1.7 set {@code posY = y + 20} after {@code fac}); the caller adds it to the world.
     */
    public static EntityBomber carpetJet(net.minecraft.world.level.Level level, double x, double y, double z) {
        EntityBomber bomber = CARPET_JET.create(level, x, y, z);
        bomber.setPos(bomber.getX(), y + 20, bomber.getZ());
        return bomber;
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        EntityBomber.LOOP_SOUND.register(bomber -> bomber.getSkin().startsWith(NtmSpace.MOD_ID + ":") ? JET_LOOP.get() : null);
    }
}
