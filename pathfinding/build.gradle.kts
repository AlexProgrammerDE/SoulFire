plugins {
  `sf-project-conventions`
}

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  // FastUtil for optimized collections used in pathfinding
  api(libs.fastutil)

  // Guava for utilities
  api(libs.guava)

  // Logging
  api(libs.slf4j)

  // Annotations
  api(libs.jetbrains.annotations)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  // Testing
  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
}
