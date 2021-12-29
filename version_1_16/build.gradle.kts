plugins {
    id("sw.java-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.16.5-2")
    compileOnly(projects.serverwreckerCommon)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.8.1")
}
