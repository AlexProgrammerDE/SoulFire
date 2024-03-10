plugins {
    `sf-java-conventions`
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(projects.buildData)
    implementation(projects.proto)
    implementation(projects.common)

    // Log/Console libraries
    implementation(libs.bundles.log4j)
    implementation(libs.jline)
    implementation(libs.jansi)
    implementation(libs.bundles.ansi4j)
    implementation(libs.terminalconsoleappender)
    api(libs.slf4j)
    implementation(libs.disruptor)

    // For command handling
    api(libs.brigadier)

    // Main protocol library
    api(libs.mcprotocollib)

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

    api(libs.bundles.kyori)

    api(libs.commons.validator)
    api(libs.commons.io)

    api(libs.guava)
    api(libs.gson)
    api(libs.pf4j) {
        isTransitive = false
    }
    api(libs.fastutil)
    api(libs.caffeine)

    api(libs.bundles.mixins)
    api(libs.reflect)
    api(libs.lambdaevents)

    // For microsoft account authentication
    api(libs.minecraftauth) {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }
    api(libs.bundles.reactor.netty)

    // For class injection
    api(libs.injector)

    testImplementation(libs.junit)
}

fun Manifest.applySFAttributes() {
    attributes["Main-Class"] = "com.soulfiremc.launcher.SoulFireJava8Launcher"
    attributes["Name"] = "SoulFire"
    attributes["Specification-Title"] = "SoulFire"
    attributes["Specification-Version"] = version.toString()
    attributes["Specification-Vendor"] = "AlexProgrammerDE"
    attributes["Implementation-Title"] = "SoulFire"
    attributes["Implementation-Version"] = version.toString()
    attributes["Implementation-Vendor"] = "AlexProgrammerDE"
    attributes["Multi-Release"] = "true"
}

tasks {
    withType<Checkstyle> {
        exclude("**/com/soulfiremc/data**")
    }
    jar {
        archiveClassifier = "unshaded"

        from(rootProject.file("LICENSE"))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .filter { it.toString().contains("build/libs") }
                .map { zipTree(it) }
        })

        manifest.applySFAttributes()
    }
}

jmh {
    warmupIterations = 2
    iterations = 2
    fork = 2
}

val repoName = if (version.toString().endsWith("SNAPSHOT")) "maven-snapshots" else "maven-releases"
publishing {
    repositories {
        maven("https://repo.codemc.org/repository/${repoName}/") {
            credentials.username = System.getenv("CODEMC_USERNAME")
            credentials.password = System.getenv("CODEMC_PASSWORD")
            name = "codemc"
        }
    }
}
