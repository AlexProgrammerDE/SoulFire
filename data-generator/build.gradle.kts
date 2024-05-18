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
  implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.10")
}

unimined.minecraft {
  version("1.20.6")

  mappings {
    intermediary()
    mojmap()
    parchment("1.20.6", "2024.05.01")

    devFallbackNamespace("intermediary")
  }

  runs.config("server") {
    javaVersion = JavaVersion.VERSION_21
  }

  fabric {
    loader("0.15.10")
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
