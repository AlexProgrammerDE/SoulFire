plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.7-76ed79c7cf-1")
    compileOnly(projects.serverwreckerCommon)
}
