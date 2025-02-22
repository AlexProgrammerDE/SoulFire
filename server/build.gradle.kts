import me.champeau.jmh.JMHTask

plugins {
  `sf-project-conventions`
  alias(libs.plugins.jmh)
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  implementation(projects.buildData)
  api(projects.proto)

  // Main protocol library
  api(libs.mcprotocollib) {
    exclude("io.netty")
  }
  api(libs.mcstructs)
  api(libs.bundles.kyori)

  // Netty raknet support for ViaBedrock
  api(libs.netty.raknet) {
    isTransitive = false
  }

  // For supporting multiple Minecraft versions
  api(libs.via.version) { isTransitive = false }
  api(libs.via.backwards) { isTransitive = false }
  api(libs.via.rewind)
  api(libs.via.legacy)
  api(libs.via.aprilfools)
  api(libs.via.loader) {
    exclude("org.slf4j", "slf4j-api")
    exclude("org.yaml", "snakeyaml")
  }

  // For Bedrock support
  api(libs.via.bedrock) {
    exclude("io.netty")
  }

  // For YAML support (ViaVersion)
  api(libs.snakeyaml)

  // For microsoft account authentication
  api(libs.minecraftauth) {
    exclude("com.google.code.gson", "gson")
    exclude("org.slf4j", "slf4j-api")
  }

  // For profiling
  api(libs.spark)

  // Log/Console libraries
  api(libs.bundles.log4j)
  api(libs.jline)
  api(libs.jansi)
  api(libs.bundles.ansi4j)
  api(libs.terminalconsoleappender)
  api(libs.slf4j)
  api(libs.disruptor)

  // For command handling
  api(libs.brigadier)

  api(libs.commons.validator)
  api(libs.commons.io)

  api(libs.oshi)
  api(libs.openai)

  api(libs.guava)
  api(libs.gson)
  api(libs.pf4j)
  annotationProcessor(libs.pf4j)
  api(libs.fastutil)
  api(libs.caffeine)
  api(libs.jetbrains.annotations)
  compileOnly(libs.immutables)
  annotationProcessor(libs.immutables)

  api(libs.bundles.armeria)
  api(libs.bundles.reactor.netty)

  api(libs.bundles.mixins)
  api(libs.reflect)
  api(libs.lambdaevents)

  // For class injection
  api(libs.injector)

  // For database support
  api(libs.bundles.hibernate)
  api(libs.hikaricp)
  api(libs.sqlite)

  // For script support
  api(libs.bundles.graalvm.polyglot)

  // For mail support
  api(libs.angus)

  // For tls cert provisioning
  api(libs.acme4j)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
}

tasks {
  withType<Checkstyle> {
    exclude("**/com/soulfiremc/server/data**")
  }
  withType<JMHTask> {
    outputs.upToDateWhen { false }
  }
}

jmh {
  warmupIterations = 2
  iterations = 2
  fork = 2
}
