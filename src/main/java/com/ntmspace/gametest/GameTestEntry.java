package com.ntmspace.gametest;

import dev.golem.core.platform.ModEntry;

/** Fabric's {@code golemcore:entry} for the GameTest suite; release jars drop the class and its line in fabric.mod.json. */
public final class GameTestEntry implements ModEntry {

    @Override
    public void declare() {
        SpaceGameTests.initAll();
    }
}
