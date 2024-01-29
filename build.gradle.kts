import com.google.protobuf.gradle.id

plugins {
    application
    idea
    id("sf.shadow-conventions")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.jmh)
}

allprojects {
    group = "net.pistonmaster"
    version = "1.5.0-SNAPSHOT"
    description = "Advanced Minecraft Server-Stresser Tool."
}

var mainClassString = "net.pistonmaster.soulfire.launcher.SoulFireJava8Launcher"

application {
    applicationName = "SoulFire"
    mainClass = mainClassString
}

dependencies {
    implementation(projects.buildData)

    // The java 8 launcher takes care of notifiying the user if they are using an unsupported java version
    implementation(projects.j8Launcher)

    // Log/Console libraries
    implementation(libs.bundles.log4j)
    implementation(libs.jline)
    implementation(libs.jansi)
    implementation(libs.terminalconsoleappender)
    api(libs.slf4j)
    implementation(libs.disruptor)

    // For command handling
    api(libs.brigadier)

    // For CLI support
    implementation(libs.picoli)
    annotationProcessor(libs.picoli.codegen)

    // For GUI support
    implementation(libs.flatlaf)
    implementation(libs.flatlaf.intellij.themes)
    implementation(libs.flatlaf.extras)
    implementation(libs.xchart)
    implementation(libs.miglayout.swing)

    val lwjglVersion = "3.3.3"
    val lwjglPlatforms = listOf("linux", "macos", "macos-arm64", "windows")
    lwjglPlatforms.forEach { platform ->
        implementation("org.lwjgl:lwjgl-nfd:$lwjglVersion:natives-$platform")
        implementation("org.lwjgl:lwjgl:$lwjglVersion:natives-$platform")
    }
    implementation("org.lwjgl:lwjgl-nfd:$lwjglVersion")

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

    api(libs.kyori.plain)
    api(libs.kyori.gson)

    api(libs.commons.validator)
    api(libs.commons.io)

    api(libs.guava)
    api(libs.gson)
    api(libs.pf4j) {
        isTransitive = false
    }
    api(libs.fastutil)
    api(libs.caffeine)

    api(libs.classtransform.mixinstranslator)
    api(libs.classtransform.mixinsdummy)
    api(libs.classtransform.additionalclassprovider)
    api(libs.reflect)
    api(libs.lambdaevents)

    // For detecting the dir to put data in
    implementation(libs.appdirs)

    // For microsoft account authentication
    api(libs.minecraftauth) {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }

    // For TheAltening account authentication
    api(libs.thealtening)

    // For class injection
    api(libs.injector)

    // gRPC
    implementation(libs.grpc.proto)
    implementation(libs.grpc.services)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.netty)

    // For JsonFormat
    implementation(libs.protobuf.util)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}

tasks {
    distTar {
        onlyIf { false }
    }
    distZip {
        onlyIf { false }
    }
    shadowDistTar {
        onlyIf { false }
    }
    shadowDistZip {
        onlyIf { false }
    }
    // So the run task doesn't get marked as up-to-date, ever.
    run.get().apply {
        outputs.upToDateWhen { false }
    }
    withType<Checkstyle> {
        exclude("**/net/pistonmaster/soulfire/data**")
        exclude("**/net/pistonmaster/soulfire/grpc/generated**")
    }
    jar {
        archiveBaseName = "SoulFire"
        manifest {
            attributes["Main-Class"] = mainClassString
            attributes["Name"] = "SoulFire"
            attributes["Specification-Title"] = "SoulFire"
            attributes["Specification-Version"] = version.toString()
            attributes["Specification-Vendor"] = "AlexProgrammerDE"
            attributes["Implementation-Title"] = "SoulFire"
            attributes["Implementation-Version"] = version.toString()
            attributes["Implementation-Vendor"] = "AlexProgrammerDE"
            attributes["Multi-Release"] = "true"
        }
    }
    shadowJar {
        archiveBaseName = "SoulFire"
        excludes.addAll(
            setOf(
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/*.SF",
                "META-INF/sponge_plugins.json",
                "plugin.yml",
                "bungee.yml",
                "fabric.mod.json",
                "velocity-plugin.json"
            )
        )
    }
}

jmh {
    warmupIterations = 2
    iterations = 2
    fork = 2
}

idea {
    module {
        generatedSourceDirs.addAll(
            listOf(
                file("${protobuf.generatedFilesBaseDir}/main/grpc"),
                file("${protobuf.generatedFilesBaseDir}/main/java")
            )
        )
    }
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
