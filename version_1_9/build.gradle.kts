plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.AlexProgrammerDE:MCProtocolLib:1.9-9c37a56c70-1")
    compileOnly(projects.serverwreckerCommon)
}
