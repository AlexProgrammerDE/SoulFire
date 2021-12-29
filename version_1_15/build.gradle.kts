plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.15.2-1")
    compileOnly(projects.serverwreckerCommon)
}
