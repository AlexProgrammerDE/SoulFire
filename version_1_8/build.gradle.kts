plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.8-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
