plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.8-d76a51675e-1")
    compileOnly(projects.serverwreckerCommon)
}
