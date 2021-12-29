plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.11-6fc80e17c8-1")
    compileOnly(projects.serverwreckerCommon)
}
