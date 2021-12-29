plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.10-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
