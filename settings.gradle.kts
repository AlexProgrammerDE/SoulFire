enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("org.cadixdev.licenser") version "0.6.1"
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
}

rootProject.name = "serverwrecker"

setOf(
    "version_1_7",
    "version_1_8",
    "version_1_9",
    "version_1_12",
    "version_1_17",
    "version_1_18",
    "version_1_19",
).forEach {
    setupSWSubproject(it)
}

setupSWSubproject("common")
setupSWSubproject("core")
setupSWSubproject("protocol")

fun setupSWSubproject(name: String) {
    setupSubproject("serverwrecker-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
