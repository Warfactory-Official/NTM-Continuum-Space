package com.ntmspace.world.gen.legacy;

/** The actual horizontal write/carve geometry shared by native crater generation and cold queries. */
public record CraterGeometry(int centerX, int centerZ, int radius, int exponent) {
    public CraterGeometry {
        if (radius <= 0 || exponent <= 0) throw new IllegalArgumentException("Invalid crater geometry");
    }

    public static double maximumDepth(double radius) {
        return radius * 0.35D;
    }

    public static double depth(double distance, double radius, double maximumDepth, int exponent) {
        return -Math.pow(distance, exponent) / Math.pow(radius, exponent) * maximumDepth + maximumDepth;
    }

    private double distanceSquared(int minX, int minZ, int maxX, int maxZ) {
        var dx = (double) Math.max(minX, Math.min(centerX, maxX)) - centerX;
        var dz = (double) Math.max(minZ, Math.min(centerZ, maxZ)) - centerZ;
        return dx * dx + dz * dz;
    }

    /** The integer rand(3) rim can reline a column as far as radius + 2. */
    public boolean intersectsHorizontal(int minX, int minZ, int maxX, int maxZ) {
        var reach = radius + 2D;
        return distanceSquared(minX, minZ, maxX, maxZ) <= reach * reach;
    }

    /** Whether at least one whole block can be excavated in any column of the footprint. */
    public boolean intersectsCarving(int minX, int minZ, int maxX, int maxZ) {
        return depth(Math.sqrt(distanceSquared(minX, minZ, maxX, maxZ)), radius, maximumDepth(radius), exponent) >= 1D;
    }

    public int groundHeight(int firstFree, int x, int z, int minBuild) {
        if (firstFree <= minBuild + 1) return firstFree;
        var distance = Math.sqrt(distanceSquared(x, z, x, z));
        var carved = (int) Math.max(0D, Math.min(depth(distance, radius, maximumDepth(radius), exponent),
                firstFree - minBuild - 2));
        return firstFree - carved;
    }
}
