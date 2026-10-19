package com.ntmspace.world.gen.noise;

/** The two-value holder hbm-space's {@code OctavePerlinNoiseSampler} uses ({@code com.hbm.util.Tuple.Pair}). */
public record Pair<K, V>(K key, V value) {

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
