plugins {
    id("net.pistonmaster.java-conventions")
}

dependencies {
    implementation(projects.serverwreckerCommon)
    implementation(projects.serverwreckerProtocol)
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("com.mojang:brigadier:1.0.18")
    implementation("com.formdev:flatlaf:1.4")
    implementation("com.formdev:flatlaf-intellij-themes:1.4")
    implementation("org.pf4j:pf4j:3.6.0")
    implementation("com.thealtening.api:api:4.1.0")
    implementation("com.google.guava:guava:21.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.8.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.8.1")
}
