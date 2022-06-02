plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:1.18.2-1")
    compileOnly(projects.serverwreckerCommon)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.11.0")
}

setupVersion("v1_18")
