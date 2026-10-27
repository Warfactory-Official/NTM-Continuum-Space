pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the right Fabric Loom variant per node, as in NTM-Continuum (obfuscated below 26).
    id("dev.kikugie.loom-back-compat") version "0.4.2"
}

stonecutter {
    create(rootProject) {
        // The same six nodes as HBM: an addon runs on exactly the HBM node it was built against.
        fun match(version: String, vararg loaders: String) = loaders.forEach {
            version("$version-$it", version).buildscript = "build.$it.gradle.kts"
        }

        match("26.1", "fabric", "neoforge")
        match("1.21.1", "fabric", "neoforge")
        match("1.20.1", "fabric", "forge")

        vcsVersion = "26.1-neoforge"
    }
}

rootProject.name = "ntmspace"

// NTM: Space is an HBM addon built on Golem Core. Both are composite builds during development, so changes to either
// are picked up with no publish cycle (Maven publishing of both comes later). Each node's artifact is substituted by
// the project of the same node, spelled out because the artifact names (<id>-<loader>-<mc>) differ from the project
// names (<mc>-<loader>).
val nodes = listOf("26.1-fabric", "26.1-neoforge", "1.21.1-fabric", "1.21.1-neoforge", "1.20.1-fabric", "1.20.1-forge")

includeBuild("../Golem-Core") {
    dependencySubstitution {
        nodes.forEach { node ->
            val (mc, loader) = node.split('-', limit = 2)
            substitute(module("dev.golem:golemcore-$loader-$mc")).using(project(":$node"))
        }
    }
}

includeBuild(providers.gradleProperty("hbmProjectDir").orElse("../NTM-Continuum").get()) {
    dependencySubstitution {
        nodes.forEach { node ->
            val (mc, loader) = node.split('-', limit = 2)
            substitute(module("com.hbm:hbm-$loader-$mc")).using(project(":$node"))
        }
    }
}
