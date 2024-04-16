plugins {
  `sf-java-conventions`
  alias(libs.plugins.loom)
}

repositories {
  maven("https://maven.parchmentmc.org") {
    name = "ParchmentMC"
  }
}

val minecraftVersion = property("minecraft_version")
val parchmentVersion = property("parchment_version")
val loaderVersion = property("loader_version")

tasks.create("generateData") {
  group = "data-generator"
  description = "Generates data for SoulFire"

  finalizedBy(tasks.runServer)
}

dependencies {
  minecraft("com.mojang:minecraft:${minecraftVersion}")
  @Suppress("UnstableApiUsage")
  mappings(loom.layered {
    officialMojangMappings {
      nameSyntheticMembers = true
    }
    parchment("org.parchmentmc.data:parchment-${parchmentVersion}@zip")
  })

  modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

tasks {
  processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
      expand(getProperties())
      expand(mutableMapOf("version" to project.version))
    }
  }
}
