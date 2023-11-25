plugins {
    id("sw.license-conventions")
    id("sw.java-conventions")
    id("net.kyori.blossom")
    id("net.kyori.indra.git")
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
