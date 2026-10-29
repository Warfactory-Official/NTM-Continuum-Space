package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemFluidIcon;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.FloraBlocks;
import com.ntmspace.registry.PlanetTerrainBlocks;
import com.ntmspace.registry.SpaceItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * hbm-space {@code ChemicalPlantRecipes.registerDefaults}: the rows hbm-space changes in HBM's table ({@code chem.biogas},
 * {@code chem.rubber}: they replace HBM's rows, removed by {@code recipe_removals/chemical_plant.json}, and take HBM's
 * {@code order}), {@code chem.vinylrubber} (right after {@code chem.rubber}, same order; ids sort it after) and the
 * {@code /// NTM SPACE ///} block, appended after the loaded HBM rows, one order each. The changed and
 * following rows look HBM's order up from its loaded table. 1.7's
 * {@code GeneralConfig.enable528PressurizedRecipes ? p : 0} and {@code GeneralConfig.enable528 ? p : 0} pressures are one
 * row per branch: the default (pressure 0) keeps its id with {@code requires: ["!<flag>"]}, the other is
 * {@code <id>.<flag>} with {@code requires: ["<flag>"]}, both at the same {@code order}. 1.7's
 * {@code setIcon(gas_full / canister_full / fluid_icon, fluid)} icons are set.
 * Replaces the former {@code gen_recipe_changes_chem.py}'s chemical plant rows and the authored {@code chem.hcl}.
 */
public final class ChemicalPlantTable extends GenericTableSource<GenericRecipe> {

    // hbm-space OreDictManager names HBM's OreDictManager has no alias for: their c: tags
    private static final NTMMaterial ZI = new NTMMaterial(-1, "zinc");
    private static final TagKey<Item> KEY_BUTTER = tag("c", "foods/butter");
    private static final TagKey<Item> KEY_STRAWBERRY = tag("c", "crops/strawberry");

    /**
     * 1.7 {@code GeneralConfig.enable528PressurizedRecipes} and {@code GeneralConfig.enable528}.
     */
    private static final String P528 = "enable_528_pressurized";
    private static final String E528 = "enable_528";

    public ChemicalPlantTable() {
        super(ChemicalPlantRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // commented out in hbm-space: chem.oxygen, chem.xenon, chem.xenonoxy (air separation) and chem.birkeland
        remove("hbm:chemical_plant/oxygen");
        remove("hbm:chemical_plant/xenon");
        remove("hbm:chemical_plant/xenonoxy");
        remove("hbm:chemical_plant/birkeland");

        // hbm-space dropped the 4,000 mB air input
        remove("hbm:chemical_plant/biogas");
        this.register(replacing(new GenericRecipe("chem.biogas").setupNamed(60, 100).setIcon(gasFull(Fluids.BIOGAS))
                                                                .inputItems(
                                                                        new ComparableStack(ModFuelItems.biomass.get(),
                                                                                16))
                                                                .outputFluids(new FluidStack(Fluids.BIOGAS, 2_000))));

        // hbm-space adds the zinc dust
        int rubber = orderOf("hbm:chemical_plant/rubber");
        remove("hbm:chemical_plant/rubber");
        remove("hbm:chemical_plant/rubber.enable_528_pressurized");
        branches(P528, on -> at(rubber, new GenericRecipe("chem.rubber").setup(100, 200)
                                                                        .inputItems(S.dust(), ZI.dust())
                                                                        .inputFluids(
                                                                                new FluidStack(Fluids.UNSATURATEDS, 500,
                                                                                        on ? 2 : 0))
                                                                        .outputItems(new ItemStack(
                                                                                ModItems.ingot_rubber.get(), 2))));

        // right after chem.rubber: its order (ties sort by id)
        branches(E528, on -> at(rubber, new GenericRecipe("chem.vinylrubber").setupNamed(100, 400)
                                                                             .inputFluids(
                                                                                     new FluidStack(SpaceFluids.VINYL,
                                                                                             500),
                                                                                     new FluidStack(Fluids.UNSATURATEDS,
                                                                                             400, on ? 2 : 0))
                                                                             .outputItems(new ItemStack(
                                                                                     ModItems.ingot_rubber.get(), 2))));

        /// NTM SPACE ///
        // Base tables can gain rows; keep the appended Space run contiguous after their actual last order.
        var o = orderAfterBase(recipe -> recipe.order);
        this.register(at(o++, new GenericRecipe("chem.coppersulf").setup(50, 100).setIcon(
                                                                          ItemFluidIcon.make(SpaceFluids.COPPERSULFATE))
                                                                  .inputFluids(
                                                                          new FluidStack(SpaceFluids.AQUEOUS_COPPER,
                                                                                  500))
                                                                  .outputItems(
                                                                          new ItemStack(SpaceItems.POWDER_NICKEL.get(),
                                                                                  2),
                                                                          new ItemStack(ModItems.powder_copper.get(),
                                                                                  2))
                                                                  .outputFluids(
                                                                          new FluidStack(SpaceFluids.COPPERSULFATE,
                                                                                  200))));

        this.register(at(o++, new GenericRecipe("chem.uraniumbromide").setup(200, 1_000).setIcon(
                                                                              ItemFluidIcon.make(SpaceFluids.URANIUM_BROMIDE))
                                                                      .inputItems(U235.billet(), new ComparableStack(
                                                                                      ModItems.powder_bromine.get()),
                                                                              ASBESTOS.ingot())
                                                                      .inputFluids(
                                                                              new FluidStack(Fluids.HYDROGEN, 4_000))
                                                                      .outputFluids(new FluidStack(
                                                                              SpaceFluids.URANIUM_BROMIDE, 4_000))));

        this.register(at(o++, new GenericRecipe("chem.thoriumbromide").setup(200, 1_000).setIcon(
                                                                              ItemFluidIcon.make(SpaceFluids.THORIUM_BROMIDE))
                                                                      .inputItems(TH232.billet(), new ComparableStack(
                                                                                      ModItems.powder_bromine.get()),
                                                                              ASBESTOS.ingot())
                                                                      .inputFluids(
                                                                              new FluidStack(Fluids.HYDROGEN, 4_000))
                                                                      .outputFluids(new FluidStack(
                                                                              SpaceFluids.THORIUM_BROMIDE, 4_000))));

        int hydrazine = o++;
        branches(P528, on -> at(hydrazine,
                new GenericRecipe("chem.hydrazine").setupNamed(250, 1_000).setIcon(canisterFull(SpaceFluids.HYDRAZINE))
                                                   .inputFluids(new FluidStack(Fluids.NITRIC_ACID, 2_000),
                                                           new FluidStack(SpaceFluids.AMMONIA, 1_000, on ? 1 : 0))
                                                   .outputFluids(new FluidStack(SpaceFluids.HYDRAZINE, 800))));

        this.register(at(o++, new GenericRecipe("chem.ammonia").setup(50, 100)
                                                               .inputFluids(new FluidStack(SpaceFluids.NITROGEN, 600),
                                                                       new FluidStack(Fluids.WATER, 1_000))
                                                               .outputFluids(
                                                                       new FluidStack(SpaceFluids.AMMONIA, 800))));

        int bloodfuel = o++;
        branches(P528, on -> at(bloodfuel,
                new GenericRecipe("chem.bloodfuel").setupNamed(250, 1_000).setIcon(canisterFull(SpaceFluids.BLOODGAS))
                                                   .inputFluids(new FluidStack(SpaceFluids.AMMONIA, 350, on ? 1 : 0),
                                                           new FluidStack(Fluids.BLOOD, 800, on ? 2 : 0))
                                                   .outputFluids(new FluidStack(SpaceFluids.BLOODGAS, 1000))));

        this.register(at(o++, new GenericRecipe("chem.hcl").setup(50, 100)
                                                           .inputFluids(new FluidStack(Fluids.HYDROGEN, 500),
                                                                   new FluidStack(Fluids.CHLORINE, 500))
                                                           .outputFluids(new FluidStack(SpaceFluids.HCL, 1000))));

        int ammoniumnitrate = o++;
        branches(P528, on -> at(ammoniumnitrate, new GenericRecipe("chem.ammoniumnitrate").setup(250, 1_000)
                                                                                          .inputFluids(new FluidStack(
                                                                                                          SpaceFluids.AMMONIA,
                                                                                                          500, on ? 1 : 0),
                                                                                                  new FluidStack(
                                                                                                          SpaceFluids.NITROGEN,
                                                                                                          1000))
                                                                                          .outputItems(new ItemStack(
                                                                                                  SpaceItems.AMMONIUM_NITRATE.get(),
                                                                                                  4))));

        this.register(at(o++, new GenericRecipe("chem.nmass").setup(250, 10_000)
                                                             .inputFluids(new FluidStack(Fluids.SCHRABIDIC, 650),
                                                                     (new FluidStack(Fluids.IONGEL, 800)))
                                                             .inputItems(
                                                                     new ComparableStack(ModItems.pellet_charged.get(),
                                                                             1),
                                                                     new ComparableStack(ModItems.ingot_euphemium.get(),
                                                                             1))
                                                             .outputFluids(new FluidStack(SpaceFluids.NMASS, 1000),
                                                                     new FluidStack(Fluids.WASTEGAS, 2000))));

        // port: Quarter the four-cake batch to fit one-item output slots at the same per-cake cost.
        this.register(at(o++, new GenericRecipe("chem.masscake").setup(50, 100)
                                                                .inputFluids(new FluidStack(SpaceFluids.CMILK, 1000),
                                                                        new FluidStack(SpaceFluids.CREAM, 250))
                                                                .inputItems(
                                                                        new ComparableStack(Items.SUGAR, 2),
                                                                        new ComparableStack(Items.EGG, 1))
                                                                .outputItems(new ItemStack(Items.CAKE, 1))));

        this.register(at(o++, new GenericRecipe("chem.butter").setup(100, 100)
                                                              .inputFluids(new FluidStack(SpaceFluids.EMILK, 1000))
                                                              .outputItems(new ItemStack(SpaceItems.BUTTER.get()))));

        this.register(at(o++, new GenericRecipe("chem.strawberryicecream").setup(150, 100)
                                                                          .inputFluids(new FluidStack(SpaceFluids.CREAM,
                                                                                  1000))
                                                                          .inputItems(new TagStack(KEY_BUTTER, 2),
                                                                                  new ComparableStack(
                                                                                          Blocks.PACKED_ICE.asItem(),
                                                                                          1),
                                                                                  new TagStack(KEY_STRAWBERRY, 4))
                                                                          .outputItems(new ItemStack(
                                                                                  SpaceItems.S_CREAM.get(), 4))));

        this.register(at(o++, new GenericRecipe("chem.minmusicecream").setup(150, 100)
                                                                      .inputFluids(
                                                                              new FluidStack(SpaceFluids.CREAM, 1000))
                                                                      .inputItems(new TagStack(KEY_BUTTER, 4),
                                                                              new ComparableStack(
                                                                                      PlanetTerrainBlocks.MINMUS_SMOOTH.get()
                                                                                                                       .asItem(),
                                                                                      2), new ComparableStack(
                                                                                      PlanetTerrainBlocks.MINMUS_STONE.get()
                                                                                                                      .asItem(),
                                                                                      2))
                                                                      .outputItems(
                                                                              new ItemStack(SpaceItems.MIN_CREAM.get(),
                                                                                      4))));

        this.register(at(o++, new GenericRecipe("chem.minmussmoothstone").setup(200, 1_000)
                                                                         .inputFluids(
                                                                                 new FluidStack(SpaceFluids.MILK, 350),
                                                                                 new FluidStack(SpaceFluids.EMILK, 250))
                                                                         .inputItems(new ComparableStack(
                                                                                         Blocks.ICE.asItem(), 4),
                                                                                 new ComparableStack(
                                                                                         FloraBlocks.MINT_LEAVES.get(),
                                                                                         2), new ComparableStack(
                                                                                         PlanetTerrainBlocks.MINMUS_SMOOTH.get()
                                                                                                                          .asItem(),
                                                                                         2))
                                                                         .outputItems(new ItemStack(
                                                                                 PlanetTerrainBlocks.MINMUS_SMOOTH.get(),
                                                                                 4))));

        this.register(at(o++, new GenericRecipe("chem.menthol").setup(50, 100)
                                                               .inputFluids(new FluidStack(Fluids.STEAM, 350))
                                                               .inputItems(new ComparableStack(
                                                                       FloraBlocks.MINT_LEAVES.get(), 4))
                                                               .outputItems(
                                                                       new ItemStack(SpaceItems.INGOT_MENTHOL.get(),
                                                                               2))));

        this.register(at(o++, new GenericRecipe("chem.mentholfromminmus").setupNamed(50, 300)
                                                                         .inputFluids(new FluidStack(Fluids.STEAM, 350),
                                                                                 new FluidStack(Fluids.SULFURIC_ACID,
                                                                                         500))
                                                                         .inputItems(new ComparableStack(
                                                                                 PlanetTerrainBlocks.MINMUS_SMOOTH.get()
                                                                                                                  .asItem(),
                                                                                 40))
                                                                         .outputItems(new ItemStack(
                                                                                 SpaceItems.INGOT_MENTHOL.get(), 15))));

        this.register(at(o++, new GenericRecipe("chem.mintslice").setup(50, 100)
                                                                 .inputFluids(new FluidStack(SpaceFluids.MILK, 350))
                                                                 .inputItems(new ComparableStack(
                                                                                 SpaceItems.BILLET_MENTHOL.get(), 3),
                                                                         new ComparableStack(
                                                                                 PlanetTerrainBlocks.MINMUS_STONE.get()
                                                                                                                 .asItem(),
                                                                                 1),
                                                                         new ComparableStack(Items.SUGAR, 6))
                                                                 .outputItems(new ItemStack(
                                                                         SpaceItems.CHOCOLATE_MINT_BILLET.get(), 3))));

        this.register(at(o++, new GenericRecipe("chem.minmusstone").setup(300, 500)
                                                                   .inputFluids(
                                                                           new FluidStack(Fluids.SULFURIC_ACID, 500))
                                                                   .inputItems(new ComparableStack(
                                                                           PlanetTerrainBlocks.MINMUS_SMOOTH.get()
                                                                                                            .asItem(),
                                                                           2))
                                                                   .outputItems(new ItemStack(
                                                                           PlanetTerrainBlocks.MINMUS_STONE.get(),
                                                                           2))));

        this.register(at(o++, new GenericRecipe("chem.soil").setup(100, 1_000)
                                                            .inputFluids(new FluidStack(Fluids.WATER, 4000))
                                                            .inputItems(new ComparableStack(
                                                                            SpaceItems.AMMONIUM_NITRATE.get(), 1),
                                                                    new ComparableStack(Blocks.GRAVEL.asItem(), 8))
                                                            .outputItems(new ItemStack(Blocks.DIRT, 8))));

        this.register(at(o++, new GenericRecipe("chem.chloromethane").setup(50, 1_000)
                                                                     .inputFluids(new FluidStack(Fluids.GAS, 750),
                                                                             new FluidStack(Fluids.CHLORINE, 250))
                                                                     .outputFluids(
                                                                             new FluidStack(SpaceFluids.CHLOROMETHANE,
                                                                                     1000))));

        int nitricacidalt = o++;
        branches(P528, on -> at(nitricacidalt, new GenericRecipe("chem.nitricacidalt").setupNamed(50, 1_000)
                                                                                      .inputFluids(new FluidStack(
                                                                                                      Fluids.WATER, 500),
                                                                                              new FluidStack(
                                                                                                      SpaceFluids.AMMONIA,
                                                                                                      1000, on ? 1 : 0))
                                                                                      .outputFluids(new FluidStack(
                                                                                              Fluids.NITRIC_ACID,
                                                                                              1_000))));

        // WARNING: NILERED CHEMISTRY ZONE //
        this.register(at(o++,
                new GenericRecipe("chem.hydrapiss").setupNamed(250, 1_000).setIcon(canisterFull(SpaceFluids.HYDRAZINE))
                                                   .inputFluids(new FluidStack(Fluids.NITRIC_ACID, 2000))
                                                   .inputItems(new ComparableStack(ModItems.rag_piss.get()))
                                                   .outputFluids(new FluidStack(SpaceFluids.HYDRAZINE, 800))));

        int synleather = o++;
        branches(P528, on -> at(synleather, new GenericRecipe("chem.synleather").setupNamed(200, 500)
                                                                                .inputFluids(
                                                                                        new FluidStack(Fluids.PEROXIDE,
                                                                                                250, on ? 1 : 0))
                                                                                .inputItems(ANY_PLASTIC.ingot(),
                                                                                        new ComparableStack(
                                                                                                Items.STRING, 4))
                                                                                .outputItems(
                                                                                        new ItemStack(Items.LEATHER))));
    }

    /**
     * Rows carry the {@code order} {@link #at} gives them: HBM's position, or the next one after HBM's rows.
     */
    @Override
    protected boolean ordered() {
        return false;
    }

    /**
     * 1.7's {@code GeneralConfig.<flag> ? on : off} inside one registration: the default row ({@code make(false)}) keeps
     * its id and requires {@code !flag}, the other ({@code make(true)}) is {@code <id>.<flag>} and requires {@code flag}.
     */
    private void branches(String flag, BranchRow make) {
        this.register(make.row(false).requires("!" + flag));
        GenericRecipe on = make.row(true).requires(flag);
        this.register(id(on) + "." + flag, on);
    }

    private interface BranchRow {
        GenericRecipe row(boolean on);
    }

    /**
     * 1.7 {@code new ItemStack(ModItems.gas_full, 1, fluid.getID())}: a full gas tank of the fluid.
     */
    private static ItemStack gasFull(FluidType fluid) {
        ItemFluidContainerNT tank = ModFluidNetItems.gas_full.get();
        return tank.make(fluid, tank.capacity);
    }

    /**
     * 1.7 {@code new ItemStack(ModItems.canister_full, 1, fluid.getID())}: a full canister of the fluid.
     */
    private static ItemStack canisterFull(FluidType fluid) {
        ItemFluidContainerNT canister = ModFluidNetItems.canister_full.get();
        return canister.make(fluid, canister.capacity);
    }

    /**
     * A row hbm-space changes: it takes the {@code order} of HBM's row of the same id, the one it replaces.
     */
    private GenericRecipe replacing(GenericRecipe recipe) {
        return at(orderOf("hbm:chemical_plant/" + id(recipe)), recipe);
    }

    private static GenericRecipe at(int order, GenericRecipe recipe) {
        recipe.order = order;
        return recipe;
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }
}
