import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer;

plugins {
    id("sw.java-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    processResources {
        expand("version" to version, "description" to description)
    }

    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        mergeServiceFiles()
        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    build {
        dependsOn(shadowJar)
    }
}
