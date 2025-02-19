enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "3.19.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      file("gradle/libs.versions.toml")
    }
  }
}

develocity {
  buildScan {
    val isCi = !System.getenv("CI").isNullOrEmpty()
    if (isCi) {
      termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
      termsOfUseAgree = "yes"
      tag("CI")
    }
    publishing.onlyIf { isCi }
  }
}

include(
  "data-generator",
  "build-data",
  "j8-launcher",
  "proto",
  "server",
  "dedicated",
  "client"
)

rootProject.name = "soulfire"
