package com.ntmspace.jei;

//? if jei && forgelike {
import com.ntmspace.NtmSpace;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/** NTM: Space's {@code @JeiPlugin} on NeoForge and Forge; the body is {@link SpaceJeiCommon}. */
@JeiPlugin
public class SpaceJeiPluginForgelike implements IModPlugin {

    private static final Identifier UID = dev.golem.core.Ids.of(NtmSpace.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        SpaceJeiCommon.registerCategories(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        SpaceJeiCommon.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        SpaceJeiCommon.registerCatalysts(registration);
    }
}
//?}
