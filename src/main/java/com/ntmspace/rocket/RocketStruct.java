package com.ntmspace.rocket;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.world.SolarSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code handler/RocketStruct}: a payload ({@code capsule}) on up to {@link #MAX_STAGES} stages, stage 0
 * at the bottom. Validation, the issue list the assembly screen prints, fill requirements, masses and heights are 1.7's.
 *
 * <p>Saved by part item id ({@link #CODEC}: {@code capsule}, {@code stages[{fuselage, fins, thruster, fc, tc}]},
 * {@code freq}); 1.7 wrote numeric item ids into NBT and a data watcher. {@code extraIssues} travel only in packets.
 */
public final class RocketStruct {

    public @Nullable RocketPart capsule;
    public List<RocketStage> stages = new ArrayList<>();
    public int satFreq = 0;

    public List<Component> extraIssues = new ArrayList<>();

    public static final int MAX_STAGES = 5;

    public RocketStruct() {}

    public RocketStruct(ItemStack capsule) {
        this.capsule = RocketParts.get(capsule);
    }

    public RocketStruct(@Nullable RocketPart capsule) {
        this.capsule = capsule;
    }

    public RocketStruct copy() {
        RocketStruct rocket = new RocketStruct(capsule);
        for (RocketStage stage : stages) rocket.stages.add(stage.copy());
        rocket.satFreq = satFreq;
        rocket.extraIssues = new ArrayList<>(extraIssues);
        return rocket;
    }

    public void addStage(ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        addStage(RocketParts.get(fuselage), RocketParts.get(fins), RocketParts.get(thruster),
                fuselage.isEmpty() ? 1 : fuselage.getCount(), thruster.isEmpty() ? 1 : thruster.getCount());
    }

    public void addStage(@Nullable RocketPart fuselage, @Nullable RocketPart fins, @Nullable RocketPart thruster,
                         int fuselageCount, int thrusterCount) {
        RocketStage stage = new RocketStage();
        stage.fuselage = fuselage;
        stage.fins = fins;
        stage.thruster = thruster;
        stage.fuselageCount = fuselageCount;
        stage.thrusterCount = thrusterCount;
        stages.add(0, stage);
    }

    public boolean validate() {
        if (extraIssues.size() > 0) return false;

        if (capsule == null || capsule.type() != PartType.WARHEAD) return false;

        if (!capsule.isApollo() && !capsule.isSatellite()) return false;

        if (stages.size() == 0) return false;

        for (RocketStage stage : stages) {
            if (stage.fuselage == null || stage.fuselage.type() != PartType.FUSELAGE) return false;
            if (stage.fins != null && stage.fins.type() != PartType.FINS) return false;
            if (stage.thruster == null || stage.thruster.type() != PartType.THRUSTER) return false;

            if (stage.thrusterCount > stage.fuselageCount || stage.fuselageCount % stage.thrusterCount != 0) return false;

            if (!"ANY".equals(stage.fuselage.fuel()) && !java.util.Objects.equals(stage.fuselage.fuel(), stage.thruster.fuel())) return false;
        }

        return true;
    }

    public void addIssue(Component issue) {
        extraIssues.add(issue);
    }

    /**
     * Lists any validation issues so the player can rectify easily. {@code from}/{@code to} are the drives' targets
     * (null for no drive).
     */
    public List<Component> findIssues(int stageNum, SolarSystem.@Nullable Body from, SolarSystem.@Nullable Body to,
                                      boolean fromOrbit, boolean toOrbit) {
        List<Component> issues = new ArrayList<>();

        // If we have no parts, we have no worries
        if (capsule == null && stages.size() == 0) return issues;

        if (capsule == null || (!capsule.isApollo() && !capsule.isSatellite()))
            issues.add(Component.literal("Invalid Capsule/Satellite").withStyle(ChatFormatting.RED));

        // Current stage stats
        if (stageNum < stages.size()) {
            RocketStage stage = stages.get(stageNum);
            issues.add(Component.literal("Dry mass: " + getLaunchMass(stageNum) + "kg"));
            issues.add(Component.literal("Wet mass: " + getWetMass(stageNum) + "kg"));
            if (stage.thruster != null) {
                issues.add(Component.literal("Thrust: " + getThrust(stage) + "N"));
                issues.add(Component.literal("ISP: " + getISP(stage) + "s"));
            }
        }

        for (int i = 0; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage == null)
                issues.add(Component.literal("Stage " + (i + 1) + " missing fuselage").withStyle(ChatFormatting.RED));
            if (stage.thruster == null)
                issues.add(Component.literal("Stage " + (i + 1) + " missing thruster").withStyle(ChatFormatting.RED));

            if (stage.fuselage == null || stage.thruster == null)
                continue;

            if (stage.thrusterCount > stage.fuselageCount)
                issues.add(Component.literal("Stage " + (i + 1) + " too many thrusters").withStyle(ChatFormatting.RED));
            if (stage.fuselageCount % stage.thrusterCount != 0)
                issues.add(Component.literal("Stage " + (i + 1) + " uneven thrusters").withStyle(ChatFormatting.RED));

            if (!"ANY".equals(stage.fuselage.fuel()) && !java.util.Objects.equals(stage.fuselage.fuel(), stage.thruster.fuel()))
                issues.add(Component.literal("Stage " + (i + 1) + " fuel mismatch").withStyle(ChatFormatting.RED));

            if (i > 0 && stage.fins == null)
                issues.add(Component.literal("Stage " + (i + 1) + " lacks landing legs").withStyle(ChatFormatting.YELLOW));

            // I was gonna add all sorts of realistic restrictions but then realised
            // KSP lets you shit any part onto any part, and that's fun
            // so who am I to kill your creative spirit
            // put that ant engine on your rhino fuselage
        }

        if (from != null && to != null) {
            int fuelRequirement = getFuelRequired(stageNum, from, to, fromOrbit, toOrbit);
            int fuelCapacity = getFuelCapacity(stageNum);

            if (fuelRequirement == Integer.MAX_VALUE) {
                issues.add(Component.literal("Insufficient thrust").withStyle(ChatFormatting.YELLOW));
            } else if (fuelCapacity < fuelRequirement) {
                issues.add(Component.literal("Insufficient fuel: " + fuelCapacity + "/" + fuelRequirement + "mB").withStyle(ChatFormatting.YELLOW));
            } else if (fuelCapacity > 0 && fuelRequirement > 0) {
                issues.add(Component.literal("Trip possible! " + fuelCapacity + "/" + fuelRequirement + "mB").withStyle(ChatFormatting.GREEN));
            }
        }

        issues.addAll(extraIssues);

        return issues;
    }

    /** The fuels and amounts the stages hold; {@code Fluids.NONE} is solid fuel. Insertion order is 1.7's HashMap's. */
    public Map<FluidType, Integer> getFillRequirement() {
        Map<FluidType, Integer> tanks = new LinkedHashMap<>();

        for (RocketStage stage : stages) {
            if (stage.thruster == null || stage.fuselage == null) continue;

            FluidType fuel = stage.thruster.getFuel();
            FluidType oxidizer = stage.thruster.getOxidizer();

            if (fuel != null) {
                int amount = stage.fuselage.getTankSize() * stage.fuselageCount;
                if (tanks.containsKey(fuel)) amount += tanks.get(fuel);
                tanks.put(fuel, amount);
            }

            if (oxidizer != null) {
                int amount = stage.fuselage.getTankSize() * stage.fuselageCount;
                if (tanks.containsKey(oxidizer)) amount += tanks.get(oxidizer);
                tanks.put(oxidizer, amount);
            }
        }

        return tanks;
    }

    /** 1.7 {@code hasSufficientFuel}: whether the bottom stage can make the trip (pods and bare capsules by rule). */
    public boolean hasSufficientFuel(SolarSystem.@Nullable Body from, SolarSystem.@Nullable Body to, boolean fromOrbit, boolean toOrbit) {
        if (capsule != null && RocketParts.POD.equals(capsule.id())) {
            // Pods can transfer, fall to orbited body, and return to station, but NOT hop on the surface
            return from == to && (fromOrbit || toOrbit);
        }

        if (stages.size() == 0) {
            return from == to && fromOrbit && !toOrbit; // Capsules can return to orbited body from orbit only
        }

        if (from == null || to == null) return false;

        int fuelRequirement = getFuelRequired(0, from, to, fromOrbit, toOrbit);
        int fuelCapacity = getFuelCapacity(0);

        return fuelCapacity >= fuelRequirement;
    }

    public int getFuelCapacity(int stageNum) {
        if (stageNum >= stages.size()) return -1;

        RocketStage stage = stages.get(stageNum);

        if (stage.fuselage == null) return -1;

        return stage.fuselage.getTankSize() * stage.fuselageCount;
    }

    /** 1.7 {@code getFuelRequired}: {@code SolarSystem.getCostBetween} with the bodies' air pressures. */
    public int getFuelRequired(int stageNum, SolarSystem.Body from, SolarSystem.Body to, boolean fromOrbit, boolean toOrbit) {
        if (stageNum >= stages.size()) return -1;

        RocketStage stage = stages.get(stageNum);

        if (stage.fuselage == null || stage.thruster == null) return -1;

        int rocketMass = getLaunchMass(stageNum);
        int thrust = getThrust(stage);
        int isp = getISP(stage);

        return SolarSystem.getCostBetween(from, to, rocketMass, thrust, isp, fromOrbit, toOrbit,
                Orbit.pressureOf(from), Orbit.pressureOf(to));
    }

    public int getThrust() {
        return getThrust(stages.get(0));
    }

    private int getThrust(RocketStage stage) {
        return stage.thruster.getThrust() * stage.thrusterCount;
    }

    private int getISP(RocketStage stage) {
        return stage.thruster.getISP();
    }

    /** The dry mass of the active stage + the wet mass of the stages above it. */
    public int getLaunchMass() {
        return getMass(0, false);
    }

    /** The dry mass of the selected stage + the wet mass of the stages above it. */
    public int getLaunchMass(int stageNum) {
        return getMass(stageNum, false);
    }

    public int getWetMass(int stageNum) {
        return getMass(stageNum, true);
    }

    private int getMass(int stageNum, boolean wet) {
        int mass = 0;

        if (capsule != null) mass += capsule.mass();

        for (int i = stageNum; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) mass += stage.fuselage.mass() * stage.fuselageCount;
            if (stage.thruster != null) mass += stage.thruster.mass() * stage.thrusterCount;

            if (stage.fuselage != null && (i > stageNum || wet)) {
                mass += stage.fuselage.getTankSize() * stage.fuselageCount / 4; // Reduced fuel weight to improve multi-stages
            }
        }

        // 1.7 MathHelper.ceiling_float_int of an int: the int itself
        return mass;
    }

    public double getHeight() {
        double height = 0;

        if (capsule != null) height += capsule.height();

        boolean isDeployed = true;

        for (RocketStage stage : stages) {
            if (stage.fuselage != null) height += stage.fuselage.height() * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height() : 0, isDeployed && stage.fins != null ? stage.fins.height() : 0);
            isDeployed = false;
        }

        return height;
    }

    public double getHeight(int stageNum) {
        double height = 0;

        if (stages.size() > 0) {
            RocketStage stage = stages.get(Math.min(stageNum, stages.size() - 1));
            if (stage.fuselage != null) height += stage.fuselage.height() * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height() : 0, stageNum == 0 && stage.fins != null ? stage.fins.height() : 0);
        }

        if (stages.size() == 0 || stageNum == stages.size() - 1) {
            if (capsule != null) height += capsule.height();
        }

        return height;
    }

    public double getOffset(int stageNum) {
        double height = 0;

        for (int i = 0; i < Math.min(stageNum, stages.size() - 1); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) height += stage.fuselage.height() * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height() : 0, i == 0 && stage.fins != null ? stage.fins.height() : 0);
        }

        return height;
    }

    // ---------------------------------------------------------------------------------------- persistence and sync

    private static String idOf(@Nullable RocketPart part) {
        return part == null ? "" : part.id();
    }

    private static @Nullable RocketPart partOf(String id) {
        return id.isEmpty() ? null : RocketParts.get(id);
    }

    /** The packet form (1.7 {@code writeToByteBuffer}): part ids as strings, then the extra issues. */
    public void writeToByteBuffer(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(idOf(capsule));

        buf.writeInt(stages.size());
        for (RocketStage stage : stages) {
            buf.writeUtf(idOf(stage.fuselage));
            buf.writeUtf(idOf(stage.fins));
            buf.writeUtf(idOf(stage.thruster));
            buf.writeByte(stage.fuselageCount);
            buf.writeByte(stage.thrusterCount);
        }

        buf.writeInt(extraIssues.size());
        for (Component issue : extraIssues) {
            buf.writeUtf(issue.getString());
            buf.writeInt(issue.getStyle().getColor() == null ? -1 : issue.getStyle().getColor().getValue());
        }
    }

    public static RocketStruct readFromByteBuffer(RegistryFriendlyByteBuf buf) {
        RocketStruct rocket = new RocketStruct();

        rocket.capsule = partOf(buf.readUtf());

        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            RocketStage stage = new RocketStage();
            stage.fuselage = partOf(buf.readUtf());
            stage.fins = partOf(buf.readUtf());
            stage.thruster = partOf(buf.readUtf());
            stage.fuselageCount = buf.readByte();
            stage.thrusterCount = buf.readByte();

            rocket.stages.add(stage);
        }

        count = buf.readInt();
        for (int i = 0; i < count; i++) {
            String text = buf.readUtf();
            int color = buf.readInt();
            Component issue = Component.literal(text);
            if (color >= 0) issue = issue.copy().withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color)));
            rocket.extraIssues.add(issue);
        }

        return rocket;
    }

    /** One stage as saved: part ids ({@code ""} for none) and the two counts. */
    public record StageIds(String fuselage, String fins, String thruster, int fc, int tc) {
        public static final Codec<StageIds> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("fuselage", "").forGetter(StageIds::fuselage),
                Codec.STRING.optionalFieldOf("fins", "").forGetter(StageIds::fins),
                Codec.STRING.optionalFieldOf("thruster", "").forGetter(StageIds::thruster),
                Codec.INT.optionalFieldOf("fc", 1).forGetter(StageIds::fc),
                Codec.INT.optionalFieldOf("tc", 1).forGetter(StageIds::tc)).apply(i, StageIds::new));
    }

    /**
     * The saved form (1.7 {@code writeToNBT}: {@code capsule}, {@code stages}, {@code freq}), by part id; an immutable
     * value, as an item component must be.
     */
    public record Design(String capsule, List<StageIds> stages, int freq) {
        public static final Codec<Design> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("capsule", "").forGetter(Design::capsule),
                StageIds.CODEC.listOf().optionalFieldOf("stages", List.of()).forGetter(Design::stages),
                Codec.INT.optionalFieldOf("freq", 0).forGetter(Design::freq)).apply(i, Design::new));

        public Design {
            stages = List.copyOf(stages);
        }
    }

    public Design toDesign() {
        List<StageIds> list = new ArrayList<>();
        for (RocketStage s : stages) list.add(new StageIds(idOf(s.fuselage), idOf(s.fins), idOf(s.thruster), s.fuselageCount, s.thrusterCount));
        return new Design(idOf(capsule), list, satFreq);
    }

    /** 1.7 {@code readFromNBT}: counts below 1 become 1. */
    public static RocketStruct fromDesign(Design design) {
        RocketStruct rocket = new RocketStruct(partOf(design.capsule()));
        for (StageIds s : design.stages()) {
            RocketStage stage = new RocketStage();
            stage.fuselage = partOf(s.fuselage());
            stage.fins = partOf(s.fins());
            stage.thruster = partOf(s.thruster());
            stage.fuselageCount = Math.max(s.fc(), 1);
            stage.thrusterCount = Math.max(s.tc(), 1);
            rocket.stages.add(stage);
        }
        rocket.satFreq = design.freq();
        return rocket;
    }

    public static final Codec<RocketStruct> CODEC = Design.CODEC.xmap(RocketStruct::fromDesign, RocketStruct::toDesign);

    /** Structural equality (parts and counts), for change detection. */
    public boolean sameAs(@Nullable RocketStruct other) {
        if (other == null || other.capsule != capsule || other.stages.size() != stages.size()) return false;
        for (int i = 0; i < stages.size(); i++) {
            RocketStage a = stages.get(i), b = other.stages.get(i);
            if (a.fuselage != b.fuselage || a.fins != b.fins || a.thruster != b.thruster
                    || a.fuselageCount != b.fuselageCount || a.thrusterCount != b.thrusterCount) return false;
        }
        return true;
    }

    public static final class RocketStage {

        public @Nullable RocketPart fuselage;
        public @Nullable RocketPart fins;
        public @Nullable RocketPart thruster;

        public int fuselageCount = 1;
        public int thrusterCount = 1;

        public RocketStage copy() {
            RocketStage stage = new RocketStage();
            stage.fuselage = fuselage;
            stage.fins = fins;
            stage.thruster = thruster;
            stage.fuselageCount = fuselageCount;
            stage.thrusterCount = thrusterCount;
            return stage;
        }

        public int getStack() {
            return Math.max(fuselageCount / thrusterCount, 1);
        }

        public int getCluster() {
            return Math.max(fuselageCount / getStack(), 1);
        }
    }
}
