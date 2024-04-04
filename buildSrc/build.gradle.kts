plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  // this is OK as long as the same version catalog is used in the main build and build-logic
  // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(libs.gradle.plugin.shadow)
  implementation(libs.gradle.plugin.licenser)
  implementation(libs.gradle.plugin.lombok)
  implementation(libs.gradle.plugin.indra.git)
}
