plugins {
  base
  id("io.freefair.javadoc-utf-8")
  id("io.freefair.aggregate-javadoc")
}

dependencies {
  javadocClasspath("org.projectlombok:lombok:1.18.42")
  javadocClasspath(libs.immutables.value)
  javadocClasspath(libs.immutables.gson)

  rootProject.subprojects.forEach { subproject ->
    if (subproject.name == "data-generator") {
      return@forEach
    }

    subproject.plugins.withId("java") {
      javadocClasspath(subproject)
      javadoc(subproject)
    }
  }
}

tasks {
  javadoc {
    classpath += project.project("mod").sourceSets["main"].compileClasspath
  }
  build {
    dependsOn("javadoc")
  }
}

tasks.register<Javadoc>("generateTSDoc") {
  group = "documentation"
  description = "Generates the typescript documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  destinationDir = rootProject.layout.buildDirectory.asFile.get().resolve("docs/typescript/headers")
  options.doclet = "com.soulfiremc.doclet.TSDoclet"
  options.docletpath = listOf(rootProject.rootDir.resolve("buildSrc/build/libs/soulfire-buildsrc.jar"))
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
}

tasks.register<Javadoc>("generatePyDoc") {
  group = "documentation"
  description = "Generates the python documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  destinationDir = rootProject.layout.buildDirectory.asFile.get().resolve("docs/python/headers")
  options.doclet = "com.soulfiremc.doclet.PyDoclet"
  options.docletpath = listOf(rootProject.rootDir.resolve("buildSrc/build/libs/soulfire-buildsrc.jar"))
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
}

tasks {
  javadoc {
    title = "SoulFire Javadocs"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
  }
}

allprojects {
  group = "com.soulfiremc"
  version = property("maven_version")!!
  description = "Advanced Minecraft Server-Stresser Tool."

  repositories {
    maven("https://repo.viaversion.com") {
      name = "ViaVersion Repository"
      content {
        includeGroup("com.viaversion")
        includeGroup("com.viaversion.mcstructs")
        includeGroup("net.raphimc")
      }
    }
    maven("https://jitpack.io") {
      name = "Jitpack Repository"
      content {
        includeGroupByRegex("com\\.github\\..*")
      }
    }
    maven("https://libraries.minecraft.net") {
      name = "Minecraft Repository"
      content {
        includeGroup("net.minecraft")
        includeGroup("com.mojang")
      }
    }
    maven("https://nexus.lucko.me/repository/hosted/") {
      name = "Lucko Nexus"
      content {
        includeGroup("me.lucko")
        includeModule("net.kyori", "adventure-text-feature-pagination")
      }
    }
    maven("https://maven.parchmentmc.org") {
      name = "ParchmentMC"
      content {
        includeGroup("org.parchmentmc")
      }
    }
    maven("https://maven.fabricmc.net") {
      name = "FabricMC"
      content {
        includeGroup("net.fabricmc")
      }
    }
    ivy("https://github.com/3arthqu4ke") {
      patternLayout {
        artifact("/[organisation]/releases/download/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
      }
      metadataSources {
        artifact()
      }
      content {
        includeGroup("headlessmc")
      }
    }
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "Sonatype Snapshot Repository"
      mavenContent { snapshotsOnly() }
    }
    maven("https://repo.pistonmaster.net/releases") {
      name = "PistonDev Release Repository"
    }
    maven("https://repo.pistonmaster.net/snapshots") {
      name = "PistonDev Snapshots Repository"
    }
    mavenCentral()
  }
}
