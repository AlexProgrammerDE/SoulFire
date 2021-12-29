plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.9-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}
