plugins {
    id("sf.java-conventions")
    alias(libs.plugins.blossom)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", rootProject.version.toString())
                property("description", rootProject.description)
                property("url", "https://github.com/AlexProgrammerDE/SoulFire")
                property("commit", indraGit.commit()?.name ?: "unknown")
            }
        }
    }
}
