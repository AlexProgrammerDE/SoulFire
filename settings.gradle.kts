enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    maven("https://maven.fabricmc.net/") {
      name = "Fabric"
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "3.17.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    maven("https://repo.opencollab.dev/maven-releases") {
      name = "OpenCollab Releases"
    }
    maven("https://repo.opencollab.dev/maven-snapshots") {
      name = "OpenCollab Snapshots"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
      name = "PaperMC Repository"
    }
    maven("https://repo.viaversion.com/") {
      name = "ViaVersion Repository"
    }
    maven("https://maven.lenni0451.net/releases") {
      name = "Lenni0451"
    }
    maven("https://maven.lenni0451.net/snapshots") {
      name = "Lenni0451 Snapshots"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
      name = "Sonatype Repository"
    }
    maven("https://jitpack.io/") {
      name = "JitPack Repository"
    }
    maven("https://repo.spring.io/milestone") {
      name = "Spring Milestone Repository"
    }
    mavenCentral()
  }
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
  "common",
  "server",
  "dedicated",
  "client"
)

rootProject.name = "soulfire"
