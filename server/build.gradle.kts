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
  annotationProcessor(libs.bundles.log4j)
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
  implementation(libs.immutables.gson)
  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  api(libs.bundles.armeria)
  api(libs.bundles.reactor.netty)

  api(libs.bundles.mixins)
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

  // For extra math functions
  api(libs.joml)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
}

tasks.register<Javadoc>("generateTSDoc") {
  group = "documentation"
  description = "Generates the typescript documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  setDestinationDir(rootProject.layout.buildDirectory.asFile.get().resolve("docs/typescript/headers"))
  options.doclet = "com.soulfiremc.doclet.TSDoclet"
  options.docletpath = listOf(rootProject.rootDir.resolve("buildSrc/build/libs/soulfire-buildsrc.jar"))
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
}

tasks.register<Javadoc>("generatePyDoc") {
  group = "documentation"
  description = "Generates the python documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  setDestinationDir(rootProject.layout.buildDirectory.asFile.get().resolve("docs/python/headers"))
  options.doclet = "com.soulfiremc.doclet.PyDoclet"
  options.docletpath = listOf(rootProject.rootDir.resolve("buildSrc/build/libs/soulfire-buildsrc.jar"))
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
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
