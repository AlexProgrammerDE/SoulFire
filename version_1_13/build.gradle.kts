plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:e9307442db")
    compileOnly(projects.serverwreckerCommon)
}
