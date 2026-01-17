plugins {
  `sf-special-publish-conventions`
  id("net.fabricmc.fabric-loom")
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

  minecraft("com.mojang:minecraft:1.21.11_unobfuscated")
  implementation("net.fabricmc:fabric-loader:0.18.4")

  val viaFabricPlusNotation = "com.viaversion:viafabricplus:4.4.3-SNAPSHOT-UNOBF"
  implementation(viaFabricPlusNotation) {
    exclude("org.lz4")
  }
  include(viaFabricPlusNotation) {
    isTransitive = false
  }

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.3")

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
  val headlessMcNotation = "headlessmc:headlessmc-lwjgl:2.8.0:no-asm@jar"
  api(headlessMcNotation) {
    exclude("io.netty")
    exclude("org.slf4j")
  }
  include(headlessMcNotation) {
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

  processIncludeJars {
    dependsOn(":proto:jar")
  }
}

configurations.create("mod-jar")

artifacts {
  add("mod-jar", tasks.jar.flatMap { it.archiveFile }) {
    builtBy(tasks.jar)
  }
}

publishing {
  publications {
    getByName<MavenPublication>("mavenJava") {
      artifact(tasks.jar)
    }
  }
}

jmh {
  warmupIterations = 2
  iterations = 2
  fork = 2
}
