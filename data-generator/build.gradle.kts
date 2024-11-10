import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
}

repositories {
  maven("https://maven.parchmentmc.org") {
    name = "ParchmentMC"
  }
  mavenCentral()
}

dependencies {
  implementation("org.vineflower:vineflower:1.10.1")
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2")
}

unimined.minecraft {
  version("1.21.3")

  mappings {
    intermediary()
    mojmap()
    parchment("1.21", "2024.11.10")

    devFallbackNamespace("intermediary")
  }

  runs.config("server") {
    javaVersion = JavaVersion.VERSION_21
  }

  fabric {
    loader("0.16.5")
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
    onlyIf { false}
  }
}
