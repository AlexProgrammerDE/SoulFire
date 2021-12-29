plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:e9307442db")
    compileOnly(projects.serverwreckerCommon)
}

setupVersion("v1_13")
