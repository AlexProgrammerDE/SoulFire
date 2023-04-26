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
        minimize() {
            exclude(dependency("ch.qos.logback:logback-classic"))
            exclude(dependency("com.formdev:flatlaf"))
            exclude(dependency("org.fusesource.jansi:jansi"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
