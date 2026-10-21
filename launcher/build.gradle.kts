plugins {
  `sf-vulkan-runtime-consumer`
  `sf-project-conventions`
}

val runtimeAssets = configurations.resolvable("runtimeAssets") {
  extendsFrom(configurations.runtimeClasspath.get())
}

dependencies {
  implementation(project(path = ":mod", configuration = "launcherRuntimeElements")) {
    // Loom's merged client is local to :mod; the manifest pins Mojang's client JAR instead.
    exclude(group = "net.minecraft")
    exclude(group = "net.fabricmc", module = "dev-launch-injector")
    exclude(group = "net.fabricmc", module = "fabric-log4j-util")
  }
  implementation(libs.bundles.classtransform)
  implementation(libs.reflect)
  implementation(libs.guava)
}

val launcherArtifactName = tasks.jar.flatMap { it.archiveFileName }
val launcherCoordinate = "$group:$name:$version"

val generateLauncherAssets = tasks.register<com.soulfiremc.gradle.GenerateLauncherAssets>("generateLauncherAssets") {
  minecraftVersion.set(providers.gradleProperty("minecraft_version"))
  libraries.from(runtimeAssets)
  libraries.from(tasks.jar)
  coordinates.set(runtimeAssets.zip(launcherArtifactName) { configuration, artifactName ->
    configuration.resolvedConfiguration.resolvedArtifacts.associate { artifact ->
      val id = artifact.moduleVersion.id
      artifact.file.name to listOfNotNull(id.group, id.name, id.version, artifact.classifier).joinToString(":")
    } + (artifactName to launcherCoordinate)
  })
  metadataDirectory.set(rootProject.layout.projectDirectory.dir(".gradle/launcher-assets"))
  outputDirectory.set(layout.buildDirectory.dir("generated/launcher-assets"))
}

val launcherAssetsElements = configurations.consumable("launcherAssetsElements") {
  outgoing.artifact(generateLauncherAssets.flatMap { it.outputDirectory })
}
