package com.ntmspace.block;

import com.ntmspace.registry.BodyOreBlocks;
import com.ntmspace.rocket.DriveBody;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;

/** Named celestial ore families; physical identity is the registered block plus its BODY state. */
public enum CelestialOre {
    IRON("ore_iron", true),
    GOLD("ore_gold", true),
    REDSTONE("ore_redstone", true),
    LAPIS("ore_lapis", false, DriveBody.KERBIN, DriveBody.LAYTHE),
    EMERALD("ore_emerald", false, DriveBody.KERBIN, DriveBody.LAYTHE),
    QUARTZ("ore_quartz", false, DriveBody.MUN),
    DIAMOND("ore_diamond", true),
    URANIUM("ore_uranium", true),
    PLUTONIUM("ore_plutonium", false),
    THORIUM("ore_thorium", true),
    MORKITE("ore_morkite", false),
    TITANIUM("ore_titanium", true),
    SULFUR("ore_sulfur", true),
    GLOWSTONE("ore_glowstone", false, DriveBody.MOHO),
    NITER("ore_niter", true),
    COPPER("ore_copper", true),
    NICKEL("ore_nickel", true, DriveBody.KERBIN),
    MINERAL("ore_mineral", false, DriveBody.IKE, DriveBody.MOHO),
    ZINC("ore_zinc", true),
    TUNGSTEN("ore_tungsten", true),
    ALUMINIUM("ore_aluminium", true),
    FLUORITE("ore_fluorite", true),
    BERYLLIUM("ore_beryllium", true),
    LEAD("ore_lead", true),
    OIL("ore_oil", false, DriveBody.KERBIN, DriveBody.DUNA, DriveBody.LAYTHE),
    OIL_EMPTY("ore_oil_empty", false),
    GAS("ore_gas", false, DriveBody.EVE),
    GAS_EMPTY("ore_gas_empty", false),
    LIGNITE("ore_lignite", false, DriveBody.KERBIN),
    ASBESTOS("ore_asbestos", false, DriveBody.KERBIN, DriveBody.IKE, DriveBody.LAYTHE),
    SCHRABIDIUM("ore_schrabidium", false, DriveBody.EVE),
    LITHIUM("ore_lithium", false, DriveBody.MUN, DriveBody.IKE),
    NIOBIUM("ore_niobium", false, DriveBody.DRES, DriveBody.EVE),
    CADMIUM("ore_cadmium", false),
    PALLADIUM("ore_palladium", false),
    IODINE("ore_iodine", false, DriveBody.EVE),
    ARSENIC("ore_arsenic", false),
    SILICON("ore_silicon", false),
    FIRE("ore_fire", false, DriveBody.MOHO),
    SHALE("ore_shale", false, DriveBody.MUN, DriveBody.MOHO, DriveBody.DRES),
    BRINE("ore_brine", false, DriveBody.MUN, DriveBody.MINMUS, DriveBody.IKE),
    BRINE_EMPTY("ore_brine_empty", false),
    TEKTO("ore_tekto", false, DriveBody.TEKTO),
    TEKTO_EMPTY("ore_tekto_empty", false),
    AUSTRALIUM("ore_australium", false, DriveBody.MOHO),
    RARE("ore_rare", true),
    COBALT("ore_cobalt", true),
    CINNEBAR("ore_cinnebar", true),
    COLTAN("ore_coltan", false, DriveBody.KERBIN, DriveBody.IKE, DriveBody.DRES),
    LANTHANIUM("ore_lanthanium", false, DriveBody.DRES),
    CLUSTER_IRON("cluster_iron", true),
    CLUSTER_TITANIUM("cluster_titanium", true),
    CLUSTER_ALUMINIUM("cluster_aluminium", true),
    CLUSTER_COPPER("cluster_copper", true),
    BEDROCK_OIL("ore_bedrock_oil", false, DriveBody.KERBIN);

    public final String name;
    private final Set<DriveBody> spawnLocations;

    CelestialOre(String name, boolean everywhere, DriveBody... bodies) {
        this.name = name;
        var locations = everywhere ? EnumSet.allOf(DriveBody.class) : EnumSet.noneOf(DriveBody.class);
        if (everywhere) locations.removeAll(List.of(bodies));
        else locations.addAll(List.of(bodies));
        spawnLocations = Collections.unmodifiableSet(locations);
    }

    public BlockState state(DriveBody body) {
        if (body == DriveBody.ORBIT) return host();
        var target = BlockBodyOre.Body.valueOf(body.name());
        return Objects.requireNonNull(BodyOreBlocks.bodyState(name, target),
                () -> "No registered body-state ore family " + name + " on " + body);
    }

    public BlockState host() { return BodyOreBlocks.state(name, BlockBodyOre.Body.KERBIN); }

    public Set<DriveBody> spawnsOn() { return spawnLocations; }

    public List<DriveBody> bodies() {
        return Arrays.stream(DriveBody.values()).filter(body -> body != DriveBody.ORBIT).toList();
    }
}
