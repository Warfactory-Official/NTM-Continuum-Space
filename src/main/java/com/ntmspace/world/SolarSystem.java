package com.ntmspace.world;

import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * hbm-space's solar system ({@code dim/SolarSystem}, {@code dim/CelestialBody}, {@code util/AstronomyUtil}): every body
 * with its orbit, and the celestial mechanics the sky is drawn from - Kepler positions over the body tree, then for one
 * vantage body each other body's apparent angle, inclination, size and phase. A straight port; values are hbm-space's
 * {@code SolarSystem.init()} (KSP's, converted), the body tree generated from that source.
 */
public final class SolarSystem {

    // AstronomyUtil
    public static final double GRAVITATIONAL_CONSTANT = 6.6743015e-11F;
    public static final long SECONDS_IN_DAY = 20 * 60;
    public static final long SECONDS_IN_KSP_DAY = 6 * 60 * 60;
    public static final long TICKS_IN_DAY = SECONDS_IN_DAY * 20;
    public static final double DAY_FACTOR = (double) SECONDS_IN_DAY / (double) SECONDS_IN_KSP_DAY;
    /** Server-authoritative celestial rate; changed by the Time Wand and published by CelestialTimeSync. */
    public static volatile long TIME_MULTIPLIER = 1;
    /** The local client's received rate, kept separate from an integrated server's authority. */
    public static volatile long CLIENT_TIME_MULTIPLIER = 1;

    public static long timeMultiplier(Level level) {
        return level.isClientSide() ? CLIENT_TIME_MULTIPLIER : TIME_MULTIPLIER;
    }

    // SolarSystem
    public static final double RENDER_SCALE = 180;
    public static final double SUN_RENDER_SCALE = 4;
    public static final float MAX_APPARENT_SIZE_SURFACE = 24;
    /** {@code SolarSystem.MAX_APPARENT_SIZE_ORBIT}: how large the orbited body may be drawn from a station. */
    public static final float MAX_APPARENT_SIZE_ORBIT = 160;
    /** {@code AstronomyUtil.KM_IN_AU}: Kerbin's orbit is 1 AU. */
    public static final float KM_IN_AU = 13_599_840F;
    /** {@code AstronomyUtil.DEFAULT_ALTITUDE_KM}: the parking orbit above a body's surface for transfer costs. */
    public static final float DEFAULT_ALTITUDE_KM = 100;
    private static final int ECCENTRICITY_ITERATION_COUNT = 4;

    /** One body: hbm-space's {@code CelestialBody} values the sky reads. */
    public static final class Body {
        public final String name;
        public final float massKg;
        public final float radiusKm;
        public final float semiMajorAxisKm;
        public final float eccentricity;
        public final float semiMinorAxisFactor;
        public final float argumentPeriapsis;
        public final float inclination;
        public final float ascendingNode;
        private final int rotationalPeriod;
        public final float[] color;
        public final float axialTilt;
        public final String tidallyLockedTo;
        public final float[] rings;
        public Body parent;
        public final List<Body> satellites = new ArrayList<>();

        Body(String name, float massKg, float radiusKm, float semiMajorAxisKm, float eccentricity, float argumentPeriapsisDegrees,
             float inclinationDegrees, float ascendingNodeDegrees, int rotationalPeriod, float[] color, float axialTilt,
             String tidallyLockedTo, float[] rings, Body... satellites) {
            this.name = name;
            this.massKg = massKg;
            this.radiusKm = radiusKm;
            this.semiMajorAxisKm = semiMajorAxisKm;
            this.eccentricity = eccentricity;
            this.semiMinorAxisFactor = (float) Math.sqrt(1 - eccentricity * eccentricity);
            this.argumentPeriapsis = (float) Math.toRadians(argumentPeriapsisDegrees);
            this.inclination = (float) Math.toRadians(inclinationDegrees);
            this.ascendingNode = (float) Math.toRadians(ascendingNodeDegrees);
            this.rotationalPeriod = rotationalPeriod;
            this.color = color;
            this.axialTilt = axialTilt;
            this.tidallyLockedTo = tidallyLockedTo;
            this.rings = rings;
            for (Body satellite : satellites) {
                satellite.parent = this;
                this.satellites.add(satellite);
            }
        }

        /** {@code textures/misc/space/<name>.png} in NTM: Space's namespace. */
        public String texture() {
            return "textures/misc/space/" + name + ".png";
        }

        public Body getStar() {
            Body body = this;
            while (body.parent != null) body = body.parent;
            return body;
        }

        public Body getPlanet() {
            if (this.parent == null) return this;
            Body body = this;
            while (body.parent.parent != null) body = body.parent;
            return body;
        }

        /** In ticks. */
        public double getRotationalPeriod() {
            return getRotationalPeriod(TIME_MULTIPLIER);
        }

        public double getClientRotationalPeriod() {
            return getRotationalPeriod(CLIENT_TIME_MULTIPLIER);
        }

        public double getRotationalPeriod(long timeMultiplier) {
            return (double) rotationalPeriod * (DAY_FACTOR / (double) timeMultiplier) * 20;
        }

        /** {@code CelestialBody.getSunPower}: the sun's strength here relative to Kerbin's, by the planet's distance. */
        public float getSunPower() {
            float distanceAU = getPlanet().semiMajorAxisKm / KM_IN_AU;
            return 1 / (distanceAU * distanceAU);
        }

        /** {@code CelestialBody.getSurfaceGravity}, in m/s^2. */
        public float getSurfaceGravity() {
            float radius = radiusKm * 1000;
            return G_FLOAT * massKg / (radius * radius);
        }

        /** The year length in KSP days, from the semi-major axis. */
        public double getOrbitalPeriod() {
            double semiMajorAxis = semiMajorAxisKm * 1_000;
            double orbitalPeriod = 2 * Math.PI * Math.sqrt((semiMajorAxis * semiMajorAxis * semiMajorAxis)
                    / (GRAVITATIONAL_CONSTANT * parent.massKg));
            return orbitalPeriod / (double) SECONDS_IN_KSP_DAY;
        }
    }

    /** A body as seen from another ({@code AstroMetric}). */
    public static final class AstroMetric {
        public final Body body;
        public double[] position;
        public double distance;
        public double apparentSize;
        public double angle;
        public double inclination;
        public double phase;
        public double phaseObscure;

        AstroMetric(Body body, double[] position) {
            this.body = body;
            this.position = position;
        }
    }

    public static final Body KERBOL =
        new Body("kerbol", 1.757e+28F, 261600.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 432000, new float[] {1.0F, 0.8667F, 0.0F}, 0.0F, null, null,
            new Body("moho", 2.526e+21F, 250.0F, 5263138.0F, 0.2F, 15.0F, 7.0F, 70.0F, 210000, new float[] {0.4863F, 0.4F, 0.3456F}, 30.0F, null, null),
            new Body("eve", 1.224e+23F, 700.0F, 9832684.0F, 0.01F, 0.0F, 2.1F, 15.0F, 80500, new float[] {0.408F, 0.298F, 0.553F}, 0.0F, null, null,
                new Body("gilly", 1.242e+17F, 13.0F, 31500.0F, 0.55F, 10.0F, 12.0F, 80.0F, 28255, new float[] {0.612F, 0.494F, 0.439F}, 0.0F, null, null)),
            new Body("kerbin", 5.292e+22F, 600.0F, 13599840.0F, 0.0F, 0.0F, 0.0F, 0.0F, 21549, new float[] {0.608F, 0.914F, 1.0F}, 0.0F, null, null,
                new Body("mun", 9.76e+20F, 200.0F, 12000.0F, 0.054F, 0.0F, 5.15F, 17.0F, 138984, new float[] {0.51F, 0.51F, 0.51F}, 0.0F, "kerbin", null),
                new Body("minmus", 2.646e+19F, 60.0F, 47000.0F, 0.0F, 38.0F, 6.0F, 78.0F, 40400, new float[] {0.6F, 0.882F, 0.764F}, 0.0F, null, null)),
            new Body("duna", 4.515e+21F, 320.0F, 20726155.0F, 0.05F, 0.0F, 0.06F, 135.5F, 65518, new float[] {0.6471F, 0.2824F, 0.1608F}, 0.0F, "ike", null,
                new Body("ike", 2.782e+20F, 130.0F, 3200.0F, 0.03F, 0.0F, 0.2F, 0.0F, 65518, new float[] {0.533F, 0.502F, 0.526F}, 0.0F, "duna", null)),
            new Body("dres", 3.219e+20F, 138.0F, 40839348.0F, 0.145F, 90.0F, 5.0F, 280.0F, 34800, new float[] {0.318F, 0.306F, 0.318F}, 0.0F, null, new float[] {10.0F, 3.0F, 0.4F, 0.4F, 0.4F}),
            new Body("jool", 4.233e+24F, 6000.0F, 68773560.0F, 0.05F, 0.0F, 1.304F, 52.0F, 36000, new float[] {0.4588F, 0.6784F, 0.3059F}, 0.0F, null, null,
                new Body("laythe", 2.94e+22F, 500.0F, 27184.0F, 0.0288F, 0.0F, 0.348F, 0.0F, 52981, new float[] {0.388F, 0.384F, 0.851F}, 0.0F, "jool", null),
                new Body("vall", 3.109e+21F, 300.0F, 43152.0F, 0.111F, 342.9F, 7.48F, 128.0F, 105962, new float[] {0.773F, 0.843F, 0.855F}, 0.0F, null, null),
                new Body("tylo", 4.233e+22F, 600.0F, 68500.0F, 0.002F, 0.0F, 0.3F, 0.0F, 211926, new float[] {0.875F, 0.863F, 0.851F}, 0.0F, null, null),
                new Body("bop", 3.726e+19F, 65.0F, 128500.0F, 0.235F, 25.0F, 15.0F, 10.0F, 544507, new float[] {0.329F, 0.302F, 0.263F}, 0.0F, null, null),
                new Body("pol", 1.081e+19F, 44.0F, 179890.0F, 0.171F, 15.0F, 4.25F, 2.0F, 901902, new float[] {0.541F, 0.478F, 0.373F}, 0.0F, null, null)),
            new Body("sarnus", 1.223e+24F, 5300.0F, 125798522.0F, 0.0534F, 0.0F, 2.02F, 184.0F, 28500, new float[] {1.0F, 0.6862F, 0.5882F}, 0.0F, null, new float[] {10.0F, 3.0F, 0.6F, 0.4F, 0.3F},
                new Body("hale", 1.2166e+16F, 6.0F, 10488.0F, 0.0F, 0.0F, 1.0F, 55.0F, 23555, new float[] {0.306F, 0.259F, 0.235F}, 0.0F, null, null),
                new Body("ovok", 4.233e+17F, 26.0F, 12169.0F, 0.01F, 0.0F, 1.5F, 55.0F, 29440, new float[] {0.361F, 0.361F, 0.361F}, 0.0F, null, null),
                new Body("eeloo", 1.115e+21F, 210.0F, 19106.0F, 0.0034F, 0.0F, 2.3F, 55.0F, 57915, new float[] {0.62F, 0.647F, 0.655F}, 0.0F, null, null),
                new Body("slate", 2.965e+22F, 540.0F, 42593.0F, 0.04F, 0.0F, 2.3F, 55.0F, 192771, new float[] {0.81F, 0.62F, 0.483F}, 0.0F, null, null),
                new Body("tekto", 2.883e+21F, 480.0F, 67355.0F, 0.028F, 0.0F, 9.4F, 55.0F, 57915, new float[] {0.357F, 0.47F, 0.313F}, 25.0F, null, null)),
            new Body("urlum", 1.7896e+23F, 2177.0F, 254317012.0F, 0.045F, 0.0F, 0.64F, 61.0F, 41000, new float[] {0.67F, 0.86F, 0.922F}, 0.0F, null, new float[] {6.0F, 3.0F, 0.6F, 0.8F, 0.9F},
                new Body("polta", 1.3512e+21F, 220.0F, 11728.0F, 0.002F, 0.6F, 2.45F, 40.0F, 73017, new float[] {0.31F, 0.377F, 0.33F}, 0.0F, null, null),
                new Body("priax", 5.0691e+19F, 74.0F, 11728.0F, 0.002F, 0.0F, 15.5F, 40.0F, 73017, new float[] {0.251F, 0.24F, 0.212F}, 0.0F, null, null),
                new Body("wal", 7.4427e+21F, 370.0F, 67553.0F, 0.023F, 0.0F, 1.9F, 40.0F, 1009410, new float[] {0.502F, 0.42F, 0.224F}, 0.0F, null, null,
                    new Body("tal", 3.2e+18F, 22.0F, 3109.0F, 0.0F, 0.0F, 1.9F, 40.0F, 48874, new float[] {0.596F, 0.51F, 0.416F}, 0.0F, null, null))),
            new Body("neidon", 2.1228e+23F, 2145.0F, 409355192.0F, 0.0534F, 0.0F, 2.02F, 184.0F, 40250, new float[] {0.424F, 0.298F, 0.659F}, 0.0F, null, null,
                new Body("thatmo", 2.788e+21F, 286.0F, 32301.0F, 0.0534F, 0.0F, 4.02F, 284.0F, 306443, new float[] {0.757F, 0.765F, 0.773F}, 0.0F, null, null),
                new Body("nissee", 5.951e+18F, 30.0F, 487744.0F, 0.0534F, 0.0F, 45.02F, 84.0F, 27924, new float[] {0.173F, 0.149F, 0.118F}, 0.0F, null, null)),
            new Body("plock", 7.768e+20F, 189.0F, 535833706.0F, 0.26F, 0.5F, 6.15F, 260.0F, 106309, new float[] {0.737F, 0.592F, 0.443F}, 0.0F, null, null,
                new Body("karen", 7.014e+19F, 85.0F, 2458.0F, 0.0F, 50.0F, 0.0F, 260.0F, 106327, new float[] {0.483F, 0.392F, 0.341F}, 0.0F, null, null)));

    private static final Map<String, Body> BY_NAME = new HashMap<>();

    static {
        index(KERBOL);
    }

    private static void index(Body body) {
        BY_NAME.put(body.name, body);
        for (Body satellite : body.satellites) index(satellite);
    }

    private SolarSystem() {}

    public static Body body(String name) {
        return BY_NAME.get(name);
    }

    /** The legacy CelestialBody lookup: an unknown name denotes the overworld's body. */
    public static Body getBody(String name) {
        return BY_NAME.getOrDefault(name, BY_NAME.get("kerbin"));
    }

    // ---------------------------------------------------------------------------------------- time of day

    /** {@code WorldProviderCelestial.getDayLength}: a solar day on {@code body}, in ticks. */
    public static double dayLength(Body body) {
        return dayLength(body, TIME_MULTIPLIER);
    }

    public static double dayLength(Body body, long timeMultiplier) {
        return body.getRotationalPeriod(timeMultiplier) / (1 - (1 / body.getPlanet().getOrbitalPeriod()));
    }

    /** {@code WorldProviderCelestial.calculateCelestialAngle}: the SOLAR angle on {@code body} (0 noon, 0.5 midnight). */
    public static float solarAngle(Body body, long worldTime, float partialTicks) {
        return solarAngle(body, worldTime, partialTicks, TIME_MULTIPLIER);
    }

    public static float solarAngle(Body body, long worldTime, float partialTicks, long timeMultiplier) {
        double dayLength = dayLength(body, timeMultiplier);
        double j = worldTime % dayLength;
        double f1 = (j + partialTicks) / dayLength - 0.25F;
        if (f1 < 0.0F) ++f1;
        if (f1 > 1.0F) --f1;
        double f2 = f1;
        f1 = 0.5F - Math.cos(f1 * Math.PI) / 2.0F;
        return (float) (f2 + (f1 - f2) / 3.0D);
    }

    // ---------------------------------------------------------------------------------------- mechanics

    /** The level observer uses game time, independently of the level's adjustable day clock. */
    public static List<AstroMetric> calculateMetricsFromBody(Level level, float partialTicks,
                                                            Body body, float solarAngle) {
        return calculateMetricsFromBody((double) level.getGameTime() + partialTicks, body, solarAngle, timeMultiplier(level));
    }

    /** {@code calculateMetricsFromBody}: every other body as seen from {@code body}, farthest first. */
    public static List<AstroMetric> calculateMetricsFromBody(double totalWorldTime, Body body, float solarAngle) {
        return calculateMetricsFromBody(totalWorldTime, body, solarAngle, TIME_MULTIPLIER);
    }

    public static List<AstroMetric> calculateClientMetricsFromBody(double totalWorldTime, Body body, float solarAngle) {
        return calculateMetricsFromBody(totalWorldTime, body, solarAngle, CLIENT_TIME_MULTIPLIER);
    }

    public static List<AstroMetric> calculateMetricsFromBody(double totalWorldTime, Body body, float solarAngle, long timeMultiplier) {
        List<AstroMetric> metrics = new ArrayList<>();

        double rotOffset = -120.0F;
        Body tidalLockedBody = body.tidallyLockedTo != null ? body(body.tidallyLockedTo) : null;
        if (tidalLockedBody != null) {
            // If locked to parent, adjust child
            if (tidalLockedBody.getPlanet() != body) {
                tidalLockedBody = body;
                rotOffset += 180.0F;
            }
        }

        double ticks = totalWorldTime * (double) timeMultiplier;
        calculatePositionsRecursive(metrics, null, body.getStar(), ticks, tidalLockedBody, solarAngle * 360.0, rotOffset);
        calculateMetricsFromBody(metrics, body);
        metrics.sort((a, b) -> (int) (b.distance - a.distance));
        return metrics;
    }

    private static void calculatePositionsRecursive(List<AstroMetric> metrics, AstroMetric parentMetric, Body body, double ticks,
                                                    Body lockBody, double solarAngle, double rotOffset) {
        double[] parentPosition = parentMetric != null ? parentMetric.position : new double[3];
        for (Body satellite : body.satellites) {
            double[] position = satellite == lockBody
                    ? calculatePositionFromAngle(satellite, solarAngle + calculateSiderealAngle(satellite, ticks)
                            - Math.toDegrees(satellite.argumentPeriapsis) - Math.toDegrees(satellite.ascendingNode) + rotOffset)
                    : calculatePositionFromTime(satellite, ticks);
            position = new double[] {position[0] + parentPosition[0], position[1] + parentPosition[1],
                    position[2] + parentPosition[2]};
            AstroMetric metric = new AstroMetric(satellite, position);
            metrics.add(metric);
            calculatePositionsRecursive(metrics, metric, satellite, ticks, lockBody, solarAngle, rotOffset);
        }
    }

    private static double[] calculatePositionFromTime(Body body, double ticks) {
        double yearTicks = body.getOrbitalPeriod() * (double) TICKS_IN_DAY;
        double meanAnomaly = 2 * Math.PI * (ticks / yearTicks);
        return calculatePosition(body, meanAnomaly, body.semiMajorAxisKm);
    }

    private static double[] calculatePositionFromAngle(Body body, double angle) {
        return calculatePosition(body, Math.toRadians(angle), body.semiMajorAxisKm);
    }

    private static double[] calculatePosition(Body body, double meanAnomaly, double semiMajorAxis) {
        double eccentricAnomaly = calculateEccentricAnomaly(meanAnomaly, body.eccentricity);

        // Orbital plane
        double x = semiMajorAxis * (Math.cos(eccentricAnomaly) - body.eccentricity);
        double y = semiMajorAxis * body.semiMinorAxisFactor * Math.sin(eccentricAnomaly);
        double z;

        // Rotate by argument of periapsis
        double px = x;
        x = Math.cos(body.argumentPeriapsis) * px - Math.sin(body.argumentPeriapsis) * y;
        y = Math.sin(body.argumentPeriapsis) * px + Math.cos(body.argumentPeriapsis) * y;

        // Rotate by inclination
        z = Math.sin(body.inclination) * y;
        y = Math.cos(body.inclination) * y;

        // Rotate by longitude of ascending node
        px = x;
        x = Math.cos(body.ascendingNode) * px - Math.sin(body.ascendingNode) * y;
        y = Math.sin(body.ascendingNode) * px + Math.cos(body.ascendingNode) * y;

        return new double[] {x, y, z};
    }

    private static double calculateEccentricAnomaly(double meanAnomaly, float eccentricity) {
        double eccentricAnomaly = meanAnomaly;
        for (int i = 0; i < ECCENTRICITY_ITERATION_COUNT; i++) {
            eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(eccentricAnomaly);
        }
        return eccentricAnomaly;
    }

    private static void calculateMetricsFromBody(List<AstroMetric> metrics, Body body) {
        AstroMetric from = null;
        for (AstroMetric metric : metrics) {
            if (metric.body == body) {
                from = metric;
                break;
            }
        }
        for (AstroMetric to : metrics) {
            if (from == to) continue;
            calculateMetric(to, from.position);
        }
    }

    private static void calculateMetric(AstroMetric metric, double[] position) {
        metric.distance = distance(position, metric.position);
        metric.apparentSize = getApparentSize(Math.min(metric.body.radiusKm, 3_000), metric.distance);
        metric.angle = getApparentAngleDegrees(position, metric.position);
        metric.inclination = getApparentInclinationDegrees(position, metric.position);
        metric.phase = getPhase(position, metric.position);
        metric.phaseObscure = getPhaseObscure(position, metric.position);
    }

    private static double distance(double[] a, double[] b) {
        double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double getApparentSize(double radius, double distance) {
        return 2D * (float) Math.atan((2D * radius) / (2D * distance)) * RENDER_SCALE;
    }

    private static double getApparentAngleDegrees(double[] from, double[] to) {
        double angleToOrigin = Math.atan2(-from[1], -from[0]);
        double angleToTarget = Math.atan2(to[1] - from[1], to[0] - from[0]);
        return wrapDegrees(Math.toDegrees(angleToOrigin - angleToTarget));
    }

    private static double getApparentInclinationDegrees(double[] from, double[] to) {
        double x = from[0] - to[0];
        double y = from[1] - to[1];
        double planeDistance = Math.sqrt(x * x + y * y);
        double offsetDistance = from[2] - to[2];
        return wrapDegrees(Math.toDegrees(Math.atan2(offsetDistance, planeDistance)));
    }

    private static double getPhase(double[] from, double[] to) {
        return getApparentAngleDegrees(to, from) / 180.0;
    }

    private static double getPhaseObscure(double[] from, double[] to) {
        return Math.min(Math.abs(getPhase(from, to)), 1.0F - Math.abs(getApparentInclinationDegrees(from, to) / 180.0F));
    }

    /** {@code calculateSunSize}: how large the sun is drawn from {@code from}. */
    public static double calculateSunSize(Body from) {
        if (from.parent == null) return 0;
        if (from.parent.parent != null) return calculateSunSize(from.parent);
        return getApparentSize(from.parent.radiusKm, from.semiMajorAxisKm);
    }

    public static double calculateSiderealAngle(Body body, double ticks) {
        body = body.getPlanet();
        double[] position = calculatePositionFromTime(body, ticks);
        return Math.toDegrees(Math.atan2(position[1], position[0]));
    }

    /** 1.7 {@code MathHelper.wrapAngleTo180_double}. */
    private static double wrapDegrees(double value) {
        value %= 360.0D;
        if (value >= 180.0D) value -= 360.0D;
        if (value < -180.0D) value += 360.0D;
        return value;
    }

    // ---------------------------------------------------------------------------------------- orbits (stations)

    /**
     * 1.7's {@code AstronomyUtil.GRAVITATIONAL_CONSTANT} is a float, so its products with a float mass are float
     * products; the station and transfer formulas below keep that precision (the sky's are this class's double).
     */
    public static final float G_FLOAT = 6.6743015e-11F;

    /**
     * {@code calculateMetricsFromSatellite}: every body as seen from a satellite (an orbital station) circling
     * {@code orbiting} at {@code altitude} metres from its centre, farthest first. Nothing is skipped as "self": the
     * orbited body is a metric like the others, drawn huge below.
     */
    public static List<AstroMetric> calculateMetricsFromSatellite(double ticks, Body orbiting, double altitude) {
        List<AstroMetric> metrics = new ArrayList<>();
        calculatePositionsRecursive(metrics, null, orbiting.getStar(), ticks, null, 0, 0);
        double[] position = calculatePositionSatellite(orbiting, altitude, ticks);
        for (AstroMetric metric : metrics) {
            if (metric.body == orbiting) {
                position = add(position, metric.position);
                break;
            }
        }
        for (AstroMetric to : metrics) calculateMetric(to, position);
        metrics.sort((a, b) -> (int) (b.distance - a.distance));
        return metrics;
    }

    /** {@code calculateMetricsBetweenSatelliteOrbits}: as above from a point {@code t} of the way between two orbits. */
    public static List<AstroMetric> calculateMetricsBetweenSatelliteOrbits(double ticks, Body from, Body to, double fromAltitude,
                                                                          double toAltitude, double t) {
        List<AstroMetric> metrics = new ArrayList<>();
        calculatePositionsRecursive(metrics, null, from.getStar(), ticks, null, 0, 0);
        double[] fromPos = calculatePositionSatellite(from, fromAltitude, ticks);
        double[] toPos = calculatePositionSatellite(to, toAltitude, ticks);
        for (AstroMetric metric : metrics) {
            if (metric.body == from) fromPos = add(fromPos, metric.position);
            if (metric.body == to) toPos = add(toPos, metric.position);
        }
        // Lerp smoothly between the two positions (1.7: BobMathUtil.clampedLerp per axis)
        double[] position = {clampedLerp(fromPos[0], toPos[0], t), clampedLerp(fromPos[1], toPos[1], t),
                clampedLerp(fromPos[2], toPos[2], t)};
        for (AstroMetric metric : metrics) calculateMetric(metric, position);
        metrics.sort((a, b) -> (int) (b.distance - a.distance));
        return metrics;
    }

    /** {@code calculateDistanceBetweenTwoBodies}: the distance between two bodies' centres now, in km. */
    public static double calculateDistanceBetweenTwoBodies(double ticks, Body from, Body to) {
        List<AstroMetric> metrics = new ArrayList<>();
        calculatePositionsRecursive(metrics, null, from.getStar(), ticks, null, 0, 0);
        double[] fromPos = new double[3];
        double[] toPos = new double[3];
        for (AstroMetric metric : metrics) {
            if (metric.body == from) fromPos = metric.position;
            if (metric.body == to) toPos = metric.position;
        }
        return distance(fromPos, toPos);
    }

    /** The observer angle from the level's current game-time positions, without a surface tidal lock. */
    public static double calculateSingleAngle(Level level, Body from, Body to) {
        var metrics = new ArrayList<AstroMetric>();
        calculatePositionsRecursive(metrics, null, from.getStar(),
                (double) level.getGameTime() * timeMultiplier(level), null, 0, 0);
        return calculateSingleAngle(metrics, from, to);
    }

    /** {@code calculateSingleAngle(metrics, from, to)}: the apparent angle of {@code to} seen from {@code from}. */
    public static double calculateSingleAngle(List<AstroMetric> metrics, Body from, Body to) {
        AstroMetric metricFrom = null;
        AstroMetric metricTo = null;
        for (AstroMetric metric : metrics) {
            if (metric.body == from) {
                metricFrom = metric;
            } else if (metric.body == to) {
                metricTo = metric;
            }
        }
        if (metricFrom == null || metricTo == null) return 0;
        return getApparentAngleDegrees(metricFrom.position, metricTo.position);
    }

    /** {@code calculateSingleAngle(world, partialTicks, metrics, orbiting, altitude)}: the orbited body seen from a satellite. */
    public static double calculateSingleAngle(double ticks, List<AstroMetric> metrics, Body orbiting, double altitude) {
        double[] from = calculatePositionSatellite(orbiting, altitude, ticks);
        double[] to = new double[3];
        for (AstroMetric metric : metrics) {
            if (metric.body == orbiting) {
                to = metric.position;
                from = add(from, to);
                break;
            }
        }
        return getApparentAngleDegrees(from, to);
    }

    /** {@code calculatePositionSatellite}: a circular orbit at {@code altitude} metres around {@code body}, in km. */
    private static double[] calculatePositionSatellite(Body body, double altitude, double ticks) {
        double orbitalPeriod = 2 * Math.PI * Math.sqrt((altitude * altitude * altitude) / (G_FLOAT * body.massKg));
        orbitalPeriod /= (double) SECONDS_IN_KSP_DAY;
        double orbitTicks = orbitalPeriod * (double) TICKS_IN_DAY;
        double meanAnomaly = 2 * Math.PI * (ticks / orbitTicks);
        return new double[] {altitude / 1000 * Math.cos(meanAnomaly), altitude / 1000 * Math.sin(meanAnomaly), 0};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /** {@code BobMathUtil.clampedLerp}. */
    private static double clampedLerp(double a, double b, double t) {
        if (t < 0) return a;
        if (t > 1) return b;
        return a + (b - a) * t;
    }

    // ---------------------------------------------------------------------------------------- delta-v

    /**
     * {@code getCostBetween}: millibuckets of each of two propellants a craft of {@code mass} kg needs to get from
     * {@code from} to {@code to}; {@code fromPressure}/{@code toPressure} are the bodies' air pressures in atm (1.7 read
     * them from {@code CBT_Atmosphere}, 0 without air).
     */
    public static int getCostBetween(Body from, Body to, int mass, int thrust, int isp, boolean fromOrbit, boolean toOrbit,
                                     double fromPressure, double toPressure) {
        double fromDrag = getAtmosphericDrag(fromPressure);
        double toDrag = getAtmosphericDrag(toPressure);
        double launchDV = fromOrbit ? 0 : getLiftoffDeltaV(from, mass, thrust, fromDrag);
        double travelDV = getDeltaVBetween(from, to);
        double landerDV = toOrbit ? 0 : getLandingDeltaV(to, mass, thrust, toDrag);
        double totalDV = launchDV + travelDV + landerDV;
        return getFuelCost(totalDV, mass, isp);
    }

    /**
     * {@code getFuelCost}: the propellant (mB of each of two) to change the velocity of {@code mass} kg by {@code deltaV}
     * m/s at {@code isp} seconds, rounded up to whole 100 mB.
     */
    public static int getFuelCost(double deltaV, int mass, int isp) {
        double g0 = 9.81;
        double exhaustVelocity = isp * g0;
        double massFraction = 1 - Math.exp(-(deltaV / exhaustVelocity));
        double totalMass = mass / (1 - massFraction);
        double propellantMass = totalMass - mass;
        double propellantVolume = propellantMass / 2; // two propellants
        return propellantVolume + 100 > Integer.MAX_VALUE ? Integer.MAX_VALUE : ceil(propellantVolume * 0.01D) * 100;
    }

    /** 1.7 {@code MathHelper.ceiling_double_int}. */
    private static int ceil(double value) {
        int i = (int) value;
        return value > (double) i ? i + 1 : i;
    }

    /** {@code getAtmosphericDrag} from the air pressure (atm). */
    private static double getAtmosphericDrag(double pressure) {
        if (pressure <= 0) return 0;
        return Math.log(pressure + 1.0D) / 10.0D;
    }

    /** {@code getLiftoffDeltaV}: surface to orbit, ignoring friction; thrust in newtons. */
    public static double getLiftoffDeltaV(Body body, float craftMassKg, float craftThrustN, double atmosphericDrag) {
        return calculateSurfaceToOrbitDeltaV(body, craftMassKg, craftThrustN, atmosphericDrag, false);
    }

    /** {@code getLandingDeltaV}: aerobraking where there is air. */
    public static double getLandingDeltaV(Body body, float craftMassKg, float craftThrustN, double atmosphericDrag) {
        return calculateSurfaceToOrbitDeltaV(body, craftMassKg, craftThrustN, atmosphericDrag, atmosphericDrag > 0.006);
    }

    private static double calculateSurfaceToOrbitDeltaV(Body body, float craftMassKg, float craftThrustN, double atmosphericDrag,
                                                        boolean lossesOnly) {
        float gravity = body.getSurfaceGravity();
        double orbitalDeltaV = Math.sqrt((G_FLOAT * body.massKg) / (body.radiusKm * 1_000));
        double thrustToWeightRatio = craftThrustN / (craftMassKg * gravity);
        if (thrustToWeightRatio < 1) return Double.MAX_VALUE;
        double acceleration = (thrustToWeightRatio - 1) * gravity;
        double timeToOrbit = orbitalDeltaV / acceleration;
        double gravityLosses = gravity * timeToOrbit * 2; // No perfect burns
        if (lossesOnly) return gravityLosses * (1 - atmosphericDrag); // drag helps on the way down
        return orbitalDeltaV + gravityLosses * (1 + atmosphericDrag); // and hinders on the way up
    }

    /**
     * {@code getDeltaVBetween}: the Hohmann transfer from the orbit of {@code start} to the orbit of {@code end}, two
     * burns. Transfers to or from the star are not supported (1.7 threw; this returns 0, never asked by the port).
     */
    public static double getDeltaVBetween(Body start, Body end) {
        if (start == end) return 0;
        if (start.parent == null || end.parent == null) return 0;
        if (start.parent == end.parent) {
            // Intersystem transfer
            double first = hohmann(start.parent.massKg, start.semiMajorAxisKm, end.semiMajorAxisKm, start.massKg,
                    start.radiusKm + DEFAULT_ALTITUDE_KM);
            double second = hohmann(start.parent.massKg, end.semiMajorAxisKm, start.semiMajorAxisKm, end.massKg,
                    end.radiusKm + DEFAULT_ALTITUDE_KM);
            return first + second;
        } else if (start == end.parent) {
            // Transferring from parent body to moon
            double first = hohmann(start.massKg, start.radiusKm + DEFAULT_ALTITUDE_KM, end.semiMajorAxisKm);
            double second = hohmann(start.massKg, end.semiMajorAxisKm, start.radiusKm + DEFAULT_ALTITUDE_KM, end.massKg,
                    end.radiusKm + DEFAULT_ALTITUDE_KM);
            return first + second;
        } else if (start.parent == end) {
            // Transferring from moon to parent body
            double first = hohmann(end.massKg, start.semiMajorAxisKm, end.radiusKm + DEFAULT_ALTITUDE_KM, start.massKg,
                    start.radiusKm + DEFAULT_ALTITUDE_KM);
            double second = hohmann(end.massKg, end.radiusKm + DEFAULT_ALTITUDE_KM, start.semiMajorAxisKm);
            return first + second;
        }
        // Complex transfer (moon -> moon, moon -> other planet); 1.7's loops kept as written, including that the
        // escape burn of each moon uses the moon's own orbit radius as its parking orbit
        Body commonParent = getCommonParent(start, end);
        Body fromBody = start;
        Body toBody = end;
        float currentFromOrbitRadius = fromBody.semiMajorAxisKm;
        float currentToOrbitRadius = toBody.semiMajorAxisKm;
        double burnCost = 0;
        while (fromBody.parent != commonParent) {
            burnCost += hohmann(fromBody.parent.massKg, fromBody.semiMajorAxisKm, fromBody.semiMajorAxisKm, fromBody.massKg,
                    fromBody.semiMajorAxisKm);
            currentFromOrbitRadius = fromBody.semiMajorAxisKm;
            fromBody = fromBody.parent;
        }
        while (toBody.parent != commonParent) {
            burnCost += hohmann(toBody.parent.massKg, toBody.semiMajorAxisKm, toBody.semiMajorAxisKm, toBody.massKg,
                    toBody.semiMajorAxisKm);
            currentToOrbitRadius = toBody.semiMajorAxisKm;
            toBody = toBody.parent;
        }
        burnCost += hohmann(commonParent.massKg, fromBody.semiMajorAxisKm, toBody.semiMajorAxisKm, fromBody.massKg, currentFromOrbitRadius);
        burnCost += hohmann(commonParent.massKg, toBody.semiMajorAxisKm, fromBody.semiMajorAxisKm, toBody.massKg, currentToOrbitRadius);
        return burnCost;
    }

    private static Body getCommonParent(Body start, Body end) {
        Body startParent = start.parent;
        while (startParent != null) {
            Body endParent = end.parent;
            while (endParent != null) {
                if (startParent == endParent) return startParent;
                endParent = endParent.parent;
            }
            startParent = startParent.parent;
        }
        throw new IllegalArgumentException("Bodies aren't in the same solar system");
    }

    /** Orbit to orbit around a parent, no well to escape (low orbit to a moon). */
    private static double hohmann(float parentMassKg, float fromRadiusKm, float toRadiusKm) {
        double mu = parentMassKg * G_FLOAT;
        double startOrbitalRadius = fromRadiusKm * 1_000;
        double endOrbitalRadius = toRadiusKm * 1_000;
        double transferSemiMajorAxis = (startOrbitalRadius + endOrbitalRadius) / 2;
        double currentOrbitalVelocity = Math.sqrt(mu / startOrbitalRadius);
        double requiredVelocity = Math.sqrt(mu * ((2 / startOrbitalRadius) - (1 / transferSemiMajorAxis)));
        return Math.abs(requiredVelocity - currentOrbitalVelocity);
    }

    /** Orbit to orbit around a parent, escaping from the well of {@code fromMassKg} at a parking orbit. */
    private static double hohmann(float parentMassKg, float fromRadiusKm, float toRadiusKm, float fromMassKg,
                                  float parkingOrbitRadiusKm) {
        double hyperbolicVelocity = hohmann(parentMassKg, fromRadiusKm, toRadiusKm);
        double fromMu = fromMassKg * G_FLOAT;
        double parkingOrbitRadius = parkingOrbitRadiusKm * 1_000;
        double parkingOrbitVelocity = Math.sqrt(fromMu / parkingOrbitRadius);
        double escapeVelocity = Math.sqrt((2 * fromMu) / parkingOrbitRadius);
        double escapeHyperVelocity = Math.sqrt(hyperbolicVelocity * hyperbolicVelocity + escapeVelocity * escapeVelocity);
        return escapeHyperVelocity - parkingOrbitVelocity;
    }

    // ---------------------------------------------------------------- the orrery (hbm-space SolarSystem, OrreryMetric)

    /** 1.7 {@code ORRERY_MAX_RADIUS}: the largest radius (km) a body is spaced and drawn by. */
    public static final double ORRERY_MAX_RADIUS = 20_000;
    /** 1.7 {@code ORRERY_MIN_RADIUS}: the smallest radius (km) a moon is spaced by. */
    public static final double ORRERY_MIN_RADIUS = 2_000;

    /** 1.7 {@code OrreryMetric}: a body's squeezed position about its parent and its 32-point orbit, in km (y = up the orbital plane). */
    public static final class OrreryMetric {
        private static final int PATH_RESOLUTION = 32;

        public final Body body;
        public final double[] position;
        public final double[][] orbitalPath = new double[PATH_RESOLUTION][];

        OrreryMetric(Body body, double[] position) {
            this.body = body;
            this.position = position;
        }
    }

    /**
     * 1.7 {@code calculatePositionsOrrery}: every body around {@code star}, depth first in satellite order, at
     * {@code ticks = (totalWorldTime + partialTicks) * TIME_MULTIPLIER}.
     */
    public static List<OrreryMetric> calculatePositionsOrrery(double totalWorldTime, float partialTicks, Body star) {
        return calculatePositionsOrrery(totalWorldTime, partialTicks, star, TIME_MULTIPLIER);
    }

    public static List<OrreryMetric> calculatePositionsOrrery(double totalWorldTime, float partialTicks, Body star, long timeMultiplier) {
        List<OrreryMetric> metrics = new ArrayList<>();
        double ticks = (totalWorldTime + partialTicks) * (double) timeMultiplier;
        calculatePositionsRecursiveOrrery(metrics, null, star, ticks);
        return metrics;
    }

    /**
     * 1.7 {@code calculatePositionsRecursiveOrrery}: a "mock" system with the orbits neatly divided. Each satellite's
     * orbit radius is the running distance from the parent: the parent's radius (at most the maximum) times 1.42, then
     * per satellite its clamped radius times {@code (1 + e) * 2 * 1.42} plus, per moon of it, the moon's clamped radius
     * times {@code (1 + e) * 2 * 2} - added once before placing the satellite and once after.
     */
    private static void calculatePositionsRecursiveOrrery(List<OrreryMetric> metrics, OrreryMetric parentMetric, Body body,
                                                          double ticks) {
        double[] parent = parentMetric != null ? parentMetric.position : new double[3];

        double distance = Math.min(body.radiusKm, ORRERY_MAX_RADIUS) * 1.42;
        for (Body satellite : body.satellites) {
            double extraDistance = clamp(satellite.radiusKm, ORRERY_MIN_RADIUS, ORRERY_MAX_RADIUS) * ((1 + satellite.eccentricity) * 2) * 1.42;
            for (Body inner : satellite.satellites) {
                extraDistance += clamp(inner.radiusKm, ORRERY_MIN_RADIUS, ORRERY_MAX_RADIUS) * ((1 + inner.eccentricity) * 2) * 2;
            }

            // distance is combined radii * hypotenuse (so cubes don't intersect at edges)
            distance += extraDistance;

            double[] position = calculatePositionFromTime(satellite, ticks, distance);
            position = new double[] {position[0] + parent[0], position[1] + parent[1], position[2] + parent[2]};

            OrreryMetric metric = new OrreryMetric(satellite, position);
            for (int i = 0; i < metric.orbitalPath.length; i++) {
                double[] point = calculatePosition(satellite, Math.toRadians((float) i / metric.orbitalPath.length * 360), distance);
                metric.orbitalPath[i] = new double[] {point[0] + parent[0], point[1] + parent[1], point[2] + parent[2]};
            }

            metrics.add(metric);

            distance += extraDistance;

            calculatePositionsRecursiveOrrery(metrics, metric, satellite, ticks);
        }
    }

    private static double[] calculatePositionFromTime(Body body, double ticks, double semiMajorAxis) {
        double yearTicks = body.getOrbitalPeriod() * (double) TICKS_IN_DAY;
        double meanAnomaly = 2 * Math.PI * (ticks / yearTicks);
        return calculatePosition(body, meanAnomaly, semiMajorAxis);
    }

    /** 1.7 {@code MathHelper.clamp_double}. */
    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }
}
