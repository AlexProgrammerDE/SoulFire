plugins {
  base
  id("io.freefair.javadoc-utf-8")
  id("io.freefair.aggregate-javadoc")
}

dependencies {
  javadocClasspath("org.projectlombok:lombok:1.18.42")
  javadocClasspath(libs.immutables.value)
  javadocClasspath(libs.immutables.gson)

  // Explicitly list Java subprojects for configuration cache compatibility
  listOf(
    projects.buildData,
    projects.proto,
    projects.shared,
    projects.mod,
    projects.launcher,
    projects.j8Launcher,
    projects.clientLauncher,
    projects.dedicatedLauncher
  ).forEach { projectDep ->
    javadocClasspath(projectDep)
    javadoc(projectDep)
  }
}

tasks {
  javadoc {
    dependsOn(projects.mod.dependencyProject.tasks.named("compileJava"))
    classpath += files(projects.mod.dependencyProject.tasks.named("compileJava").map {
      (it as JavaCompile).classpath
    })
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
  destinationDir = layout.buildDirectory.dir("docs/typescript/headers").get().asFile
  options.doclet = "com.soulfiremc.doclet.TSDoclet"
  options.docletpath = listOf(layout.projectDirectory.dir("buildSrc/build/libs").file("soulfire-buildsrc.jar").asFile)
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
}

tasks.register<Javadoc>("generatePyDoc") {
  group = "documentation"
  description = "Generates the python documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  destinationDir = layout.buildDirectory.dir("docs/python/headers").get().asFile
  options.doclet = "com.soulfiremc.doclet.PyDoclet"
  options.docletpath = listOf(layout.projectDirectory.dir("buildSrc/build/libs").file("soulfire-buildsrc.jar").asFile)
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
        includeGroup("net.fabricmc.fabric-api")
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
    maven("https://maven.lenni0451.net/everything") {
      name = "Lenni0451 Repository"
      content {
        includeGroup("net.raphimc")
      }
    }
    maven("https://repo.opencollab.dev/maven-snapshots") {
      name = "OpenCollab Snapshot Repository"
      content {
        includeGroup("org.cloudburstmc.netty")
      }
    }
    mavenCentral()
  }
}
