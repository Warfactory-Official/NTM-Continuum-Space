plugins {
    id("java-library")
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-forge"

java { toolchain.languageVersion = JavaLanguageVersion.of(javaVersion) }

sourceSets["main"].java {
    exclude("com/ntmspace/platform/fabric/**", "com/ntmspace/platform/neoforge/**")
}

// The dev-only probe addon (src/probe); see the Fabric buildscript.
val probe = sourceSets.create("probe") {
    // src/probe/java and src/probe/resources: Stonecutter maps every source set onto the shared src tree itself.
    java.exclude("com/ntmspace/probe/platform/fabric/**", "com/ntmspace/probe/platform/neoforge/**")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

legacyForge {
    version = prop("deps.forge")
    runs {
        configureEach {
            systemProperty("forge.enabledGameTestNamespaces", "$modId,ntmprobe")
            ideName = "NTM: Space Forge ${name.replaceFirstChar { it.uppercase() }} ($mcVersion)"
            sourceSet = probe
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
            propOrNull("quickPlayWorld")?.let { programArguments.addAll("--quickPlaySingleplayer", it) }
        }
        register("server") {
            server()
            gameDirectory = file("runs/server")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = file("runs/gametest")
            jvmArgument("-Dntmprobe.doubleSubstitution=${providers.gradleProperty("probe.doubleSubstitution").orElse("false").get()}")
            jvmArgument("-Dgolem.gametest.filter=${providers.gradleProperty("gametest.filter").orElse("(ntmspace|ntmprobe):.*").get()}")
        }
    }
    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
        register("ntmprobe") { sourceSet(probe) }
    }
}

// Same reasons as new-hbm's Forge buildscript: every GameTest run starts from an empty flat world.
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst {
        delete(layout.projectDirectory.dir("runs/gametest/world"))
        val properties = layout.projectDirectory.file("runs/gametest/server.properties").asFile
        properties.parentFile.mkdirs()
        val lines = if (properties.exists()) properties.readLines().filterNot {
            it.startsWith("level-type=") || it.startsWith("level-seed=") || it.startsWith("max-tick-time=")
        } else emptyList()
        properties.writeText((lines + listOf("level-type=minecraft:flat", "level-seed=0", "max-tick-time=-1"))
            .joinToString(System.lineSeparator()) + System.lineSeparator())
    }
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
}

dependencies {
    modImplementation("dev.golem:golemcore-forge-$mcVersion:${prop("deps.golemcore")}+$mcVersion")
    modImplementation("com.hbm:hbm-forge-$mcVersion:${prop("deps.hbm")}+$mcVersion")

    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        modCompileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
        modCompileOnly("mezz.jei:jei-$jei-forge-api:$jeiVer")
        modRuntimeOnly("mezz.jei:jei-$jei-forge:$jeiVer")
    }
    compileOnly("org.jspecify:jspecify:1.0.0")
}

val modProps = mapOf(
    "mod_id" to modId,
    "mod_name" to prop("mod.name"),
    "mod_version" to prop("mod.version"),
    "mod_author" to prop("mod.author"),
    "mod_license" to prop("mod.license"),
    "mod_credits" to prop("mod.credits"),
    "mod_description" to prop("mod.description"),
    "minecraft_range" to prop("deps.minecraft_range"),
    "forge_loader_range" to prop("deps.forge_loader_range"),
    "java_version" to javaVersion.toString(),
    "pack_format" to prop("pack_format"),
    "pack_format_entry" to "\"pack_format\": ${prop("pack_format")}",
)

tasks.processResources {
    inputs.properties(modProps)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(modProps) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "data/$modId/neoforge/**", "data/neoforge/**")
}
tasks.named<ProcessResources>("processProbeResources") {
    inputs.properties(modProps)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(modProps) }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
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

extra["mc_version"] = mcVersion
extra["java_version"] = javaVersion.toString()
// Forge 1.20.1's convention tag namespace is `forge:`; see new-hbm's Forge buildscript.
extra["convention_namespace"] = "forge"
apply(from = rootProject.file("../Golem-Core/gradle/mod/run-toolchain.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/harness-spec.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-recipes.gradle.kts"))
extra["loader"] = "forge"
// Orders the mod before KubeJS (optional dependency) so its script groups and bindings reach the startup scripts.
apply(from = rootProject.file("../Golem-Core/gradle/mod/kubejs-order.gradle.kts"))
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
        "dev.latvian.mods:kubejs-forge:${prop("deps.kubejs")}",
        "dev.latvian.mods:rhino-forge:${prop("deps.kubejs_rhino")}",
        propOrNull("deps.kubejs_architectury")?.let { "dev.architectury:architectury-forge:$it" },
    )) modRuntimeOnly(mod) { isTransitive = false }
}
for ((run, tasksOfRun) in listOf("client" to listOf("runClient", "prepareClientRun"), "harness" to listOf("runClient", "prepareClientRun"), "server" to listOf("runServer", "prepareServerRun"), "gametest" to listOf("runGameTestServer", "prepareGameTestServerRun"))) {
    if (!kubejsRun) break
    val copy = tasks.register<Copy>("copyKubeJsScripts${run.replaceFirstChar { it.uppercase() }}") {
        description = "Copies harness/kubejs into the $run run's kubejs folder (-Pkubejs.run)."
        from(rootProject.file("harness/kubejs")) { exclude("README.md") }
        into(layout.projectDirectory.dir("runs/$run/kubejs"))
    }
    tasks.matching { it.name in tasksOfRun }.configureEach { dependsOn(copy) }
}
