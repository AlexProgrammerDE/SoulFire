plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.12-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
}

setupVersion("v1_12")
