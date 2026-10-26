package com.ntmspace.fork.neutron;

import com.hbm.handler.HazmatRegistry;
import net.minecraft.server.level.ServerLevel;
import com.hbm.blocks.machine.DemonLamp;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.hazard.type.IHazardType;
import com.hbm.tileentity.machine.BlockEntityDecon;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityStorageDrum;
import com.hbm.util.ContaminationUtil;
import com.hbm.items.ModDataComponents;
import com.hbm.handler.ArmorUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.config.SpaceConfig;
import dev.golem.core.Ids;
import dev.golem.core.data.GolemComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Space's persistent neutron activation, behind {@link SpaceConfig#disableNeutron}
 * ({@code HAZ_01_disableNeutrons}, default true: off). Ported from {@code hazard/type/HazardTypeNeutron},
 * {@code hazard/transformer/HazardTransformerRadiationNBT}, {@code hazard/type/HazardTypeRadiation} (the activation
 * term), {@code extprop/HbmLivingProps} ({@code activation}), {@code util/ContaminationUtil} (the NEUTRON branch and
 * the geiger's "playerAct" line), {@code handler/EntityEffectHandler.handleRadiationFX} (decay),
 * {@code TileEntityReactorResearch}, {@code TileEntityDemonLamp}, {@code TileEntityDecon},
 * {@code TileEntityStorageDrum} and the {@code NEUTRON} rows of {@code HazardRegistry} / {@code OreDictManager}.
 *
 * <p>The sources of neutrons are what hbm-space has, and nothing else: a carried item with a NEUTRON hazard (the
 * demon core, the Be neutron-source billets, gold-198 and lead-209 billets, the plutonium and source pile rods, and
 * the reactor-grade plutonium, Pu-240, curium, Cf-252 and Es-253 shapes), a running research reactor whose sides are
 * not all shielded, and the demon lamp. Each irradiates the <em>living</em> entity (its {@code activation}, which in
 * turn adds radiation and decays with a 20 minute half life) and, for a player, <em>every stack in the main inventory
 * and armour slots, including the offhand</em> (a stack's activation, which makes it radioactive). Items lying on the ground, blocks and
 * containers are never activated; RTGs and the other reactors emit no neutrons in hbm-space.
 *
 * <p>Activation uses HBM's {@code hbm:neutron_activation} item component and the neutron field of its
 * {@code hbm:living_props} entity attachment. Gamma component radiation and activation combine before the
 * positivity check and reacher curve; activation contributes once per item.
 *
 * <p>What stays on with the switch off is what hbm-space kept on: an activated stack still radiates (nothing activates
 * one while the switch is off), the decontaminator and the storage drum still decay stack activation, and the geiger
 * counter always prints the "Player radioactivity" line (0.0 RAD/s while off).
 *
 * <p>HBM hooks used: {@code HazardTransformerRadiationNBT.COMPONENT_RADIATION},
 * {@code TileEntityReactorResearch.RADIATING}, {@code DemonLamp.TileEntityDemonLamp.IRRADIATED},
 * {@code BlockEntityDecon.DECONTAMINATED}, {@code TileEntityStorageDrum.SLOT_TICK}, {@code ContaminationUtil.GEIGER_LINES};
 * and the existing {@code Environment.LIVING_TICK}, hazard type registry and {@code HazardSystem.trafos}.
 * Not ported: {@code HazardTransformerRadiationME} (AE2 drives; HBM has no ME scrape on any node).
 */
public final class NeutronActivation {

    private NeutronActivation() {}

    /** 1.7 {@code HazardTypeNeutron.NEUTRON_KEY} ({@code "ntmNeutron"}): a stack's activation, RAD/s per item. */
    public static final GolemComponent<Float> STACK = ModDataComponents.NEUTRON_ACTIVATION;

    /** 1.7 {@code HazardRegistry.NEUTRON}. */
    public static final IHazardType NEUTRON = HazardRegistry.registerType(Ids.of(NtmSpace.MOD_ID, "neutron"), new HazardTypeNeutron());

    /** 1.7 decay factors: per tick on a living entity, per tick in a decontaminator, per tick in a storage drum. */
    public static final float LIVING_DECAY = 0.998816F, DECON_DECAY = 0.899916F, DRUM_DECAY = 0.9899916F;

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        SpaceConfig.load();

        HazardSystem.trafos.add(new Transformer());
        HazardTransformerRadiationNBT.COMPONENT_RADIATION.register(NeutronActivation::stackActivation);
        ContaminationUtil.registerNeutronSystem(new ContaminationUtil.NeutronSystem() {
            public boolean enabled() { return NeutronActivation.enabled(); }
            public float stackRads(ItemStack stack) { return activation(stack) * stack.getCount(); }
            public boolean updateStack(ItemStack stack, float rad, float decay) {
                if (stack.isEmpty()) return false;
                Float before = STACK.get(stack).orElse(null);
                float next = Math.max(0F, (before == null ? 0F : before) * decay + rad / stack.getCount());
                if (next < 1e-5F) {
                    STACK.remove(stack);
                    return before != null;
                }
                STACK.set(stack, next);
                return before == null || next != before;
            }
            public boolean contaminate(LivingEntity entity, ContaminationUtil.ContaminationType type, double amount) {
                return NeutronActivation.contaminate(entity, type, amount);
            }
        });
        com.hbm.api.environment.Environment.LIVING_TICK.register(NeutronActivation::livingTick);
        TileEntityReactorResearch.RADIATING.register(NeutronActivation::researchReactor);
        DemonLamp.TileEntityDemonLamp.IRRADIATED.register(NeutronActivation::demonLamp);
        BlockEntityDecon.DECONTAMINATED.register((level, pos, entities) -> decon(entities));
        TileEntityStorageDrum.SLOT_TICK.register((drum, slot, stack) -> {
            float before = activation(stack);
            decay(stack, DRUM_DECAY);
            if (before != activation(stack)) drum.setChanged();
        });
        ContaminationUtil.GEIGER_LINES.register(NeutronActivation::geigerLine);
        NeutronCommands.init();
    }

    public static boolean enabled() {
        return !SpaceConfig.disableNeutron;
    }

    // ------------------------------------------------------------------------------------------------ stacks

    /** 1.7 {@code stack.stackTagCompound.getFloat(NEUTRON_KEY)}. */
    public static float stackActivation(ItemStack stack) {
        if (stack.isEmpty()) return 0F;
        return STACK.getOr(stack, 0F);
    }

    public static float activation(ItemStack stack) { return stackActivation(stack); }

    /** 1.7 {@code HazardTypeNeutron.apply}: a tenth of the dose, per item, on top of what the stack had. */
    public static void apply(ItemStack stack, float rad) {
        if (stack.isEmpty()) return;
        float activation = STACK.getOr(stack, 0F);
        STACK.set(stack, activation + (rad / stack.getCount()) / 10);
    }

    /**
     * Multiplies the stored pool, clearing a newly sub-floor result during this cleanup pass.
     */
    public static void decay(ItemStack stack, float factor) {
        if (stack.isEmpty() || !STACK.has(stack)) return;
        float activation = STACK.getOr(stack, 0F) * factor;
        if (activation < 1e-5F) STACK.remove(stack);
        else STACK.set(stack, activation);
    }

    /** Main inventory and equipment, with the modern offhand included. */
    static void forInventory(Player player, java.util.function.Consumer<ItemStack> action) {
        for (int i = 0; i < 36; i++) action.accept(player.getInventory().getItem(i));
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            action.accept(player.getItemBySlot(slot));
        }
        action.accept(player.getOffhandItem());
    }

    public static void applyInventory(Player player, float rad) { forInventory(player, stack -> apply(stack, rad)); }
    public static void decayInventory(Player player, float factor) { forInventory(player, stack -> decay(stack, factor)); }

    // ------------------------------------------------------------------------------------------------ living

    /** 1.7 {@code HbmLivingProps.getNeutronActivation}: 0 while the switch is off. */
    public static float getActivation(LivingEntity entity) {
        if (!enabled()) return 0F;
        return (float) HbmLivingProps.getNeutron(entity);
    }

    /** 1.7 {@code HbmLivingProps.setNeutronActivation}: ignored while the switch is off. */
    public static void setActivation(LivingEntity entity, float rad) {
        if (enabled()) HbmLivingProps.setNeutron(entity, rad);
    }

    /** 1.7 {@code HbmLivingProps.incrementNeutronActivation}. */
    public static void incrementActivation(LivingEntity entity, float rad) {
        if (!enabled()) return;
        float neutrons = getActivation(entity) + rad;
        if (neutrons < 0) neutrons = 0;
        setActivation(entity, neutrons);
    }

    /**
     * 1.7 {@code ContaminationUtil.contaminate(entity, NEUTRON, CREATIVE, amount)}, the only contamination type hbm-space
     * uses for neutrons: ten times the amount goes into the geiger's environment buffer, then the creative / first-200-
     * ticks player gates, then the activation, scaled by the entity's radiation resistance. Unlike RADIATION, 1.7 did not
     * check radiation immunity here.
     */
    public static boolean contaminate(LivingEntity entity, float amount) {
        return contaminate(entity, ContaminationUtil.ContaminationType.CREATIVE, amount);
    }

    public static boolean contaminate(LivingEntity entity, ContaminationUtil.ContaminationType type, double amount) {
        if (entity.level().isClientSide() || !enabled()) return false;
        HbmLivingProps.setRadEnv(entity, HbmLivingProps.getRadEnv(entity) + amount * 10);
        if (entity instanceof Player player) {
            if (player.isSpectator()) return false; // the port's contaminate skips spectators for every hazard
            boolean protectedFrom = switch (type) {
                case FARADAY -> ArmorUtil.checkForFaraday(player);
                case HAZMAT -> ArmorUtil.checkForHazmat(player);
                case HAZMAT2 -> ArmorUtil.checkForHaz2(player);
                case DIGAMMA -> ArmorUtil.checkForDigamma(player) || ArmorUtil.checkForDigamma2(player);
                case DIGAMMA2 -> ArmorUtil.checkForDigamma2(player);
                default -> false;
            };
            if (protectedFrom || player.isCreative() && type != ContaminationUtil.ContaminationType.NONE
                    && type != ContaminationUtil.ContaminationType.DIGAMMA2) return false;
            if (player.tickCount < 200) return false;
        }
        // Gamma immunity suppresses emission, but never prevents neutron accumulation.
        HbmLivingProps.setNeutron(entity, Math.max(0F, (float) HbmLivingProps.getNeutron(entity)
                + (float) (amount * (type == ContaminationUtil.ContaminationType.RAD_BYPASS ? 1D
                : Math.pow(10D, -HazmatRegistry.getResistance(entity))))));
        return true;
    }

    /** 1.7 {@code EntityEffectHandler.handleRadiationFX}, the activation part: every tick, server side. */
    public static void livingTick(LivingEntity entity) {
        if (!enabled() || entity.level().isClientSide()) return;
        if (!HbmLivingProps.DATA.has(entity)) return;
        if (ContaminationUtil.isRadImmune(entity)) return;
        float neut = getActivation(entity);
        if (neut > 0) {
            ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, neut / 20F);
            setActivation(entity, neut * LIVING_DECAY); // 20 minute half life
        }
        if (neut < 1e-5) setActivation(entity, 0);
    }

    // ------------------------------------------------------------------------------------------------ sources

    /**
     * 1.7 {@code TileEntityReactorResearch.updateEntity}: while the rods are in, the core is hot and one side is open,
     * every living thing within 25 blocks takes {@code rad / resistance / distance^2} neutrons, resistance being the sum
     * of the explosion resistance of the blocks on the line to its eyes (at least 1).
     */
    private static void researchReactor(Level level, BlockPos pos, TileEntityReactorResearch reactor, float rad) {
        if (!enabled()) return;
        double range = 25D;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(range));
        for (LivingEntity e : entities) irradiate(level, pos, e, rad);
    }

    /** One entity of {@link #researchReactor}; public for the GameTests, whose mock players are not in the level. */
    public static void irradiate(Level level, BlockPos pos, LivingEntity e, float rad) {
        if (!enabled()) return; // 1.7 skipped the whole entity loop
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
        double len = vec.length();
        vec = vec.normalize();
        float res = 0;
        for (int i = 1; i < len; i++) {
            int ix = (int) Math.floor(x + 0.5 + vec.x * i);
            int iy = (int) Math.floor(y + 0.5 + vec.y * i);
            int iz = (int) Math.floor(z + 0.5 + vec.z * i);
            res += level.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance();
        }
        if (res < 1) res = 1;
        float eRads = rad;
        eRads /= res;
        if (len != 0D) eRads /= (float) (len * len);
        contaminate(e, eRads);
        if (e instanceof Player player) {
            final float dose = eRads;
            forInventory(player, stack -> apply(stack, dose));
        }
    }

    public static void irradiate(LivingEntity entity, float dose) {
        if (!enabled()) return;
        contaminate(entity, dose);
        if (entity instanceof Player player) applyInventory(player, dose);
    }

    public static void radiate(ServerLevel level, BlockPos source, float rad) {
        researchReactor(level, source, null, rad);
    }

    public static void tick(LivingEntity entity) { livingTick(entity); }

    /** 1.7 {@code TileEntityDemonLamp.radiate}: the lamp's dose ({@code eRads}) as neutrons too. */
    private static void demonLamp(net.minecraft.server.level.ServerLevel level, BlockPos lamp, LivingEntity e, float eRads) {
        contaminate(e, eRads);
        if (e instanceof Player player && enabled()) {
            forInventory(player, stack -> apply(stack, eRads));
        }
    }

    /** 1.7 {@code TileEntityDecon.deconNeutron}, after the radiation clean-up, for everything standing in it. */
    public static void decon(List<LivingEntity> entities) {
        for (LivingEntity e : entities) {
            float neut = getActivation(e);
            if (neut > 0 && enabled()) {
                ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, neut / 20F);
                setActivation(e, neut * DECON_DECAY); // "20 minute half life not really"
            }
            if (e instanceof Player player) forInventory(player, stack -> decay(stack, DECON_DECAY));
        }
    }

    public static void decon(LivingEntity entity) { decon(List.of(entity)); }

    /** 1.7 {@code ContaminationUtil.printGeigerData}: "Player radioactivity:", after the player's contamination. */
    static void geigerLine(Player player, java.util.function.Consumer<Component> out) {
        double neut = ((int) (getActivation(player) * 10)) / 10D;
        double env = ((int) (HbmLivingProps.getRadBuf(player) * 10D)) / 10D;
        // 1.7 coloured the value with the environment's prefix, not the activation's own
        out.accept(Component.translatable("geiger.ntmspace.playerAct")
                .append(Component.literal(" " + ContaminationUtil.getPreffixFromRad(env) + neut + " RAD/s"))
                .withStyle(ChatFormatting.YELLOW));
    }

    // ------------------------------------------------------------------------------------------------ hazards

    /** 1.7 {@code HazardTypeNeutron}. */
    static final class HazardTypeNeutron implements IHazardType {

        @Override
        public void onUpdate(LivingEntity target, double hazardLevel, ItemStack stack) {
            if (!enabled()) return;
            boolean reacher = !GeneralConfig.enable528 && HazardHelper.hasReacher(target);

            float level = (float) hazardLevel * stack.getCount();
            if (level > 0) {
                float rad = (level / 20F) * (float) Math.pow(10D, -HazmatRegistry.getResistance(target));
                if (GeneralConfig.enable528 && reacher) {
                    rad = rad / 49F; // More realistic function for 528: x / distance^2
                } else if (reacher) {
                    rad = (float) Math.sqrt(rad + 1F / ((rad + 2F) * (rad + 2F))) - 1F / (rad + 2F);
                }
                // HBM applies carried hazards every hazardRate ticks; 1.7 ran this every tick
                rad *= hazardRate;
                if (target instanceof Player player) {
                    final float dose = rad;
                    forInventory(player, s -> apply(s, dose));
                }
                // odd but 1.7's: the dose is already scaled by the resistance and contaminate(CREATIVE) scales it again
                contaminate(target, rad);
            }
        }

        @Override
        public void updateEntity(ItemEntity item, double level) {}

        @Override
        public void addHazardInformation(Player player, List<Component> list, double hazardLevel, ItemStack stack,
                List<IHazardModifier> modifiers) {
            float level = (float) IHazardModifier.evalAllModifiers(stack, player, hazardLevel, modifiers);
            if (level < 1e-5 || !enabled()) return;
            list.add(Component.literal("[").append(Component.translatable("trait.ntmspace.neutron")).append("]")
                    .withStyle(ChatFormatting.BLUE));
            list.add(Component.literal((Math.floor(level * 1000) / 1000) + "RAD/s^2").withStyle(ChatFormatting.LIGHT_PURPLE));
            if (stack.getCount() > 1) {
                list.add(Component.literal("Stack: " + (Math.floor(level * 1000 * stack.getCount()) / 1000) + "RAD/s^2")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** hbm-space {@code HazardRegistry} shape multipliers. */
    static final float NUGGET = 0.1F, BILLET = 0.5F, INGOT = 1.0F, BLOCK = 10.0F;

    /** hbm-space {@code HazardRegistry} item rows with a NEUTRON entry, by item id. */
    static final Map<String, Float> ITEMS = new LinkedHashMap<>();
    /**
     * hbm-space {@code OreDictManager} frames with {@code .neutron(...)}: one group per material, the shape tags in
     * {@code c:<shape>/<material>} spelling, each with its multiplied level. Within a group only the first matching tag
     * counts (the 1.7 frame's mutex bit).
     */
    static final List<Map<String, Float>> FRAMES = new ArrayList<>();

    static {
        float au198 = 500.0F, pb209 = 10000.0F;
        float pobe = 75.0F * 3.0F, rabe = 7.5F * 3.0F, pube = 10.0F * 3.0F; // po210/ra226/pu238 * radsource_mult
        ITEMS.put("hbm:demon_core_closed", 10_000F);
        ITEMS.put("hbm:lamp_demon", 10_000F);
        ITEMS.put("hbm:billet_balefire_gold", au198 * BILLET * 0.02f);
        ITEMS.put("hbm:billet_flashlead", pb209 * BILLET * 0.002f);
        ITEMS.put("hbm:billet_po210be", pobe / 10 * BILLET);
        ITEMS.put("hbm:billet_ra226be", rabe / 10 * BILLET);
        ITEMS.put("hbm:billet_pu238be", pube / 10 * BILLET);
        ITEMS.put("hbm:pile_rod_plutonium", rabe * BILLET * 0.2f);
        ITEMS.put("hbm:pile_rod_source", rabe * BILLET * 0.3f);

        frame("pu_mix", "plutonium_rg", 6.25F / 60f, true);       // PURG
        frame("pu240", "plutonium_240", 7.5F / 120f, true);        // PU240
        frame("cm243", "curium_243", 5.6F / 80f, false);
        frame("cm244", "curium_244", 2.0F / 50f, false);
        frame("cm245", "curium_245", 0.8F / 100f, false);
        frame("cm246", "curium_246", 2.5F / 50f, false);
        frame("cm_mix", "curium_rg", 6.0F / 60f, false);           // CMRG
        frame("cm_fuel", "curium_fuel", 2.2F / 120f, false);      // CMF
        frame("cf252", "californium_252", 15.3F / 5f, false);
        frame("es253", "einsteinium_253", 18.3F / 40, false);
    }

    private static void frame(String material, String sourceMaterial, float neutron, boolean block) {
        Map<String, Float> shapes = new LinkedHashMap<>();
        neutronShape(shapes, material, sourceMaterial, "nuggets", "c", neutron * NUGGET);
        neutronShape(shapes, material, sourceMaterial, "billets", "hbm", neutron * BILLET);
        neutronShape(shapes, material, sourceMaterial, "ingots", "c", neutron * INGOT);
        if (block) neutronShape(shapes, material, sourceMaterial, "storage_blocks", "c", neutron * BLOCK);
        FRAMES.add(shapes);
    }

    /** Both spellings share a frame; its first-match rule prevents aliases from adding a second dose. */
    private static void neutronShape(Map<String, Float> shapes, String material, String sourceMaterial,
                                     String shape, String sourceNamespace, float rate) {
        shapes.put("c:" + shape + "/" + material, rate);
        shapes.put(sourceNamespace + ":" + shape + "/" + sourceMaterial, rate);
    }

    private static final Map<String, TagKey<Item>> TAGS = new java.util.concurrent.ConcurrentHashMap<>();

    private static TagKey<Item> tag(String id) {
        return TAGS.computeIfAbsent(id, key -> TagKey.create(Registries.ITEM, Ids.parse(key)));
    }

    /** The NEUTRON level hbm-space registered for this stack's item, or 0. */
    public static float registeredNeutron(ItemStack stack) {
        float level = 0F;
        for (Map<String, Float> frame : FRAMES) {
            for (Map.Entry<String, Float> shape : frame.entrySet()) {
                if (stack.is(tag(shape.getKey()))) {
                    level += shape.getValue();
                    break;
                }
            }
        }
        Float own = ITEMS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (own != null) level += own;
        return level;
    }

    /** Producer rows and activation-only gamma; explicit gamma components combine through the NBT transformer. */
    static final class Transformer implements IHazardTransformer {
        @Override
        public boolean readsLiveStorage() { return true; }

        @Override
        public boolean readsLiveStorage(ItemStack stack) { return STACK.has(stack); }

        @Override
        public void transformPost(ItemStack stack, List<HazardEntry> entries) {
            float neutron = registeredNeutron(stack);
            if (neutron > 0) entries.add(new HazardEntry(NEUTRON, neutron));
            float activation = stackActivation(stack);
            if (activation > 0 && !ModDataComponents.HAZARD_RADIATION.has(stack)) {
                entries.add(new HazardEntry(HazardRegistry.RADIATION, activation));
            }
        }
    }
}
