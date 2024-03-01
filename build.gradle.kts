plugins {
    base
}

val mavenVersion: String by project

allprojects {
    group = "com.soulfiremc"
    version = mavenVersion
    description = "Advanced Minecraft Server-Stresser Tool."

    // Uppercase all artifacts
    tasks.withType<AbstractArchiveTask> {
        if (archiveBaseName.isPresent && archiveBaseName.get() == "soulfire") {
            archiveBaseName.set("SoulFire")
        }
    }
}
