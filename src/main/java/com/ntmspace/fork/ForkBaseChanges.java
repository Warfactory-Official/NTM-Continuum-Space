package com.ntmspace.fork;

import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.items.armor.ArmorDNT;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ModMoldItems;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.config.SpaceConfig;
import com.ntmspace.inventory.material.SpaceAlloyMaterials;
import com.ntmspace.registry.SpaceItems;
import net.minecraft.world.item.ItemStack;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.machine.MachineElectrolyser;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.creativetabs.HbmTab;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.registration.Presets;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.tileentity.network.TileEntityCranePartitioner;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.util.ContaminationUtil;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.dejson.Variants;
import dev.golem.core.dejson.Models;
import dev.golem.core.hook.Verdict;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The rest of hbm-space's small changes to base-HBM classes (fork-content triage item 7, part 3), each through an HBM
 * tunable or hook so it holds only while NTM: Space is installed:
 * <ul>
 *   <li>{@code BlockDummyable}: multiblocks placed against walls and ceilings, and a player standing inside a machine
 *       they just placed walks out of it ({@code BlockMultiblockCore.wallPlacement/walkOutAfterPlacing}, V-175);</li>
 *   <li>{@code CranePartitioner}: 96 queue and 8 declog slots (V-176);</li>
 *   <li>{@code PneumoTube}: a compressor takes any non-corrosive, non-antimatter gas a fluid identifier picks (V-177);</li>
 *   <li>{@code MachineElectrolyser}: two more proxies and four more connection points on the long sides (V-178);</li>
 *   <li>{@code MachineCapacitor}: the creative {@code capacitor_complex} ("Wavefunction Capacitor (LEGACY)", 1e17 HE,
 *       cadmium-plated), still a working store with its capacity lines first (V-179);</li>
 *   <li>{@code IToolAreaAbility}: with {@code recursiveStone} off the vein miner also stops at anything in the "stone"
 *       or "cobblestone" ore dictionary entries (the {@code c:stones} / {@code c:cobblestones} item tags, V-180);</li>
 *   <li>{@code EntityTNTPrimedBase}: primed charges tumble (V-181);</li>
 *   <li>{@code EntityGhost}: ghosts watch the nearest player (V-182);</li>
 *   <li>{@code TileEntityGeiger}: clicks below 1 RAD/s only with the dose's probability (V-183);</li>
 *   <li>{@code FluidLoaderFillableItem}: an armor piece in a tank's unload slot fills its modules (V-185);</li>
 *   <li>{@code ContaminationUtil.isRadImmune}: the ocelot (and so the cat, 1.7's tamed ocelot) is no longer immune
 *       (H-76; hbm-space's nuclear creeper and Quackos keep it through {@code IRadiationImmune});</li>
 *   <li>{@code BlockBobble}: hbm-space's credit and scrap edits to HBM's PEEP, MICROWAVE and MELLOW (E-096).</li>
 * </ul>
 * The client half (wings, refueler overlay, tube tooltip) is {@code client/ForkBaseClient}.
 */
public final class ForkBaseChanges {

    private static final String NS = NtmSpace.MOD_ID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NS);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    public static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES = DeferredRegister.createBlockEntityTypes(NS);
    private static final Registrar REG = Registrar.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    /**
     * A carried complex capacitor keeps its stored charge and actual capacity together.
     */
    public static final GolemComponent<CapacitorCharge> CAPACITOR_STATE =
            GolemComponent.of(NS, "capacitor_state", CapacitorCharge.CODEC);

    /**
     * hbm-space {@code MachineCapacitor.addInformation}'s lines for {@code capacitor_complex}, after the description.
     */
    public static final List<String> COMPLEX_LORE = List.of(
            "TaCdSa236-7 N-Boosted Anti Mass core surrounded by",
            "Flashlead antimatter lattice in a BF Stabilization Matrix",
            "subjected to the Ferric Osmiridium-Lutece",
            "ψ(x,t)=Aeiℏ(px−Et) Wavefunction.");

    /**
     * hbm-space {@code capacitor_complex}: {@code MachineCapacitor(iron, 100_000_000_000_000_000L, "complex")}, hardness 5,
     * resistance 10, machine tab, {@code @Deprecated} like the other legacy capacitors (no recipe: creative only).
     */
    public static final RegistryObject<ComplexCapacitor> CAPACITOR_COMPLEX = REG.block("capacitor_complex",
                                                                                        p -> new ComplexCapacitor(p, 100_000_000_000_000_000L),
                                                                                        p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
                                                                                .transform(Presets.statesOnly(
                                                                                        Variants.facingUp()))
                                                                                .icon(d -> d.models(Models.blockItem(
                                                                                        NS + ":block/capacitor_complex")))
                                                                                .loot(Loot.self())
                                                                                .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> CAPACITOR_COMPLEX_ITEM = REG.<BlockItem>item("capacitor_complex",
                                                                                      (Item.Properties p) -> new EnergyNetBlockItem(CAPACITOR_COMPLEX.get(), Compat.blockDescription(p),
                                                                                              stack -> CAPACITOR_COMPLEX.get().tooltip(stack, COMPLEX_LORE.stream()
                                                                                                                                                          .<Component>map(Component::literal)
                                                                                                                                                          .toList())))
                                                                              .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockEntityType<ComplexCapacitorEntity>> CAPACITOR_COMPLEX_BE =
            REG.blockEntity("capacitor_complex", () -> BlockEntities.serverTicking(ComplexCapacitorEntity::new,
                    ComplexCapacitorEntity::serverTick, CAPACITOR_COMPLEX.get()));

    private ForkBaseChanges() {
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        EntityChemical.seedSlurrySingleBlock = true;
        ArmorDNT.SILENT_ATTACK.register(source ->
                source.is(Breathing.OXYPRIME));
        BlockMultiblockCore.wallPlacement = true;
        BlockMultiblockCore.walkOutAfterPlacing = true;
        TileEntityCranePartitioner.inputCount = 96;
        TileEntityCranePartitioner.outputCount = 8;
        TileEntityPneumoTube.anyGas = true;
        MachineElectrolyser.sidePorts = true;
        EntityTntNtm.spin = true;
        EntityGhost.watchPlayers = true;
        EntityQuackos.serverSafety = () -> SpaceConfig.serverSafety;
        TileEntityGeiger.probabilisticClicks = true;
        // hbm-space ItemDosimeter: below 1 RAD/s a click only as likely as the dose (V-186)
        com.hbm.items.tool.ItemDosimeter.probabilisticClicks = true;
        // hbm-space ItemGeigerCounter: the same gate on the handheld counter (V-188)
        com.hbm.items.tool.ItemGeigerCounter.probabilisticClicks = true;
        // hbm-space BlockGasBase.randomDisplayTick: the ash glasses show a gas cloud when rand.nextInt(9) == 1 (V-187)
        com.hbm.blocks.generic.BlockGasBase.ashGlassesParticleChance = 9;
        MachineFluidContainerIO.fillArmorMods = true;
        TagKey<Item> stones = TagKey.create(Registries.ITEM, Ids.of("c", "stones"));
        TagKey<Item> cobblestones = TagKey.create(Registries.ITEM, Ids.of("c", "cobblestones"));
        // hbm-space: OreDictionary.getOres(KEY_STONE / KEY_COBBLESTONE) holds the block's item. Widened, not replaced, so
        // another addon's widening (before or after this one) stays (the probe's coexistence test)
        IToolAreaAbility.RECURSION_STONE.updateAndGet(previous -> previous.or(state -> state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || new net.minecraft.world.item.ItemStack(state.getBlock()).is(stones)
                || new net.minecraft.world.item.ItemStack(state.getBlock()).is(cobblestones)));
        // hbm-space ContaminationUtil.isRadImmune: the class list is gone; of its entries only the ocelot lost immunity
        ContaminationUtil.RAD_IMMUNE.register(
                entity -> entity.getType() == EntityType.OCELOT || entity.getType() == EntityType.CAT
                        ? Verdict.DENY : Verdict.PASS);
        bobbleCredits();
    }

    /**
     * Extends the registered native blade mold, retaining its titanium and tungsten casts.
     */
    public static void registered() {
        var blade = (ItemMold.MoldMulti) ModMoldItems.mold_blade.get().mold;
        blade.add(SpaceAlloyMaterials.STAINLESS,
                () -> new ItemStack(SpaceItems.BLADE_SYNGAS.get()));
    }

    /**
     * hbm-space {@code BlockBobble.BobbleType}: the credits and plastic scraps it changed on HBM's own figures.
     */
    static void bobbleCredits() {
        BobbleType.PEEP.contribution = "Coilgun, Leadburster, Congo Lake models, and the 737";
        BobbleType.PEEP.scrap = "CPU_CLOCK";
        BobbleType.MICROWAVE.scrap = "BRIDGE_BIOS";
        BobbleType.MELLOW.contribution = "Celestial mechanics, rocketry,$atmospheric chemistry, orbital stations,$Dyson swarms";
    }
}
