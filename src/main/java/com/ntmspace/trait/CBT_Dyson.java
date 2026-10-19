package com.ntmspace.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code CBT_Dyson}: the Dyson swarms around a star, each a numbered swarm (the launcher's channel number,
 * a player-chosen int, not a registry id) with its member count. Saved as 1.7's {@code swarm} int array of
 * (id, members) pairs; consumers are runtime only.
 */
public class CBT_Dyson extends CelestialBodyTrait {

    // Correlates an ID with a swarm
    private HashMap<Integer, Swarm> swarms = new HashMap<>();

    private static class Swarm {
        int members;
        int consumers;
        // Incremented whenever another consumer is added, copied to consumers at the start of a tick
        private int addedConsumers;

        Swarm(int members) {
            this.members = members;
        }
    }

    public static void launch(Level level, int id) {
        launch(level, id, 1);
    }

    public static void launch(Level level, int id, int amount) {
        String star = CelestialTraits.starOf(level);
        CBT_Dyson dyson = CelestialTraits.getTrait(star, CBT_Dyson.class, level.isClientSide());
        if (dyson == null) dyson = new CBT_Dyson();

        Swarm swarm = dyson.swarms.get(id);
        if (swarm == null) {
            swarm = new Swarm(0);
            dyson.swarms.put(id, swarm);
        }
        swarm.members += amount;

        CelestialTraits.modifyTraits(star, dyson);
    }

    public static int count(Level level, int id) {
        CBT_Dyson dyson = CelestialTraits.getTrait(CelestialTraits.starOf(level), CBT_Dyson.class, level.isClientSide());
        if (dyson == null) return 0;
        Swarm swarm = dyson.swarms.get(id);
        return swarm == null ? 0 : swarm.members;
    }

    public static int consumers(Level level, int id) {
        CBT_Dyson dyson = CelestialTraits.getTrait(CelestialTraits.starOf(level), CBT_Dyson.class, level.isClientSide());
        if (dyson == null) return 0;
        Swarm swarm = dyson.swarms.get(id);
        if (swarm == null) return 0;
        swarm.addedConsumers++;
        return swarm.consumers;
    }

    public int size() {
        int size = 0;
        for (Swarm swarm : swarms.values()) size += swarm.members;
        return size;
    }

    /**
     * Called once per tick to lower swarm counts from satellite failures, encouraging continuous automation; based on the
     * total across all swarms. Each swarm with members loses one with chance {@code (all members) / 102,400}, the total
     * read after the swarms before it decayed. Returns whether any member was lost (port: the trait is then saved and
     * synced).
     */
    public boolean attenuate() {
        boolean decayed = false;
        for (Swarm swarm : swarms.values()) {
            swarm.consumers = swarm.addedConsumers;
            swarm.addedConsumers = 0;
            if (swarm.members <= 0) continue;
            double decayChance = (double) size() / (1024 * 5 * 20);
            if (Math.random() < decayChance) {
                swarm.members--;
                decayed = true;
            }
        }
        return decayed;
    }

    /**
     * Port-only (tests and {@code /ntmdyson swarm}): sets swarm {@code id} of {@code level}'s star to {@code members},
     * creating it if needed.
     */
    public static void set(Level level, int id, int members) {
        String star = CelestialTraits.starOf(level);
        CBT_Dyson dyson = CelestialTraits.getTrait(star, CBT_Dyson.class, level.isClientSide());
        if (dyson == null) dyson = new CBT_Dyson();
        Swarm swarm = dyson.swarms.computeIfAbsent(id, k -> new Swarm(0));
        swarm.members = members;
        CelestialTraits.modifyTraits(star, dyson);
    }

    /** Port-only (tests): a swarm's member count in this trait, 0 if it has none. */
    public int members(int id) {
        Swarm swarm = swarms.get(id);
        return swarm == null ? 0 : swarm.members;
    }

    /** Port-only (tests): a stand-alone trait with the given swarms ({@code id, members} pairs). */
    public static CBT_Dyson of(int... pairs) {
        return fromPairs(pairs);
    }

    @Override
    public CBT_Dyson copy() {
        CBT_Dyson copy = new CBT_Dyson();
        for (Entry<Integer, Swarm> entry : swarms.entrySet()) copy.swarms.put(entry.getKey(), new Swarm(entry.getValue().members));
        return copy;
    }

    private int[] pairs() {
        int[] swarmData = new int[swarms.size() * 2];
        int i = 0;
        for (Entry<Integer, Swarm> entry : swarms.entrySet()) {
            swarmData[i] = entry.getKey();
            swarmData[i + 1] = entry.getValue().members;
            i += 2;
        }
        return swarmData;
    }

    private static CBT_Dyson fromPairs(int[] swarmData) {
        CBT_Dyson dyson = new CBT_Dyson();
        for (int i = 0; i + 1 < swarmData.length; i += 2) dyson.swarms.put(swarmData[i], new Swarm(swarmData[i + 1]));
        return dyson;
    }

    @Override
    public void writeToBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(swarms.size() * 2);
        for (Entry<Integer, Swarm> entry : swarms.entrySet()) {
            buf.writeShort(entry.getKey());
            buf.writeInt(entry.getValue().members);
        }
    }

    @Override
    public void readFromBytes(RegistryFriendlyByteBuf buf) {
        int count = buf.readInt();
        swarms = new HashMap<>();
        for (int i = 0; i < count; i += 2) {
            int id = buf.readShort();
            int members = buf.readInt();
            swarms.put(id, new Swarm(members));
        }
    }

    static Codec<CBT_Dyson> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().optionalFieldOf("swarm", java.util.List.of())
                        .forGetter(d -> IntStream.of(d.pairs()).boxed().toList())
        ).apply(i, list -> fromPairs(list.stream().mapToInt(Integer::intValue).toArray())));
    }
}
