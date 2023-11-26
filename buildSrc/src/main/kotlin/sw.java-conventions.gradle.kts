plugins {
    `java-library`
    `maven-publish`
    id("sw.license-conventions")
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("io.freefair.lombok")
}

java {
    javaTarget(17)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    javadoc {
        title = "ServerWrecker Javadocs"
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
}

indra {
    github("AlexProgrammerDE", "ServerWrecker") {
        ci(true)
    }

    gpl3OnlyLicense()
    publishReleasesTo("codemc-releases", "https://repo.codemc.org/repository/maven-releases/")
    publishSnapshotsTo("codemc-snapshots", "https://repo.codemc.org/repository/maven-snapshots/")

    configurePublications {
        pom {
            name.set("ServerWrecker")
            url.set("https://github.com/AlexProgrammerDE/ServerWrecker")
            organization {
                name.set("AlexProgrammerDE")
                url.set("https://pistonmaster.net")
            }
            developers {
                developer {
                    id.set("AlexProgrammerDE")
                    timezone.set("Europe/Berlin")
                    url.set("https://pistonmaster.net")
                }
            }
        }

        versionMapping {
            usage(Usage.JAVA_API) { fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) }
            usage(Usage.JAVA_RUNTIME) { fromResolutionResult() }
        }
    }
    javaVersions {
        target(17)
        minimumToolchain(17)
        strictVersions(true)
        testWith(17)
    }
}

/*
val exportDependenciesTask by tasks.registering {
    group = "export"
    description = "Export project dependencies to a JSON file"

    doLast {
        val outputFile = layout.buildDirectory.file("resources/main/dependencies.json").get().asFile

        val componentIdentifiers = mutableSetOf<ComponentIdentifier>()
        configurations.compileClasspath.get().resolvedConfiguration
            .lenientConfiguration
            .artifacts.forEach { componentIdentifiers.add(it.id.componentIdentifier) }
        configurations.testCompileClasspath.get().resolvedConfiguration
            .lenientConfiguration
            .artifacts.forEach { componentIdentifiers.add(it.id.componentIdentifier) }
        configurations.runtimeClasspath.get().resolvedConfiguration
            .lenientConfiguration
            .artifacts.forEach { componentIdentifiers.add(it.id.componentIdentifier) }

        val result = project.dependencies.createArtifactResolutionQuery()
            .forComponents(componentIdentifiers)
            .withArtifacts(MavenModule::class, MavenPomArtifact::class)
            .execute()
        val dependencies = result.resolvedComponents
            .map { artifact ->
                val module = artifact.getArtifacts(MavenPomArtifact::class).first() as ResolvedArtifactResult
                val pom = MavenXpp3Reader().read(module.file.inputStream())
                val artifactId = pom.artifactId
                val groupId = pom.groupId
                val name = pom.name ?: artifactId
                val version = pom.version
                @Suppress("HttpUrlsUsage")
                val url = pom.url?.replace("http://", "https://")

                mapOf(
                    "artifactId" to artifactId,
                    "groupId" to groupId,
                    "name" to name,
                    "version" to version,
                    "license" to pom.licenses.firstOrNull()?.name,
                    "url" to url
                )
            }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(dependencies.toJsonString())
    }
}

tasks.named("processResources").get().dependsOn(exportDependenciesTask)

fun List<Map<String, Any?>>.toJsonString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
}
*/
