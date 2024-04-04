plugins {
  `sf-project-conventions`
}

dependencies {
  implementation(projects.buildData)
  api(projects.proto)

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

  api(libs.guava)
  api(libs.gson)
  api(libs.pf4j) {
    isTransitive = false
  }
  api(libs.fastutil)
  api(libs.caffeine)
  api(libs.jetbrains.annotations)

  api(libs.bundles.armeria)
  api(libs.bundles.reactor.netty)

  api(libs.bundles.mixins)
  api(libs.reflect)
  api(libs.lambdaevents)

  // For detecting the dir to put data in
  api(libs.appdirs)

  // For class injection
  api(libs.injector)
}
