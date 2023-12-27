plugins {
    id("sw.java-conventions")
    alias(libs.plugins.blossom)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", rootProject.version.toString())
                property("description", rootProject.description)
                property("url", "https://github.com/AlexProgrammerDE/ServerWrecker")
                property("commit", indraGit.commit()?.name ?: "unknown")
            }
        }
    }
}
