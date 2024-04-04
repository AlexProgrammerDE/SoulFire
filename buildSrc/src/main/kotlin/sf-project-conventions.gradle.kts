plugins {
  id("sf-java-conventions")
  `maven-publish`
}

publishing {
  repositories {
    val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
    maven("https://repo.codemc.org/repository/${repoName}/") {
      name = "codemc"
      credentials {
        username = System.getenv("CODEMC_USERNAME")
        password = System.getenv("CODEMC_PASSWORD")
      }
    }
  }

  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name = "SoulFire"
        description = rootProject.description
        url = "https://soulfiremc.com"
        organization {
          name = "AlexProgrammerDE"
          url = "https://pistonmaster.net"
        }
        developers {
          developer {
            id = "AlexProgrammerDE"
            timezone = "Europe/Berlin"
            url = "https://pistonmaster.net"
          }
        }
        licenses {
          license {
            name = "GNU General Public License v3.0"
            url = "https://www.gnu.org/licenses/gpl-3.0.html"
          }
        }
        scm {
          connection = "scm:git:https://github.com/AlexProgrammerDE/SoulFire.git"
          developerConnection = "scm:git:ssh://git@github.com/AlexProgrammerDE/SoulFire.git"
          url = "https://github.com/AlexProgrammerDE/SoulFire"
        }
        ciManagement {
          system = "GitHub Actions"
          url = "https://github.com/AlexProgrammerDE/SoulFire/actions"
        }
        issueManagement {
          system = "GitHub"
          url = "https://github.com/AlexProgrammerDE/SoulFire/issues"
        }
      }
    }
  }
}
