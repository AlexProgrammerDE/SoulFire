plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.16.5-2")
    compileOnly(projects.serverwreckerCommon)
}

setupVersion("v1_10")
