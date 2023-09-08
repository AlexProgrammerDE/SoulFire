import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer;

plugins {
    id("sw.java-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    build {
        dependsOn(shadowJar)
    }
}
