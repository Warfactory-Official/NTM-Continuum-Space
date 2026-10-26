package com.ntmspace.registry;

import com.hbm.creativetabs.ModTabs;
import com.ntmspace.atmosphere.EveDeath;
import com.ntmspace.block.generic.MeltedFlesh;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.registry.Preset;
import dev.golem.core.world.Deaths;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.BlockAirPump;
import com.ntmspace.breathing.ItemLoxTank;
import com.ntmspace.breathing.ItemModOxy;
import com.ntmspace.tileentity.TileEntityAirPump;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.dejson.Damage;
import dev.golem.core.dejson.Tags;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * NTM: Space's sealed-room and breathing content (hbm-space {@code ModBlocks} / {@code ModItems} entries): the air vent
 * ({@code air_vent}, "Air Vent"), the PLSS ({@code oxy_plss}) and the oxygen tank ({@code lox_tank}), the PLSS's item
 * components, and the declared data the atmosphere system reads: the {@code oxyprime} damage type and three of the tags of
 * {@link com.ntmspace.atmosphere.Sealing}.
 */
public final class AtmosphereContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    /** 1.7: {@code BlockAirPump(Material.iron)}, hardness 5, resistance 10, machine tab; top {@code vent_chlorine_seal_top}. */
    public static final RegistryObject<BlockAirPump> AIR_VENT = REG.block("air_vent", BlockAirPump::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeBottomTop("ntmspace:blocks/vent_chlorine_seal_side", "ntmspace:blocks/vent_chlorine_seal_side",
                    "ntmspace:blocks/vent_chlorine_seal_top"))
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> AIR_VENT_ITEM = REG.blockItem(AIR_VENT).tab(HbmTab.MACHINES);

    /** 1.7 {@code TileMappings}: {@code tileentity_air_vent}; the modern id is the block's. */
    public static final RegistryObject<BlockEntityType<TileEntityAirPump>> AIR_VENT_ENTITY = REG.blockEntity("air_vent",
            () -> BlockEntities.ticking(TileEntityAirPump::new, TileEntityAirPump::clientTick, TileEntityAirPump::serverTick,
                    AIR_VENT.get()));

    /** 1.7: {@code ItemModOxy(16000, 10, 1)}, stack size 1, consumables tab. */
    public static final RegistryObject<ItemModOxy> OXY_PLSS = REG.item("oxy_plss", p -> new ItemModOxy(16000, 10, 1, p),
                    p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);

    /** 1.7: {@code ItemSyringe} "lox_tank", stack size 16, consumables tab. */
    public static final RegistryObject<ItemLoxTank> LOX_TANK = REG.item("lox_tank", ItemLoxTank::new, p -> p.stacksTo(16))
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);

    /** The PLSS's oxygen in mB (1.7 NBT {@code fuel}). */
    public static final GolemComponent<Integer> PLSS_FUEL = GolemComponent.integer(NtmSpace.MOD_ID, "plss_fuel");
    /** Ticks the PLSS has been breathed from without pause (1.7 NBT {@code ticks}; the breathing sound needs > 20). */
    public static final GolemComponent<Integer> PLSS_TICKS = GolemComponent.integer(NtmSpace.MOD_ID, "plss_ticks");

    private static BlockBehaviour.Properties flesh(
            BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.SNOW).strength(0.1F, 0.3F)
                .sound(SoundType.GRAVEL).noOcclusion().replaceable().randomTicks()
                .pushReaction(PushReaction.DESTROY);
    }

    private static Preset fleshModel(String texture) {
        return Presets.shape(Models.parented("minecraft:block/snow_height2",
                Slots.of("particle", texture, "texture", texture)));
    }

    public static final RegistryObject<MeltedFlesh> FLESH_BLOCK = REG.block("flesh_block",
            p -> new MeltedFlesh(p, () -> AtmosphereContent.CHARRED_FLESH_BLOCK.get(),
                    ParticleTypes.SMOKE, ParticleTypes.CLOUD), AtmosphereContent::flesh)
            .transform(fleshModel("ntmspace:blocks/meltedflesh")).loot(Loot.only("ntmspace:flesh"));
    public static final RegistryObject<MeltedFlesh> CHARRED_FLESH_BLOCK = REG.block("charred_flesh_block",
            p -> new MeltedFlesh(p, () -> AtmosphereContent.CARBONIZED_FLESH_BLOCK.get(),
                    ParticleTypes.LARGE_SMOKE), AtmosphereContent::flesh)
            .transform(fleshModel("ntmspace:blocks/charredflesh")).loot(Loot.only("ntmspace:grilled_flesh"));
    public static final RegistryObject<MeltedFlesh> CARBONIZED_FLESH_BLOCK = REG.block("carbonized_flesh_block",
            p -> new MeltedFlesh(p, null), AtmosphereContent::flesh)
            .transform(fleshModel("ntmspace:blocks/carbonizedflesh")).loot(Loot.only("hbm:powder_coal_tiny"));
    public static final RegistryObject<BlockItem> FLESH_BLOCK_ITEM = REG.blockItem(FLESH_BLOCK).tab(HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> CHARRED_FLESH_BLOCK_ITEM = REG.blockItem(CHARRED_FLESH_BLOCK).tab(HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> CARBONIZED_FLESH_BLOCK_ITEM = REG.blockItem(CARBONIZED_FLESH_BLOCK).tab(HbmTab.BLOCKS);

    static {
        ModTabs.insertAfter(HbmTab.BLOCKS, "hbm:trapdoor_steel",
                "ntmspace:flesh_block", "ntmspace:charred_flesh_block", "ntmspace:carbonized_flesh_block");
    }

    private AtmosphereContent() {}

    public static void init() {
        Deaths.onDeath(EveDeath::onDeath);
    }

    private static boolean declared;

    /** Dejson: the damage type and tags. */
    public static void declare() {
        if (declared) return;
        declared = true;

        // 1.7 ModDamageSource.oxyprime: setDamageIsAbsolute().setDamageBypassesArmor()
        Damage.declare(NtmSpace.MOD_ID,
                Damage.type("oxyprime").tags(Damage.BYPASSES_ARMOR, Damage.BYPASSES_EFFECTS),
                Damage.type("eve").tags(Damage.BYPASSES_ARMOR, Damage.BYPASSES_EFFECTS));

        // 1.7 IBlockSealable blocks that answer false although they are full blocks (MachineBrickFurnace, MachineDiFurnace,
        // MachineDiesel; the fan is NTM: Space's substitute block, which answers itself)
        Tags.of("block", NtmSpace.MOD_ID, "unsealable")
                .values("hbm:machine_furnace_brick_off", "hbm:machine_difurnace_off", "hbm:machine_diesel")
                // hbm-space replaced every vanilla furnace with its BlockFurnaceSpace, which never seals (so it breathes the room)
                .values("minecraft:furnace").register();

        // 1.7 ISuffocationImmune: the tag #ntmspace:suffocation_immune is filled by the mob port (registry/SpaceMobs,
        // HBM's mobs and its own); Breathing reads it.


        // 1.7 ModItemsArmor .setSealed(true): T-51, steam suit, AJR, AJRO, RPA, NCRPA, environment suit, HEV, FAU, DNS,
        // Taurun and Trenchmaster
        String[] sets = {"t51", "steamsuit", "ajr", "ajro", "rpa", "ncrpa", "envsuit", "hev", "fau", "dns", "taurun", "trenchmaster"};
        Tags.Tag sealed = Tags.of("item", NtmSpace.MOD_ID, "sealed_armor");
        for (String set : sets) {
            for (String piece : new String[] {"helmet", "plate", "legs", "boots"}) sealed.values("hbm:" + set + "_" + piece);
        }
        sealed.register();

        // 1.7 ItemModInsert.withCorrosionProtection(): insert_cmb, insert_yharonite
        Tags.of("item", NtmSpace.MOD_ID, "corrosion_protection").values("hbm:insert_yharonite").values("ntmspace:insert_cmb").register();
    }
}
