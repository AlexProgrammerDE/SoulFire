plugins {
  base
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
