plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.18-2")
    compileOnly(projects.serverwreckerCommon)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.9.3")
}
