plugins {
  `sf-project-conventions`
  alias(libs.plugins.jmh)
}

dependencies {
  implementation(projects.buildData)
  api(projects.proto)
  api(projects.common)

  // Main protocol library
  api(libs.mcprotocollib) {
    exclude("io.netty")
  }
  api(libs.mcstructs)
  api(libs.bundles.kyori)
  api(libs.datafixerupper)

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

  testImplementation(libs.junit)
}

tasks {
  withType<Checkstyle> {
    exclude("**/com/soulfiremc/server/data**")
  }
}

jmh {
  warmupIterations = 2
  iterations = 2
  fork = 2
}
