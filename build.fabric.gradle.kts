plugins {
    id("java-library")
    id("dev.kikugie.loom-back-compat")
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-fabric-$mcVersion"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

sourceSets["main"].java {
    exclude("com/ntmspace/platform/neoforge/**", "com/ntmspace/platform/forge/**")
}

// The dev-only probe addon (src/probe): a second mod that hooks the same places NTM: Space does, so two addons are
// tested together. Compiled into this node's dev runs only; never part of the published jar (see tasks.jar).
val probe = sourceSets.create("probe") {
    // src/probe/java and src/probe/resources: Stonecutter maps every source set onto the shared src tree itself.
    java.exclude("com/ntmspace/probe/platform/neoforge/**", "com/ntmspace/probe/platform/forge/**")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

// Below 26.1 Fabric remaps mod dependencies, and needs an explicit Mojmap layer (see new-hbm's Fabric buildscript).
val obfuscated = mcVersion.substringBefore('.').toInt() < 26
val modernRendering = mcVersion.substringBefore('.').toInt() >= 26
val packFormatEntry = if (modernRendering)
    "\"min_format\": ${prop("pack_format")}, \"max_format\": ${prop("pack_format")}"
else
    "\"pack_format\": ${prop("pack_format")}"

// LWJGL pinned to what the target Minecraft ships (see new-hbm's Fabric buildscript).
propOrNull("deps.lwjgl")?.let { pinned ->
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.lwjgl") useVersion(pinned)
        }
    }
}

val modCfg = if (obfuscated) "modImplementation" else "implementation"
val devRuntime = if (obfuscated) "modLocalRuntime" else "localRuntime"

// Loom's stale-remap guard for both composite builds this addon compiles against.
extra["mc_version"] = mcVersion
extra["golem.build.composite.roots"] = "../Golem-Core=dev/golem,../new-hbm=com/hbm"
apply(from = rootProject.file("../Golem-Core/gradle/mod/composite-freshness.gradle.kts"))

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("deps.minecraft")}")
    if (obfuscated) "mappings"(loom.officialMojangMappings())
    modCfg("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modCfg("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")

    // Golem Core and HBM, both from their composite builds (settings.gradle.kts).
    modCfg("dev.golem:golemcore-fabric-$mcVersion:${prop("deps.golemcore")}+$mcVersion")
    modCfg("com.hbm:hbm-fabric-$mcVersion:${prop("deps.hbm")}+$mcVersion")

    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        "compileOnly"("mezz.jei:jei-$jei-common-api:$jeiVer")
        "compileOnly"("mezz.jei:jei-$jei-fabric-api:$jeiVer")
        devRuntime("mezz.jei:jei-$jei-fabric:$jeiVer") { exclude(group = "net.fabricmc", module = "fabric-loader") }
    }

    // HBM's classes carry jspecify annotations; annotations only.
    compileOnly("org.jspecify:jspecify:1.0.0")
}

loom {
    // Two mods in the dev runs: the addon and the probe, each its own classpath group.
    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
        register("ntmprobe") { sourceSet(probe) }
    }
    runs {
        named("client") {
            client()
            configName = "NTM: Space Fabric Client ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/client"
            source(probe)
            propOrNull("quickPlayWorld")?.let { programArgs("--quickPlaySingleplayer", it) }
        }
        named("server") {
            server()
            configName = "NTM: Space Fabric Server ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/server"
            source(probe)
        }
        // Headless GameTests: every ntmspace:* and ntmprobe:* test, through Fabric API's runner.
        register("gametest") {
            server()
            configName = "NTM: Space Fabric GameTest ($mcVersion)"
            ideConfigGenerated(false)
            runDir = "runs/gametest"
            source(probe)
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${project.file("runs/gametest/report.xml").absolutePath}")
            vmArg("-Dntmprobe.doubleSubstitution=${providers.gradleProperty("probe.doubleSubstitution").orElse("false").get()}")
            vmArg("-Dgolem.gametest.filter=${providers.gradleProperty("gametest.filter").orElse("(ntmspace|ntmprobe):.*").get()}")
        }
    }
}

tasks.matching { it.name == "runGametest" }.configureEach {
    doFirst { delete(layout.projectDirectory.dir("runs/gametest/world")) }
}

val modProps = mapOf(
    "mod_id" to modId,
    "mod_name" to prop("mod.name"),
    "mod_version" to prop("mod.version"),
    "mod_author" to prop("mod.author"),
    "mod_license" to prop("mod.license"),
    "mod_credits" to prop("mod.credits"),
    "mod_description" to prop("mod.description").replace("\n", "\\n"),
    "minecraft_range" to prop("deps.minecraft_range"),
    "fabric_loader" to prop("deps.fabric_loader"),
    "java_version" to javaVersion.toString(),
    "mixin_compatibility" to if (javaVersion <= 17) "JAVA_17" else "JAVA_21",
    "pack_format" to prop("pack_format"),
    "pack_format_entry" to packFormatEntry,
)

tasks.processResources {
    inputs.properties(modProps)
    filesMatching(listOf("fabric.mod.json", "pack.mcmeta", "*.mixins.json")) { expand(modProps) }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg",
        "data/$modId/neoforge/**", "data/$modId/forge/**", "data/neoforge/**")
}

tasks.named<ProcessResources>("processProbeResources") {
    inputs.properties(modProps)
    filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) { expand(modProps) }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.jar {
    from(files(rootProject.file("LICENSE"), rootProject.file("LICENSE.LESSER"), rootProject.file("CREDITS.txt"))) { rename { "${it}_${prop("mod.id")}" } }
}

tasks.compileJava {
    dependsOn(tasks.named("stonecutterGenerate"))
    options.release = javaVersion
}
tasks.named<JavaCompile>("compileProbeJava") {
    dependsOn(tasks.named("stonecutterGenerate"))
    options.release = javaVersion
}
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

extra["java_version"] = javaVersion.toString()
// Golem Core's shared build layer; this mod's parameters for it (golem.build.*) are in gradle.properties.
apply(from = rootProject.file("../Golem-Core/gradle/mod/run-toolchain.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/harness-spec.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-recipes.gradle.kts"))
extra["loader"] = "fabric"
// Neutral "golem:conditions" in data JSON, rewritten to this loader's spelling; see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/recipe-conditions.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-dimensions.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-assets.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/dejson.gradle.kts"))
// NTM: Space's machine recipe rows, generated from src/recipes on 26.1 NeoForge; see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/generated-data.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/mixin-validation.gradle.kts"))

// Credits and links in the loader metadata, from the mod.* properties.
// The uploaded jar: no GameTests (see the script).
apply(from = rootProject.file("../Golem-Core/gradle/mod/release-jar.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/mod-metadata.gradle.kts"))
// Modrinth + CurseForge upload of this node's jar; run every node with the root `publishAllMods`.
apply(from = rootProject.file("../Golem-Core/gradle/mod/publish.gradle.kts"))

// KubeJS (the optional `kubejs` peer, see deps.kubejs): served entirely by Golem Core's dev.golem.core.kubejs adapter,
// so this addon has no KubeJS code and inherits HBM's and its own recipe tables, bindings and events. Never compiled
// against, shipped or required. `-Pkubejs.run=true` puts KubeJS (and its Rhino and Architectury) in the dev runs and copies
// harness/kubejs into each run's kubejs folder; off by default so the baselines stay KubeJS-free.
val kubejsRun = propOrNull("kubejs.run") == "true" && propOrNull("deps.kubejs") != null
if (kubejsRun) {
    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.latvian.dev/releases") { name = "LatvianDev" } }
            filter { includeGroup("dev.latvian.mods") }
        }
        exclusiveContent {
            forRepository { maven("https://maven.shedaniel.me") { name = "Shedaniel" } }
            filter { includeGroup("dev.architectury") }
        }
    }
}
if (kubejsRun) dependencies {
    for (mod in listOfNotNull(
        "dev.latvian.mods:kubejs-fabric:${prop("deps.kubejs")}",
        "dev.latvian.mods:rhino-fabric:${prop("deps.kubejs_rhino")}",
        propOrNull("deps.kubejs_architectury")?.let { "dev.architectury:architectury-fabric:$it" },
    )) devRuntime(mod) { isTransitive = false }
}
for ((run, tasksOfRun) in listOf("client" to listOf("runClient"), "harness" to listOf("runClient"), "server" to listOf("runServer"), "gametest" to listOf("runGametest"))) {
    if (!kubejsRun) break
    val copy = tasks.register<Copy>("copyKubeJsScripts${run.replaceFirstChar { it.uppercase() }}") {
        description = "Copies harness/kubejs into the $run run's kubejs folder (-Pkubejs.run)."
        from(rootProject.file("harness/kubejs")) { exclude("README.md") }
        into(layout.projectDirectory.dir("runs/$run/kubejs"))
    }
    tasks.matching { it.name in tasksOfRun }.configureEach { dependsOn(copy) }
}
