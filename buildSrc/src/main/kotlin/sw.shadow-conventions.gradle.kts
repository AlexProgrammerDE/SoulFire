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
        minimize() {
            exclude(dependency("com.formdev:flatlaf"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
