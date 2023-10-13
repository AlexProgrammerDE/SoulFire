import com.google.gson.GsonBuilder
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

plugins {
    `java-library`
    `maven-publish`
    id("sw.license-conventions")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
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
