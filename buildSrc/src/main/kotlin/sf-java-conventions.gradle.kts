plugins {
    idea
    `java-library`
    `maven-publish`
    id("sf-license-conventions")
    id("sf-checkstyle-conventions")
    id("io.freefair.lombok")
}

tasks {
    javadoc {
        title = "SoulFire Javadocs"
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-nowarn",
                "-Xlint:-unchecked",
                "-Xlint:-deprecation",
                "-Xlint:-processing"
            )
        )
        options.isFork = true
    }
    test {
        useJUnitPlatform()
    }
    jar {
        from(rootProject.file("LICENSE"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
publishing {
    repositories {
        maven("https://repo.codemc.org/repository/${repoName}/") {
            credentials.username = System.getenv("CODEMC_USERNAME")
            credentials.password = System.getenv("CODEMC_PASSWORD")
            name = "codemc"
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
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
                    connection = "scm:git:git://github.com/AlexProgrammerDE/SoulFire.git"
                    developerConnection = "scm:git:ssh://github.com/AlexProgrammerDE/SoulFire.git"
                    url = "https://github.com/AlexProgrammerDE/SoulFire"
                }
            }
        }
    }
}
