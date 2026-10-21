plugins {
  `sf-project-conventions`
}

dependencies {
  implementation(libs.gson) {
    isTransitive = false
  }
  testImplementation(libs.junit)
  testRuntimeOnly(libs.junit.launcher)
}
