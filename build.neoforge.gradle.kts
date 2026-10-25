plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-neoforge"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

sourceSets["main"].java {
    exclude("com/ntmspace/platform/fabric/**", "com/ntmspace/platform/forge/**")
}

// The dev-only probe addon (src/probe); see the Fabric buildscript.
val probe = sourceSets.create("probe") {
    // src/probe/java and src/probe/resources: Stonecutter maps every source set onto the shared src tree itself.
    java.exclude("com/ntmspace/probe/platform/fabric/**", "com/ntmspace/probe/platform/forge/**")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

// The NeoForge this build runs on: deps.neoforge, or deps.neoforge_kubejs in a -Pkubejs.run=true dev run (KubeJS may need
// an older build; see versions/*/gradle.properties). Release jars always use deps.neoforge.
val neoforgeVersion = propOrNull("deps.neoforge_kubejs")?.takeIf { propOrNull("kubejs.run") == "true" } ?: prop("deps.neoforge")
// The oldest NeoForge the jar accepts (neoforge.mods.toml): deps.neoforge_floor where a node sets one, else the build's.
val neoforgeFloor = propOrNull("deps.neoforge_floor") ?: neoforgeVersion

neoForge {
    version = neoforgeVersion

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", "$modId,ntmprobe")
            ideName = "NTM: Space NeoForge ${name.replaceFirstChar { it.uppercase() }} ($mcVersion)"
            sourceSet = probe
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
            providers.gradleProperty("quickPlayWorld").orNull?.let {
                programArguments.addAll("--quickPlaySingleplayer", it)
            }
        }
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = file("runs/gametest")
            systemProperty("ntmprobe.doubleSubstitution", providers.gradleProperty("probe.doubleSubstitution").orElse("false").get())
            systemProperty("golem.gametest.filter", providers.gradleProperty("gametest.filter").orElse("(ntmspace|ntmprobe):.*").get())
        }
        register("server") {
            server()
            gameDirectory = file("runs/server")
        }
    }

    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
        register("ntmprobe") { sourceSet(probe) }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
}

// Dev-only mods on the run classpath, never compiled against or bundled.
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }
configurations.named("probeRuntimeClasspath") { extendsFrom(localRuntime) }

dependencies {
    // Golem Core and HBM, both from their composite builds (settings.gradle.kts).
    implementation("dev.golem:golemcore-neoforge-$mcVersion:${prop("deps.golemcore")}+$mcVersion")
    implementation("com.hbm:hbm-neoforge-$mcVersion:${prop("deps.hbm")}+$mcVersion")

    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        compileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
        compileOnly("mezz.jei:jei-$jei-neoforge-api:$jeiVer")
        localRuntime("mezz.jei:jei-$jei-neoforge:$jeiVer")
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
    "neoforge_version" to neoforgeFloor,
    "neoforge_loader_range" to prop("deps.neoforge_loader_range"),
    "java_version" to javaVersion.toString(),
    "pack_format" to prop("pack_format"),
    "pack_format_entry" to if (mcVersion.substringBefore('.').toInt() >= 26)
        "\"min_format\": ${prop("pack_format")}, \"max_format\": ${prop("pack_format")}"
    else
        "\"pack_format\": ${prop("pack_format")}",
)

/** The node's resource templating, shared by the mod's resources and the recipe generator's copy of them. */
fun ProcessResources.expandModResources() {
    inputs.properties(modProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(modProps) }
    exclude("fabric.mod.json", "data/$modId/forge/**", "META-INF/mods.toml")
}

tasks.processResources { expandModResources() }
tasks.named<ProcessResources>("processProbeResources") {
    inputs.properties(modProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(modProps) }
    exclude("fabric.mod.json", "META-INF/mods.toml")
}

tasks.jar {
    from(files(rootProject.file("LICENSE"), rootProject.file("LICENSE.LESSER"), rootProject.file("CREDITS.txt"))) { rename { "${it}_${prop("mod.id")}" } }
}

tasks.named("createMinecraftArtifacts") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.compileJava { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.named<JavaCompile>("compileProbeJava") { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst { delete(layout.projectDirectory.dir("runs/gametest/world")) }
}

extra["mc_version"] = mcVersion
extra["java_version"] = javaVersion.toString()
extra["loader_cooking_result_count"] = true
// Golem Core's shared build layer; this mod's parameters for it (golem.build.*) are in gradle.properties.
apply(from = rootProject.file("../Golem-Core/gradle/mod/run-toolchain.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/harness-spec.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-recipes.gradle.kts"))
extra["loader"] = "neoforge"
// Orders the mod before KubeJS (optional dependency) so its script groups and bindings reach the startup scripts.
apply(from = rootProject.file("../Golem-Core/gradle/mod/kubejs-order.gradle.kts"))
// Neutral "golem:conditions" in data JSON, rewritten to this loader's spelling; see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/recipe-conditions.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-dimensions.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-assets.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/dejson.gradle.kts"))
// NTM: Space's machine recipe rows, generated from src/recipes on 26.1 NeoForge (below); see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/generated-data.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/mixin-validation.gradle.kts"))

// Machine recipe rows, written in Java (src/recipes: transliterations of hbm-space's recipe classes, listed in
// com.ntmspace.recipes.SpaceDataSources; rows for NTM: Space's own tables and for HBM's) and generated into datapack
// files on this node only, by Golem Core's DataGenerator in a headless server run, as HBM does (see HBM's
// build.neoforge.gradle.kts). Every node's processResources depends on the run (../Golem-Core/gradle/mod/generated-data
// .gradle.kts). src/recipes is not shipped.
if (project.name == "26.1-neoforge") {
    val main = sourceSets["main"]
    val generatedDataRoot = rootProject.layout.buildDirectory.dir("generated/data")
    val recipes = sourceSets.create("recipes") {
        java.setSrcDirs(listOf(rootProject.file("src/recipes/java")))
        resources.setSrcDirs(listOf<File>())
        compileClasspath += main.compileClasspath + main.output.classesDirs
    }
    configurations["recipesRuntimeClasspath"].extendsFrom(configurations.runtimeClasspath.get())
    // One mod group for the run: NTM: Space's classes and resources plus the table sources (resources as processResources
    // packs them, but not processResources itself, whose output includes the generated rows).
    recipes.output.dir(mapOf("builtBy" to tasks.named("compileJava")), main.java.destinationDirectory)
    tasks.named<ProcessResources>("processRecipesResources") {
        dependsOn(tasks.named("stonecutterGenerate"), tasks.named("dejsonGenerate"))
        from(provider { main.resources.srcDirs })
        from(layout.buildDirectory.dir("generated/dejson"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        expandModResources()
    }
    recipes.runtimeClasspath = recipes.output + configurations["recipesRuntimeClasspath"]

    neoForge {
        mods { register("ntmspaceRecipes") { sourceSet(recipes) } }
        runs {
            // The generator's mod group repeats NTM: Space's classes and resources without the generated rows: every other
            // run loads the mod's own groups only, or it would hold two copies of the mod and could pick the one without them.
            matching { it.name != "generateData" }.configureEach {
                loadedMods.set(setOf(mods.getByName(modId), mods.getByName("ntmprobe")))
            }
            // HBM's generator run, for this mod: a headless GameTest server that writes once started and halts.
            register("generateData") {
                type = "gameTestServer"
                sourceSet = recipes
                loadedMods.set(setOf(mods.getByName("ntmspaceRecipes")))
                gameDirectory = file("runs/generatedata")
                systemProperty("golem.data.output", generatedDataRoot.get().asFile.absolutePath)
                systemProperty("golem.data.sources", "com.ntmspace.recipes.SpaceDataSources")
                systemProperty("golem.gametest.filter", "^$")
            }
        }
    }

    tasks.named<JavaExec>("runGenerateData") {
        group = "build"
        description = "Generates NTM: Space's machine recipe rows from src/recipes into build/generated/data."
        outputs.dir(generatedDataRoot)
        outputs.cacheIf { false }
        // The halted server's exit code means nothing; the manifest is written last, only once every row was encoded.
        isIgnoreExitValue = true
        val manifest = generatedDataRoot.map { it.file("manifest.txt") }
        doFirst {
            delete(manifest)
            delete(layout.projectDirectory.dir("runs/generatedata/world"))
        }
        doLast {
            check(manifest.get().asFile.isFile) {
                "$path wrote no ${manifest.get().asFile.name}: the generator never finished. Read the run's output above."
            }
        }
    }
}

// Credits and links in the loader metadata, from the mod.* properties.
// The uploaded jar: no GameTests (see the script).
apply(from = rootProject.file("../Golem-Core/gradle/mod/release-jar.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/mod-metadata.gradle.kts"))
// Modrinth + CurseForge upload of this node's jar; run every node with the root `publishAllMods`.
apply(from = rootProject.file("../Golem-Core/gradle/mod/publish.gradle.kts"))

// KubeJS (the optional `kubejs` peer, see deps.kubejs): served entirely by Golem Core's dev.golem.core.kubejs adapter,
// so this addon has no KubeJS code and inherits HBM's and its own recipe tables, bindings and events. Never compiled
// against, shipped or required. `-Pkubejs.run=true` puts KubeJS (and its Rhino) in the dev runs and copies
// harness/kubejs into each run's kubejs folder; off by default so the baselines stay KubeJS-free.
val kubejsRun = propOrNull("kubejs.run") == "true" && propOrNull("deps.kubejs") != null
if (kubejsRun) {
    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.latvian.dev/releases") { name = "LatvianDev" } }
            filter { includeGroup("dev.latvian.mods") }
        }
    }
}
val kubejsMod: Configuration by configurations.creating
if (kubejsRun) dependencies {
    for (mod in listOfNotNull(propOrNull("deps.kubejs_bat")?.let { "dev.latvian.mods:better-advanced-tooltips:$it" }, "dev.latvian.mods:kubejs-neoforge:${prop("deps.kubejs")}", "dev.latvian.mods:rhino:${prop("deps.kubejs_rhino")}")) {
        kubejsMod(mod) { isTransitive = false }
    }
}
// Mod files, not classpath entries (KubeJS carries mixins, which FML registers only for a jar in the mods folder).
// Always registered and run, so a run without the flag also clears the jars a flagged run left behind.
for ((run, task) in listOf("client" to "runClient", "harness" to "runClient", "server" to "runServer", "gametest" to "runGameTestServer")) {
    val modsDir = layout.projectDirectory.dir("runs/$run/mods")
    val sync = tasks.register("syncKubeJs${run.replaceFirstChar { it.uppercase() }}") {
        description = "Places KubeJS in the $run run's mods folder (-Pkubejs.run)."
        inputs.files(kubejsMod)
        outputs.dir(modsDir)
        doLast { project.sync { from(kubejsMod); into(modsDir) } }
    }
    val prepare = "prepare${task.removePrefix("run")}Run"
    tasks.matching { it.name == task || it.name == prepare }.configureEach { dependsOn(sync) }
}
for ((run, tasksOfRun) in listOf("client" to listOf("runClient", "prepareClientRun"), "server" to listOf("runServer", "prepareServerRun"), "gametest" to listOf("runGameTestServer", "prepareGameTestServerRun"))) {
    if (!kubejsRun) break
    val copy = tasks.register<Copy>("copyKubeJsScripts${run.replaceFirstChar { it.uppercase() }}") {
        description = "Copies harness/kubejs into the $run run's kubejs folder (-Pkubejs.run)."
        from(rootProject.file("harness/kubejs")) { exclude("README.md") }
        into(layout.projectDirectory.dir("runs/$run/kubejs"))
    }
    tasks.matching { it.name in tasksOfRun }.configureEach { dependsOn(copy) }
}
