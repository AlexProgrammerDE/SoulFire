import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id("sw.java-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    jar {
        archiveClassifier = "unshaded"
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        archiveClassifier = ""
        mergeServiceFiles()
        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    build {
        dependsOn(shadowJar)
    }
}
