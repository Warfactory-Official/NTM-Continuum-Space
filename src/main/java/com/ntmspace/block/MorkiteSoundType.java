package com.ntmspace.block;

import com.ntmspace.registry.BodyOreBlocks;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/** The legacy ModSoundTypes.mork break sound and randomized pitch. */
public final class MorkiteSoundType extends SoundType {
    public static final SoundType INSTANCE = new MorkiteSoundType();
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private MorkiteSoundType() {
        super(0.85F, 0.85F, STONE.getBreakSound(), STONE.getStepSound(),
                STONE.getPlaceSound(), STONE.getHitSound(), STONE.getFallSound());
    }

    @Override
    public SoundEvent getBreakSound() {
        BREAKING.set(Boolean.TRUE);
        return BodyOreBlocks.STEP_MORKITE.get();
    }

    @Override
    public SoundEvent getPlaceSound() {
        BREAKING.set(Boolean.FALSE);
        return super.getPlaceSound();
    }

    @Override
    public SoundEvent getStepSound() {
        BREAKING.set(Boolean.FALSE);
        return super.getStepSound();
    }

    @Override
    public float getPitch() {
        var base = BREAKING.get() ? super.getPitch() - 0.15F : super.getPitch();
        return base + ThreadLocalRandom.current().nextFloat() * 0.2F;
    }
}
