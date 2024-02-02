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
    id("com.gradle.enterprise") version "3.16.2"
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
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            file("gradle/libs.versions.toml")
        }
    }
}

gradleEnterprise {
    buildScan {
        if (!System.getenv("CI").isNullOrEmpty()) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

include(
    "data-generator",
    "build-data",
    "j8-launcher",
    "proto"
)

rootProject.name = "soulfire"
