package com.ntmspace.entity.mob.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * hbm-space {@code EntityMoonWalkHelper}, the Mun cow's move helper: 1.7's {@code EntityMoveHelper} with the heading
 * turned round (+90 instead of -90) and the forward input negated, so the cow faces away from where it walks and goes
 * there backwards. That is the joke (a moonwalk), not a bug; ported as written, including 1.7's 30 degree turn limit.
 */
public class MoonWalkControl extends MoveControl {

    public MoonWalkControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        this.mob.setZza(0.0F);

        // 1.7 had only the "update" flag; STRAFE and JUMPING do not exist there and never reach this helper
        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;
            int i = Mth.floor(this.mob.getBoundingBox().minY + 0.5D);
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedZ - this.mob.getZ();
            double d2 = this.wantedY - (double) i;
            double d3 = d0 * d0 + d2 * d2 + d1 * d1;

            if (d3 >= 2.500000277905201E-7D) {
                float f = (float) (Math.atan2(d1, d0) * 180.0D / Math.PI) + 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, 30.0F));
                float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.mob.setSpeed(speed);
                this.mob.setZza(-speed);

                if (d2 > 0.0D && d0 * d0 + d1 * d1 < 1.0D) {
                    this.mob.getJumpControl().jump();
                }
            }
        }
    }
}
