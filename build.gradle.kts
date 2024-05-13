plugins {
  base
}

allprojects {
  group = "com.soulfiremc"
  version = property("maven_version")!!
  description = "Advanced Minecraft Server-Stresser Tool."

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
}
