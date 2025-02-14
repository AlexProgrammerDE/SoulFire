plugins {
  base
  id("io.freefair.javadoc-utf-8")
  id("io.freefair.aggregate-javadoc")
}

dependencies {
  javadocClasspath("org.projectlombok:lombok:1.18.36")
  javadocClasspath(libs.immutables)

  rootProject.subprojects.forEach { subproject ->
    if (subproject.name == "data-generator") {
      return@forEach
    }

    subproject.plugins.withId("java") {
      javadoc(subproject)
    }
  }
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
