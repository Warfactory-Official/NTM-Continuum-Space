package com.ntmspace.registry;

import com.hbm.creativetabs.HbmTab;
import com.ntmspace.NtmSpace;
import com.ntmspace.block.dyson.MachineDysonConverterAnatmogenesis;
import com.ntmspace.block.dyson.MachineDysonConverterHE;
import com.ntmspace.block.dyson.MachineDysonConverterTU;
import com.ntmspace.block.dyson.MachineDysonLauncher;
import com.ntmspace.block.dyson.MachineDysonReceiver;
import com.ntmspace.block.machine.StandardInfoBlockItem;
import com.ntmspace.command.DysonCommands;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterHE;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterTU;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.tileentity.dyson.TileEntityDysonReceiver;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * hbm-space's Dyson swarm: the Dyson Spinlauncher ({@code dyson_launcher}), the Dyson Receiver, the three converters
 * (heat {@code dyson_converter_tu}, power {@code dyson_converter_he}, the anatmogenesis terraformer) - all
 * {@code BlockDummyable}s of hardness 10 and resistance 20 on the machine tab - and the swarm member item
 * ({@code swarm_member}, parts tab). The members themselves live in the star's {@code CBT_Dyson} trait; the relay
 * satellite is {@code satellite/SpaceSatellites}'. None of these machines has a GUI in 1.7; they speak through their
 * look overlays. Sounds: hbm-space's {@code misc.spinshot}, {@code misc.spincharge} and {@code block.dysonBeam}
 * (here {@code block.dyson_beam}: an id may not hold capitals).
 */
public final class DysonContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NtmSpace.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NtmSpace.MOD_ID);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NtmSpace.MOD_ID);
    public static final DeferredRegister.Sounds SOUNDS = DeferredRegister.createSounds(NtmSpace.MOD_ID);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES, SOUNDS);

    public static final RegistryObject<MachineDysonLauncher> DYSON_LAUNCHER = REG.<MachineDysonLauncher>block(
                    "dyson_launcher", MachineDysonLauncher::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDysonReceiver> DYSON_RECEIVER = REG.<MachineDysonReceiver>block(
                    "dyson_receiver", MachineDysonReceiver::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDysonConverterTU> DYSON_CONVERTER_TU = REG.<MachineDysonConverterTU>block(
                    "dyson_converter_tu", MachineDysonConverterTU::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDysonConverterHE> DYSON_CONVERTER_HE = REG.<MachineDysonConverterHE>block(
                    "dyson_converter_he", MachineDysonConverterHE::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
            .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);
    public static final RegistryObject<MachineDysonConverterAnatmogenesis> DYSON_CONVERTER_ANATMOGENESIS =
            REG.<MachineDysonConverterAnatmogenesis>block("dyson_converter_anatmogenesis",
                            MachineDysonConverterAnatmogenesis::new, p -> SpaceMachines.machine(p, 10.0F, 20.0F))
                    .transform(SpaceMachines.FOLDED).loot(Loot.self()).tag(BlockTag.PICKAXE);

    /** The launcher and the receiver are 1.7 {@code ITooltipProvider}s ({@code addStandardInfo}). */
    public static final RegistryObject<StandardInfoBlockItem> DYSON_LAUNCHER_ITEM = REG.<MachineDysonLauncher, StandardInfoBlockItem>blockItem(
            DYSON_LAUNCHER, (b, p) -> new StandardInfoBlockItem(b, p, "dyson_launcher"), Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<StandardInfoBlockItem> DYSON_RECEIVER_ITEM = REG.<MachineDysonReceiver, StandardInfoBlockItem>blockItem(
            DYSON_RECEIVER, (b, p) -> new StandardInfoBlockItem(b, p, "dyson_receiver"), Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> DYSON_CONVERTER_TU_ITEM = REG.blockItem(DYSON_CONVERTER_TU).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> DYSON_CONVERTER_HE_ITEM = REG.blockItem(DYSON_CONVERTER_HE).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> DYSON_CONVERTER_ANATMOGENESIS_ITEM =
            REG.blockItem(DYSON_CONVERTER_ANATMOGENESIS).tab(HbmTab.MACHINES);

    /** 1.7 {@code swarm_member}: a plain item (texture name {@code sat_scanner}, drawn as its OBJ), parts tab. */
    public static final RegistryObject<Item> SWARM_MEMBER = REG.simpleItem("swarm_member", p -> p).tab(HbmTab.MATERIALS);

    public static final RegistryObject<BlockEntityType<TileEntityDysonLauncher>> LAUNCHER_BE = REG.blockEntity(
            "dyson_launcher", () -> BlockEntities.ticking(TileEntityDysonLauncher::new,
                    TileEntityDysonLauncher::clientTick, TileEntityDysonLauncher::serverTick, DYSON_LAUNCHER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDysonReceiver>> RECEIVER_BE = REG.blockEntity(
            "dyson_receiver", () -> BlockEntities.ticking(TileEntityDysonReceiver::new,
                    TileEntityDysonReceiver::clientTick, TileEntityDysonReceiver::serverTick, DYSON_RECEIVER.get()));
    /** 1.7's {@code updateEntity} is empty: the heat converter does not tick. */
    public static final RegistryObject<BlockEntityType<TileEntityDysonConverterTU>> CONVERTER_TU_BE = REG.blockEntity(
            "dyson_converter_tu", () -> BlockEntities.type(TileEntityDysonConverterTU::new, DYSON_CONVERTER_TU.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDysonConverterHE>> CONVERTER_HE_BE = REG.blockEntity(
            "dyson_converter_he", () -> BlockEntities.ticking(TileEntityDysonConverterHE::new,
                    null, TileEntityDysonConverterHE::serverTick, DYSON_CONVERTER_HE.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDysonConverterAnatmogenesis>> CONVERTER_ANATMOGENESIS_BE =
            REG.blockEntity("dyson_converter_anatmogenesis", () -> BlockEntities.ticking(TileEntityDysonConverterAnatmogenesis::new,
                    TileEntityDysonConverterAnatmogenesis::clientTick, TileEntityDysonConverterAnatmogenesis::serverTick,
                    DYSON_CONVERTER_ANATMOGENESIS.get()));

    public static final RegistryObject<SoundEvent> SPINSHOT = REG.sound("misc.spinshot");
    public static final RegistryObject<SoundEvent> SPINCHARGE = REG.sound("misc.spincharge");
    public static final RegistryObject<SoundEvent> DYSON_BEAM = REG.sound("block.dyson_beam");

    private DysonContent() {}

    public static void init() {
        DysonCommands.init();
        TileEntityDysonLauncher.registerEffects();
    }
}
