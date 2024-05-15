plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
}

repositories {
  maven("https://maven.parchmentmc.org") {
    name = "ParchmentMC"
  }
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
}
