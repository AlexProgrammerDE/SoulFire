plugins {
    id("sw.shadow-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:mcauthlib:6f3d6aada5")
    implementation("com.github.GeyserMC:packetlib:2.1")
    setOf(
        "version_1_7",
        "version_1_8",
        "version_1_9",
        "version_1_10",
        "version_1_11",
        "version_1_12",
        "version_1_13",
        "version_1_14",
        "version_1_15",
        "version_1_16",
        "version_1_17",
        "version_1_18",
    ).forEach {
        implementation(project(":serverwrecker-$it", "shadow"))
    }
    compileOnly(projects.serverwreckerCommon)
}
