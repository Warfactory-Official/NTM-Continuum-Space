package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * hbm-space's {@code CBT_War}: when two planets are at war both carry this trait; health and shield, and the projectiles
 * in flight (their effects are drawn on the client). Ported member for member. One save change: 1.7 stored a projectile's
 * target as a numeric dimension id; the port stores the target level's id ({@code "ntmspace:duna"}) as {@code target}.
 */
public class CBT_War extends CelestialBodyTrait {

    public int health;
    public int shield;

    public List<Projectile> projectiles;

    public CBT_War() {
        this.health = 100;
        this.shield = 0;
        this.projectiles = new ArrayList<>();
    }

    public CBT_War(int health, int shield) {
        this.health = health;
        this.shield = shield;
        this.projectiles = new ArrayList<>();
    }

    public void launchProjectile(Projectile proj) {
        projectiles.add(proj);
    }

    public void launchProjectile(float traveltime, int size, int damage, float x, double y, double z, ProjectileType type, String target) {
        projectiles.add(new Projectile(traveltime, size, damage, x, y, z, type, target));
    }

    public void split(int amount, Projectile projectile, ProjectileType type) {
        // currently kind of temp, there might be a better way to generalize this (1.7)
        if (projectile.getTravel() <= 0) {
            for (int j = 0; j < amount; j++) {
                Random rand = new Random();
                float randX = rand.nextFloat() * 160 - 80;
                float randY = rand.nextFloat() * 90 - 80;
                this.launchProjectile(Math.abs(20 + j * 10), projectile.getSize(), projectile.getDamage(), (float) projectile.getTranslateX(),
                        projectile.getTranslateY() - randY * j, projectile.getTranslateZ() + randX * j, type, projectile.getTarget());
            }
            this.destroyProjectile(projectile);
        }
    }

    @Override
    public void update(boolean remote, String body) {
        if (remote) return;
        for (int i = 0; i < this.getProjectiles().size(); i++) {
            Projectile projectile = this.getProjectiles().get(i);
            projectile.update();
            if (projectile.getTravel() <= 0) projectile.impact();

            if (projectile.getAnimtime() >= 100) {
                this.destroyProjectile(projectile);
                String target = CelestialTraits.bodyOfLevelId(projectile.getTarget());
                i--;
                // 1.7: decided by this (attacking) trait's health, not the target's
                if (this.health > 0) {
                    CelestialTraits.damage(projectile.getDamage(), target);
                } else if (this.health <= 0) {
                    CelestialTraits.modifyTraits(CelestialTraits.planetOf(target), new CBT_Destroyed());
                    this.health = 0;
                }
            }

            if (projectile.getType() == ProjectileType.SPLITSHOT) {
                if (projectile.getTravel() <= 0) {
                    this.split(4, projectile, ProjectileType.SMALL);
                    this.destroyProjectile(projectile);
                    i--;
                }
            }
        }
        if (!projectiles.isEmpty()) CelestialTraits.markChanged();
    }

    public void destroyProjectile(Projectile proj) {
        projectiles.remove(proj);
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    @Override
    public CBT_War copy() {
        CBT_War copy = new CBT_War(health, shield);
        for (Projectile p : projectiles) copy.projectiles.add(p.copy());
        return copy;
    }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(health);
        buf.writeInt(shield);
        buf.writeInt(projectiles.size());
        for (Projectile projectile : projectiles) projectile.writeToBytes(buf);
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        // 1.7 quirk, kept: written health then shield, read back shield then health (they swap on the client)
        shield = buf.readInt();
        health = buf.readInt();
        int numProjectiles = buf.readInt();
        projectiles = new ArrayList<>(numProjectiles);
        for (int i = 0; i < numProjectiles; i++) {
            Projectile projectile = new Projectile();
            projectile.readFromBytes(buf);
            projectiles.add(projectile);
        }
    }

    private record Saved(int health, int shield, List<Projectile> projectiles) {}

    private record SavedProjectile(int damage, float traveltime, int size, double x, double y, double z, int animtime,
                                   String type, String target) {}

    static Codec<CBT_War> codec() {
        Codec<SavedProjectile> projectile = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("damage", 0).forGetter(SavedProjectile::damage),
                Codec.FLOAT.optionalFieldOf("traveltime", 0F).forGetter(SavedProjectile::traveltime),
                Codec.INT.optionalFieldOf("size", 0).forGetter(SavedProjectile::size),
                Codec.DOUBLE.optionalFieldOf("translateX", 0D).forGetter(SavedProjectile::x),
                Codec.DOUBLE.optionalFieldOf("translateY", 0D).forGetter(SavedProjectile::y),
                Codec.DOUBLE.optionalFieldOf("translateZ", 0D).forGetter(SavedProjectile::z),
                Codec.INT.optionalFieldOf("animtime", 0).forGetter(SavedProjectile::animtime),
                Codec.STRING.optionalFieldOf("type", "MEDIUM").forGetter(SavedProjectile::type),
                Codec.STRING.optionalFieldOf("target", "minecraft:overworld").forGetter(SavedProjectile::target)
        ).apply(i, SavedProjectile::new));
        Codec<Projectile> projectiles = projectile.xmap(s -> {
            Projectile p = new Projectile(s.traveltime(), s.size(), s.damage(), s.x(), s.y(), s.z(), ProjectileType.byName(s.type()), s.target());
            p.animtime = s.animtime();
            return p;
        }, p -> new SavedProjectile(p.damage, p.traveltime, p.size, p.translateX, p.translateY, p.translateZ, p.animtime,
                p.type.name(), p.target));
        return RecordCodecBuilder.<Saved>create(i -> i.group(
                Codec.INT.optionalFieldOf("health", 0).forGetter(Saved::health),
                Codec.INT.optionalFieldOf("shield", 0).forGetter(Saved::shield),
                projectiles.listOf().optionalFieldOf("projectiles", List.of()).forGetter(Saved::projectiles)
        ).apply(i, Saved::new)).xmap(saved -> {
            CBT_War war = new CBT_War(saved.health(), saved.shield());
            war.projectiles.addAll(saved.projectiles());
            return war;
        }, war -> new Saved(war.health, war.shield, new ArrayList<>(war.projectiles)));
    }

    public static class Projectile {

        private float traveltime;
        private int size;
        private int damage;
        private int animtime;
        private float flashtime;
        private double translateX;
        private double translateY;
        private double translateZ;
        private ProjectileType type;
        public int GUIangle; // is this legal (1.7)
        private String target = "minecraft:overworld";

        public Projectile() {
            this.animtime = 0;
            this.flashtime = 0;
            this.type = ProjectileType.MEDIUM; // Default type
        }

        public Projectile(float traveltime, int size, int damage, double posX, double posY, double posZ, ProjectileType type, String target) {
            this.traveltime = traveltime;
            this.size = size;
            this.damage = damage;
            this.animtime = 0;
            this.flashtime = 0;
            this.translateX = posX;
            this.translateY = posY;
            this.translateZ = posZ;
            this.type = type;
            this.target = target;
        }

        Projectile copy() {
            Projectile p = new Projectile(traveltime, size, damage, translateX, translateY, translateZ, type, target);
            p.animtime = animtime;
            p.flashtime = flashtime;
            p.GUIangle = GUIangle;
            return p;
        }

        public void writeToBytes(RegistryFriendlyByteBuf buf) {
            buf.writeInt(damage);
            buf.writeFloat(traveltime);
            buf.writeInt(size);
            buf.writeDouble(translateX);
            buf.writeDouble(translateY);
            buf.writeDouble(translateZ);
            buf.writeInt(animtime);
            buf.writeByte(type.ordinal()); // runtime packet only, never saved
            buf.writeInt(GUIangle);
            buf.writeUtf(target);
            // 1.7 sent the flash separately in the perma-sync packet (PermaSyncHandler, "take out back and shoot")
            buf.writeFloat(flashtime);
        }

        public void readFromBytes(RegistryFriendlyByteBuf buf) {
            damage = buf.readInt();
            traveltime = buf.readFloat();
            size = buf.readInt();
            translateX = buf.readDouble();
            translateY = buf.readDouble();
            translateZ = buf.readDouble();
            animtime = buf.readInt();
            type = ProjectileType.values()[buf.readByte()];
            GUIangle = buf.readInt();
            target = buf.readUtf();
            flashtime = buf.readFloat();
        }

        public ProjectileType getType() { return type; }
        public void setType(ProjectileType type) { this.type = type; }
        public float getFlashtime() { return flashtime; }

        public void update() {
            if (traveltime > 0) {
                traveltime--;
            } else {
                traveltime = 0;
                if (this.getType() != ProjectileType.SPLITSHOT) animtime = Math.min(100, animtime + 1);
            }
        }

        public void impact() {
            flashtime += 0.6f;
            flashtime = Math.min(100.0f, flashtime + 0.1f * (100.0f - flashtime) * 0.15f);
            if (flashtime >= 100) flashtime = 100;
            if (animtime == 0) flashtime = 0;
        }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getDamage() { return damage; }
        public float getTravel() { return traveltime; }
        public void setTravel(float travel) { this.traveltime = travel; }
        public void setDamage(int damage) { this.damage = damage; }
        public int getAnimtime() { return animtime; }
        public void setAnimtime(int animtime) { this.animtime = animtime; }
        public void setFlashtime(float flashtime) { this.flashtime = flashtime; }
        public double getTranslateX() { return translateX; }
        /** The target level's id ({@code "ntmspace:duna"}); 1.7 held a dimension number. */
        public String getTarget() { return target; }
        public double getTranslateY() { return translateY; }
        public double getTranslateZ() { return translateZ; }
    }

    public enum ProjectileType {
        SMALL,
        MEDIUM,
        HUGE,
        INCENDIARY,
        NUCLEAR,
        SPLITSHOT;

        static ProjectileType byName(String name) {
            for (ProjectileType type : values()) if (type.name().equals(name)) return type;
            return MEDIUM;
        }
    }
}
