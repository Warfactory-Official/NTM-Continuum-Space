package com.ntmspace.platform.fabric;

/**
 * The {@code fabric-gametest} entrypoint. On 26.1 Golem Core registers every declared suite itself; below it the
 * legacy framework collects test functions from a generator.
 */
public final class FabricGameTests {

    public FabricGameTests() {}

    //? if fabric && <26.1 {
    /*@net.minecraft.gametest.framework.GameTestGenerator
    public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> generate() {
        com.ntmspace.gametest.SpaceGameTests.initAll();
        return dev.golem.core.gametest.GameTests.functionsOf(com.ntmspace.gametest.SpaceGameTests.TESTS);
    }
    *///?}
}
