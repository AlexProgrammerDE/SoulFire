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
    }

    build {
        dependsOn(shadowJar)
    }
}
