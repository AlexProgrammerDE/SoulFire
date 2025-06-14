import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
  alias(libs.plugins.jmh)
}

val modImplementation: Configuration by configurations.creating

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  compileOnly(projects.launcher)

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.2")

  modImplementation("com.viaversion:viafabricplus:4.1.4")
  modImplementation("net.kyori:adventure-platform-fabric:6.4.0")

  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
  testImplementation(projects.launcher)
}

tasks {
  test {
    classpath += sourceSets.main.get().compileClasspath
  }
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
    standardInput = System.`in`
  }

  fabric {
    loader("0.16.14")
    accessWidener(project.projectDir.resolve("src/main/resources/soulfire.accesswidener"))
  }

  mods.modImplementation {
    mixinRemap {
      @Suppress("UnstableApiUsage")
      reset()
      enableBaseMixin()
      enableMixinExtra()
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
