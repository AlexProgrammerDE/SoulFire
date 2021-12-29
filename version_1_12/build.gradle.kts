plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.12-1.12.1-1-g98ee556-15")
    compileOnly(projects.serverwreckerCommon)
}
