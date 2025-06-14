plugins {
  `sf-project-conventions`
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  api(projects.buildData)

  api("org.ow2.asm:asm:9.8")
  api("org.ow2.asm:asm-analysis:9.8")
  api("org.ow2.asm:asm-commons:9.8")
  api("org.ow2.asm:asm-tree:9.8")
  api("org.ow2.asm:asm-util:9.8")
  api("net.fabricmc:sponge-mixin:0.15.5+mixin.0.8.7")
  api("net.fabricmc:intermediary:1.21.6-pre4")
  api("net.fabricmc:fabric-loader:0.16.14")

  api("com.fasterxml.jackson.core:jackson-annotations:2.19.1")
  api("com.fasterxml.jackson.core:jackson-core:2.19.1")
  api("com.fasterxml.jackson.core:jackson-databind:2.19.1")
  api("com.github.oshi:oshi-core:6.8.2")
  api("com.github.stephenc.jcip:jcip-annotations:1.0-1")
  api("com.google.code.gson:gson:2.13.1")
  api("com.google.guava:failureaccess:1.0.3")
  api("com.google.guava:guava:33.4.8-jre")
  api("com.ibm.icu:icu4j:76.1")
  api("com.microsoft.azure:msal4j:1.21.0")
  api("com.mojang:authlib:6.0.58")
  api("com.mojang:blocklist:1.0.10")
  api("com.mojang:brigadier:1.3.10")
  api("com.mojang:datafixerupper:8.0.16")
  api("com.mojang:jtracy:1.0.29")
  api("com.mojang:logging:1.5.10")
  api("com.mojang:patchy:2.2.10")
  api("com.mojang:text2speech:1.18.11")
  api("com.nimbusds:content-type:2.3")
  api("com.nimbusds:lang-tag:1.7")
  api("com.nimbusds:nimbus-jose-jwt:9.48")
  api("com.nimbusds:oauth2-oidc-sdk:11.25")
  api("commons-codec:commons-codec:1.18.0")
  api("commons-io:commons-io:2.19.0")
  api("commons-logging:commons-logging:1.3.5")
  api("it.unimi.dsi:fastutil:8.5.15")
  api("net.java.dev.jna:jna-platform:5.17.0")
  api("net.java.dev.jna:jna:5.17.0")
  api("net.minidev:accessors-smart:2.5.2")
  api("net.minidev:json-smart:2.5.2")
  api("net.sf.jopt-simple:jopt-simple:5.0.4")
  api("org.apache.commons:commons-compress:1.27.1")
  api("org.apache.commons:commons-lang3:3.17.0")
  api("org.apache.httpcomponents:httpclient:4.5.14")
  api("org.apache.httpcomponents:httpcore:4.4.16")
  api("org.jcraft:jorbis:0.0.17")
  api("org.joml:joml:1.10.8")
  api("org.lwjgl:lwjgl-freetype:3.3.6")
  api("org.lwjgl:lwjgl-glfw:3.3.6")
  api("org.lwjgl:lwjgl-jemalloc:3.3.6")
  api("org.lwjgl:lwjgl-openal:3.3.6")
  api("org.lwjgl:lwjgl-opengl:3.3.6")
  api("org.lwjgl:lwjgl-stb:3.3.6")
  api("org.lwjgl:lwjgl-tinyfd:3.3.6")
  api("org.lwjgl:lwjgl:3.3.6")
  api("org.lz4:lz4-java:1.8.0")
  api("org.slf4j:slf4j-api:2.0.17")

  api("headlessmc:headlessmc-lwjgl:2.6.1:no-asm@jar")
  api("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.3")
  api("org.checkerframework:checker-qual:3.49.4")

  // For CLI support
  api(libs.picoli)

  api(projects.buildData)
  api(projects.proto)

  // Main protocol library
  api(libs.bundles.kyori)

  // Newest netty
  api("io.netty:netty-all:4.2.2.Final")

  api("io.github.classgraph:classgraph:4.8.179")

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
  api(libs.fastutil)
  api(libs.caffeine)
  api(libs.jetbrains.annotations)
  api(libs.immutables.gson)

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
  api(
    files(
      configurations.detachedConfiguration(
        *libs.bundles.graalvm.polyglot.get()
          .map { it -> dependencies.create(it) }
          .toTypedArray())
        .resolve()
        .filter { file -> file.name.endsWith(".jar") }
    ))
  api(libs.bundles.swc4j)

  // For mail support
  api(libs.angus)

  // For tls cert provisioning
  api(libs.acme4j)
}
