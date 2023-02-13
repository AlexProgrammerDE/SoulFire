plugins {
    id("sw.shadow-conventions")
}

dependencies {
    api("com.github.GeyserMC:mcauthlib:d9d773e5d5")
    api("com.github.GeyserMC:packetlib:3.0.1")
    setOf(
        "version_1_7",
        "version_1_8",
        "version_1_9",
        "version_1_12",
        "version_1_17",
        "version_1_18",
        "version_1_19",
    ).forEach {
        api(project(":serverwrecker-$it", "shadow"))
        compileOnly(project(":serverwrecker-$it"))
    }
    compileOnly(projects.serverwreckerCommon)
}
