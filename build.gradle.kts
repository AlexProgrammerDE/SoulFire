plugins {
  base
  id("io.freefair.javadoc-utf-8")
  id("io.freefair.aggregate-javadoc")
}

dependencies {
  javadocClasspath("org.projectlombok:lombok:1.18.38")
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

tasks.register<Javadoc>("generateTSDoc") {
  group = "documentation"
  description = "Generates the typescript documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  setDestinationDir(rootProject.layout.buildDirectory.asFile.get().resolve("docs/typescript/headers"))
  options.doclet = "com.soulfiremc.doclet.TSDoclet"
  options.docletpath = listOf(rootProject.rootDir.resolve("buildSrc/build/libs/soulfire-buildsrc.jar"))
  (options as StandardJavadocDocletOptions).addStringOption("v", project.version.toString())
}

tasks.register<Javadoc>("generatePyDoc") {
  group = "documentation"
  description = "Generates the python documentation for the project"

  source = tasks.javadoc.get().source
  classpath = tasks.javadoc.get().classpath
  setDestinationDir(rootProject.layout.buildDirectory.asFile.get().resolve("docs/python/headers"))
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
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
      name = "Sonatype Repository"
    }
    maven("https://repo.pistonmaster.net/releases") {
      name = "PistonDev Release Repository"
    }
    maven("https://repo.pistonmaster.net/snapshots") {
      name = "PistonDev Snapshots Repository"
    }
    maven("https://repo.pistonmaster.net/extras") {
      name = "PistonDev Extras Repository"
    }
  }
}
