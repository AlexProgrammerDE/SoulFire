import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
  alias(libs.plugins.jmh)
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
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation("org.checkerframework:checker-qual:3.49.3")

  implementation("headlessmc:headlessmc-lwjgl:2.6.1:no-asm@jar")

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.2")
  implementation("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.2")

  modImplementation("com.viaversion:viafabricplus:4.1.3")

  // For CLI support
  api(libs.picoli)
  annotationProcessor(libs.picoli.codegen)

  implementation(projects.buildData)
  api(projects.proto)

  // Main protocol library
  api(libs.bundles.kyori)

  // Netty raknet support for ViaBedrock
  api(libs.netty.raknet) {
    isTransitive = false
  }

  // For microsoft account authentication
  api(libs.minecraftauth) {
    exclude("com.google.code.gson", "gson")
    exclude("org.slf4j", "slf4j-api")
  }

  // For profiling
  api(libs.spark) {
    exclude("org.ow2.asm", "asm")
  }

  // Log/Console libraries
  api(libs.bundles.log4j)
  annotationProcessor(libs.bundles.log4j)
  api(libs.jline)
  api(libs.jansi)
  api(libs.bundles.ansi4j)
  api(libs.terminalconsoleappender)
  api(libs.slf4j)
  api(libs.disruptor)

  api(libs.commons.validator)
  api(libs.commons.io)

  api(libs.openai)

  api(libs.guava)
  api(libs.gson)
  annotationProcessor(libs.pf4j)
  api(libs.fastutil)
  api(libs.caffeine)
  api(libs.jetbrains.annotations)
  implementation(libs.immutables.gson)
  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  api(libs.bundles.armeria)
  api(libs.bundles.reactor.netty)

  api(libs.reflect)
  api(libs.lambdaevents)

  // For database support
  api(libs.bundles.hibernate)
  api(libs.expressly)
  api(libs.hikaricp)
  api(libs.sqlite)
  api(libs.mariadb)

  // For script support
  api(libs.bundles.graalvm.polyglot)
  api(libs.swc4j)

  // For mail support
  api(libs.angus)

  // For tls cert provisioning
  api(libs.acme4j)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
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
