if (providers.gradleProperty("gemrender.run").orNull != "false") {
    apply(from = rootProject.file("../Golem-Core/gradle/mod/gemrender-local.gradle.kts"))
    repositories {
        exclusiveContent {
            forRepository { maven("https://cursemaven.com") { name = "CurseMaven" } }
            filter { includeGroup("curse.maven") }
        }
        maven("https://maven.createmod.net") {
            name = "CreateMod"
            content { includeGroup("dev.engine-room.flywheel") }
        }
    }
    val runtimeConfiguration = extra["golem.build.gemrender.runtimeConfiguration"] as String
    val gemRender = extra.properties["golem.gemrender.local"]
        ?: "curse.maven:gemrender-1735539:${property("deps.gemrender")}"
    dependencies.add(runtimeConfiguration, gemRender)
    val flywheel = (findProperty("deps.gemrender_flywheel") as String?)?.takeIf { it.isNotBlank() }
    if (flywheel != null) {
        val minecraft = project.name.substringBefore('-')
        dependencies.add(runtimeConfiguration, "dev.engine-room.flywheel:flywheel-fabric-$minecraft:$flywheel")
    }
}
