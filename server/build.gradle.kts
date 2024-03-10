plugins {
    `sf-java-conventions`
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(projects.buildData)
    implementation(projects.proto)
    implementation(projects.common)

    // Main protocol library
    api(libs.mcprotocollib)
    api(libs.bundles.kyori)

    // For advanced encryption and compression
    implementation(libs.velocity.native)

    // Netty raknet support for ViaBedrock
    implementation(libs.netty.raknet) {
        isTransitive = false
    }

    // For supporting multiple Minecraft versions
    implementation(libs.via.version) { isTransitive = false }
    implementation(libs.via.backwards) { isTransitive = false }
    implementation(libs.via.rewind)
    implementation(libs.via.legacy)
    implementation(libs.via.aprilfools)
    implementation(libs.via.loader) {
        exclude("org.slf4j", "slf4j-api")
        exclude("org.yaml", "snakeyaml")
    }

    // For Bedrock support
    implementation(libs.via.bedrock) {
        exclude("io.netty", "netty-codec-http")
    }

    // For YAML support (ViaVersion)
    api(libs.snakeyaml)

    api(libs.bundles.mixins)
    api(libs.reflect)
    api(libs.lambdaevents)

    // For microsoft account authentication
    api(libs.minecraftauth) {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }

    // For class injection
    api(libs.injector)

    testImplementation(libs.junit)
}

tasks {
    withType<Checkstyle> {
        exclude("**/com/soulfiremc/server/data**")
    }
}

jmh {
    warmupIterations = 2
    iterations = 2
    fork = 2
}
