plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.10-752d2d429f-1")
    compileOnly(projects.serverwreckerCommon)
}
