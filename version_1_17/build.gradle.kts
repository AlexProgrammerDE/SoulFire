plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.17.1-2")
    compileOnly(projects.serverwreckerCommon)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.9.3")
}
