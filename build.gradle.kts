import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.id

plugins {
    application
    idea
    id("sw.shadow-conventions")
    id("com.google.protobuf")
    id("net.kyori.indra.checkstyle")
}

group = "net.pistonmaster"
version = "1.4.0-SNAPSHOT"
description = "Advanced Minecraft Server-Stresser Tool."

application {
    applicationName = "ServerWrecker"
    mainClass.set("net.pistonmaster.serverwrecker.ServerWreckerBootstrap")
}

tasks {
    distTar {
        enabled = false
    }
    distZip {
        enabled = false
    }
    shadowDistTar {
        archiveBaseName.set("ServerWrecker")
    }
    shadowDistZip {
        archiveBaseName.set("ServerWrecker")
    }
    withType<ShadowJar> {
        archiveBaseName.set("ServerWrecker")
    }
    jar {
        archiveBaseName.set("ServerWrecker")
    }
}

// So the run task doesn't get marked as up-to-date, ever.
tasks.run.get().apply {
    outputs.upToDateWhen { false }
}

dependencies {
    implementation(projects.buildData)

    // Log/Console libraries
    implementation(libs.bundles.log4j)
    implementation(libs.jline)
    implementation(libs.jansi)
    implementation(libs.terminalconsoleappender)
    implementation(libs.slf4j)
    implementation(libs.disruptor)

    // For command handling
    implementation(libs.brigadier)

    // For CLI support
    implementation(libs.picoli)
    annotationProcessor(libs.picoli.codegen)

    // For GUI support
    implementation(libs.flatlaf)
    implementation(libs.flatlaf.intellij.themes)
    implementation(libs.flatlaf.extras)
    implementation(libs.xchart)
    implementation(libs.miglayout.swing)

    // For JavaFX file editor in GUI
    val javaFXVersion = "20"
    val javaFXModules = listOf("base", "graphics", "controls", "swing")
    val javaFXPlatforms = listOf("linux", "linux-aarch64", "mac", "mac-aarch64", "win")

    javaFXModules.forEach { module ->
        javaFXPlatforms.forEach { platform ->
            implementation("org.openjfx:javafx-$module:$javaFXVersion:$platform")
        }
    }

    // Main protocol library
    implementation(libs.mcprotocollib)

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

    // For code generation
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // For YAML support (ViaVersion)
    implementation(libs.snakeyaml)

    implementation(libs.kyori.plain)
    implementation(libs.kyori.gson)

    implementation(libs.commons.validator)
    implementation(libs.commons.io)

    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.pf4j) {
        isTransitive = false
    }
    implementation(libs.fastutil)
    implementation(libs.caffeine)

    implementation(libs.classtransform.mixinstranslator)
    implementation(libs.classtransform.mixinsdummy)
    implementation(libs.classtransform.additionalclassprovider)
    implementation(libs.reflect)
    implementation(libs.lambdaevents)

    // For microsoft account authentication
    implementation(libs.minecraftauth) {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }

    // For TheAltening account authentication
    implementation(libs.thealtening)

    // For class injection
    implementation(libs.injector)

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

tasks.withType<Checkstyle> {
    exclude("**/net/pistonmaster/serverwrecker/data**")
    exclude("**/net/pistonmaster/serverwrecker/grpc/generated**")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "net.pistonmaster.serverwrecker.ServerWreckerBootstrap"
    }
}

tasks.named<ShadowJar>("shadowJar") {
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
