import xyz.wagyourtail.unimined.api.minecraft.task.AbstractRemapJarTask
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
  alias(libs.plugins.jmh)
}

val modImplementation: Configuration by configurations.creating
val include: Configuration by configurations.creating

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  compileOnly(projects.shared)

  modImplementation("com.viaversion:viafabricplus:4.1.4")
  include("com.viaversion:viafabricplus:4.1.4") {
    isTransitive = false
  }
  modImplementation("net.kyori:adventure-platform-fabric:6.4.0")
  include("net.kyori:adventure-platform-fabric:6.4.0") {
    isTransitive = false
  }

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.3")

  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  annotationProcessor(libs.picoli.codegen)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
  testImplementation(projects.shared)
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
  runs.off = true
}

configurations.create("remapped")

artifacts {
  val remapJarTask = tasks.getByName<RemapJarTask>("remapJar")
  add("remapped", remapJarTask.outputs.files.singleFile) {
    builtBy(remapJarTask)
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
  withType<AbstractRemapJarTask> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
