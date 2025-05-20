import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
}

repositories {
  maven("https://maven.parchmentmc.org") {
    name = "ParchmentMC"
  }
  ivy("https://github.com/3arthqu4ke") {
    patternLayout {
      artifact("/[organisation]/releases/download/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
    }
    metadataSources {
      artifact()
    }
  }
  mavenCentral()
}

val modImplementation: Configuration by configurations.creating

dependencies {
  implementation("org.vineflower:vineflower:1.11.1")
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
  implementation(libs.reflect)
  implementation("headlessmc:headlessmc-lwjgl:2.6.1:no-asm@jar")

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.2")
  implementation("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.2")

  modImplementation("com.viaversion:viafabricplus:4.1.3")
}

unimined.minecraft {
  version("1.21.5")

  mappings {
    intermediary()
    mojmap()
    parchment("1.21.5", "2025.04.19")

    devFallbackNamespace("intermediary")
  }

  runs.config("server") {
    javaVersion = JavaVersion.VERSION_21
  }

  runs.config("client") {
    javaVersion = JavaVersion.VERSION_21
    jvmArgs("-Djoml.nounsafe=true")
  }

  fabric {
    loader("0.16.14")
  }

  mods {
    remap(modImplementation) {
    }
  }

  defaultRemapJar = true
}

tasks {
  processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
      expand(getProperties())
      expand(mutableMapOf("version" to project.version))
    }
  }
  withType<RemapJarTask> {
    onlyIf { false }
  }
}
