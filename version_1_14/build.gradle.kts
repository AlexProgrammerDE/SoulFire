plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.14.4-2")
    compileOnly(projects.serverwreckerCommon)
}

setupVersion("v1_14")
