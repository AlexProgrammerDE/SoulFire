plugins {
    `java-library`
    `maven-publish`
    id("sf.license-conventions")
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("net.kyori.indra.git")
    id("net.kyori.indra.checkstyle")
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
}

indra {
    github("AlexProgrammerDE", "SoulFire") {
        ci(true)
    }

    gpl3OnlyLicense()
    publishReleasesTo("codemc-releases", "https://repo.codemc.org/repository/maven-releases/")
    publishSnapshotsTo("codemc-snapshots", "https://repo.codemc.org/repository/maven-snapshots/")

    configurePublications {
        pom {
            name = "SoulFire"
            url = "https://github.com/AlexProgrammerDE/SoulFire"
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
        }

        versionMapping {
            usage(Usage.JAVA_API) { fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) }
            usage(Usage.JAVA_RUNTIME) { fromResolutionResult() }
        }
    }
    javaVersions {
        target(21)
        minimumToolchain(21)
        strictVersions(true)
        testWith(21)
    }
}
