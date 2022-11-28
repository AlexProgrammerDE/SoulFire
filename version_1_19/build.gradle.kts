plugins {
    id("sw.version-conventions")
}

dependencies {
    implementation("com.github.GeyserMC:MCProtocolLib:feature~1.19-SNAPSHOT")
    compileOnly(projects.serverwreckerCommon)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.12.0")
}

setupVersion("v1_19")
