plugins {
    id("sw.shadow-conventions")
}

dependencies {
    api("com.github.GeyserMC:mcauthlib:6f3d6aada5")
    api("com.github.GeyserMC:packetlib:3.0")
    setOf(
        "version_1_7",
        "version_1_8",
        "version_1_9",
        "version_1_12",
        "version_1_16",
        "version_1_17",
        "version_1_18",
    ).forEach {
        api(project(":serverwrecker-$it", "shadow"))
        compileOnly(project(":serverwrecker-$it"))
    }
    compileOnly(projects.serverwreckerCommon)
}
