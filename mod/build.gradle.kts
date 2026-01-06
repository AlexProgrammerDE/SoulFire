import net.fabricmc.loom.task.RemapJarTask

plugins {
  `sf-special-publish-conventions`
  id("net.fabricmc.fabric-loom-remap")
  alias(libs.plugins.jmh)
}

repositories {
  maven("https://maven.parchmentmc.org") {
    name = "ParchmentMC"
    content {
      includeGroup("org.parchmentmc.data")
    }
  }
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  compileOnly(projects.shared)

  minecraft("com.mojang:minecraft:1.21.11")
  @Suppress("UnstableApiUsage")
  mappings(loom.layered {
    officialMojangMappings()
    parchment("org.parchmentmc.data:parchment-1.21.11:2025.12.20@zip")
  })
  modImplementation("net.fabricmc:fabric-loader:0.18.4")

  modImplementation("com.viaversion:viafabricplus:4.4.2-SNAPSHOT") {
    exclude("org.lz4")
  }
  include("com.viaversion:viafabricplus:4.4.2-SNAPSHOT") {
    isTransitive = false
  }

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.2")

  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  annotationProcessor(libs.picoli.codegen)

  // For CLI support
  api(libs.picoli) {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  include(libs.picoli) {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  api(projects.proto) {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  include(projects.proto) {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  api("headlessmc:headlessmc-lwjgl:2.8.0:no-asm@jar") {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  include("headlessmc:headlessmc-lwjgl:2.6.1:no-asm@jar") {
    exclude("io.netty")
    exclude("org.slf4j")
  }

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
  testImplementation(projects.shared)

  jmhImplementation(projects.shared)
}

loom {
  accessWidenerPath = file("src/main/resources/soulfire.accesswidener")

  runs {
    configureEach {
      ideConfigGenerated(false)
    }
  }
}

configurations {
  testRuntimeClasspath {
    exclude(group = "net.fabricmc", module = "fabric-log4j-util")
  }
}

tasks {
  test {
    useJUnitPlatform()
  }

  remapJar {
    dependsOn(":proto:jar")
  }
}

configurations.create("remapped")

val remapJarTask = tasks.named<RemapJarTask>("remapJar")
artifacts {
  add("remapped", remapJarTask.flatMap { it.archiveFile }) {
    builtBy(remapJarTask)
  }
}

publishing {
  publications {
    getByName<MavenPublication>("mavenJava") {
      artifact(remapJarTask)
    }
  }
}

jmh {
  warmupIterations = 2
  iterations = 2
  fork = 2
}
