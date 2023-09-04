enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("org.cadixdev.licenser") version "0.6.1"
        id("net.kyori.indra") version "3.1.3"
        id("net.kyori.indra.git") version "3.1.1"
        id("net.kyori.blossom") version "2.0.1"
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://jitpack.io/") {
            name = "JitPack Repository"
        }
        maven("https://libraries.minecraft.net/") {
            name = "Minecraft Repository"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Repository"
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            file("gradle/libs.versions.toml")
        }
    }
}

include("build-data")

rootProject.name = "serverwrecker"
