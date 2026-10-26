package com.ntmspace.atmosphere;

import com.ntmspace.breathing.Breathing;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The push of hbm-space's {@code EntityDepress}, the blast of air out of a room whose seal broke: for its lifetime (20
 * ticks from a blob) every entity within 8 blocks is pulled 0.1 per tick towards the breach, or pushed away if it is on
 * the far side of it, except players who are in another pressurised room. Kept as a server-side ticker instead of an
 * entity: it only lives one second, and a player's motion is sent with {@code hurtMarked} (1.7's entity ran on both sides).
 */
public final class Decompressions {

    private Decompressions() {}

    /** 1.7 {@code EntityDepress.range}: push/pull range. */
    public static final double RANGE = 8;

    /** One leak pulse, shared by the active ticker and callers that advance a pulse themselves. */
    public static final class Depress {
        private final ServerLevel level;
        private final Direction dir;
        private double x, y, z;
        private int ttl;

        public Depress(ServerLevel level, Direction dir, int timeToLive) {
            this.level = level;
            this.dir = dir;
            this.ttl = timeToLive;
        }

        public void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void tick() {
            push(this);
            ttl--;
        }

        public boolean isRemoved() {
            return ttl <= 0;
        }
    }

    private static final List<Depress> ACTIVE = new ArrayList<>();

    static void add(ServerLevel level, double x, double y, double z, Direction dir, int timeToLive) {
        var pulse = new Depress(level, dir, timeToLive);
        pulse.setPos(x, y, z);
        ACTIVE.add(pulse);
    }

    static void clear() {
        ACTIVE.clear();
    }

    static void tick() {
        Iterator<Depress> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Depress depress = iterator.next();
            depress.tick();
            if (depress.isRemoved()) iterator.remove();
        }
    }

    private static void push(Depress d) {
        AABB box = new AABB(d.x - RANGE, d.y - RANGE, d.z - RANGE, d.x + RANGE, d.y + RANGE, d.z + RANGE);
        for (Entity entity : d.level.getEntities((Entity) null, box, e -> true)) {
            Vec3 vec = new Vec3(d.x - entity.getX(), d.y - entity.getY(), d.z - entity.getZ());

            // check if the player is in a different pressurized room (other living entities update too rarely, 1.7)
            if (entity instanceof Player player && Breathing.hasGravity(player)) continue;

            double dist = vec.length();
            if (dist > RANGE) continue;

            boolean succ = switch (d.dir) {
                case UP -> entity.getY() < d.y;
                case DOWN -> entity.getY() > d.y;
                case EAST -> entity.getX() < d.x;
                case WEST -> entity.getX() > d.x;
                case SOUTH -> entity.getZ() < d.z;
                case NORTH -> entity.getZ() > d.z;
            };

            vec = vec.normalize();
            double speed = succ ? 0.1D : -0.1D;
            entity.setDeltaMovement(entity.getDeltaMovement().add(vec.x * speed, vec.y * speed, vec.z * speed));
            if (entity instanceof LivingEntity) entity.hurtMarked = true;
        }
    }
}
