import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.*

plugins {
    application
    idea
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "3.0.4"
    id("com.google.protobuf") version "0.9.4"
}

version = "1.2.0-SNAPSHOT"
description = "Advanced Minecraft Server-Stresser Tool."

repositories {
    maven("https://repo.opencollab.dev/maven-releases") {
        name = "OpenCollab Releases"
    }
    maven("https://repo.opencollab.dev/maven-snapshots") {
        name = "OpenCollab Snapshots"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC Repository"
    }
    maven("https://repo.viaversion.com/") {
        name = "ViaVersion Repository"
    }
    maven("https://maven.lenni0451.net/releases") {
        name = "Lenni0451"
    }
    maven("https://maven.lenni0451.net/snapshots") {
        name = "Lenni0451 Snapshots"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype Repository"
    }
    maven("https://jitpack.io/") {
        name = "JitPack Repository"
    }
    mavenCentral()
}

val moduleOpens = setOf(
    "java.desktop/sun.awt.X11"
)

application {
    applicationName = "ServerWrecker"
    mainClass.set("net.pistonmaster.serverwrecker.ServerWreckerBootstrap")
    applicationDefaultJvmArgs += moduleOpens.map { "--add-opens=$it=ALL-UNNAMED" }
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

    implementation(libs.kyori.event)
    implementation(libs.kyori.plain)
    implementation(libs.kyori.gson)

    implementation(libs.commons.validator)
    implementation(libs.commons.io)

    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.pf4j) {
        isTransitive = false
    }

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
    runtimeOnly(libs.grpc.netty)

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
        generatedSourceDirs.addAll(listOf(
            file("${protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.generatedFilesBaseDir}/main/java")
        ))
    }
}

tasks.compileJava.get().apply {
    options.compilerArgs.add("-Aproject=${project.name}")
}

tasks.named<Jar>("jar").get().apply {
    manifest {
        attributes["Main-Class"] = "net.pistonmaster.serverwrecker.ServerWreckerBootstrap"
        attributes["Add-Opens"] = moduleOpens.joinToString(" ")
    }
}

tasks.named<ShadowJar>("shadowJar").get().apply {
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

launch4j {
    mainClassName.set("ServerWrecker")
    icon.set("${rootDir}/assets/robot.ico")
    headerType.set("gui")
    productName.set("ServerWrecker")
    internalName.set("ServerWrecker")
    companyName.set("AlexProgrammerDE")
    copyright.set("© 2023 AlexProgrammerDE")
    copyConfigurable.set(emptyArray<Any>())
    jarFiles.set(project.tasks.shadowJar.get().outputs.files)
    requires64Bit.set(true)
    supportUrl.set("https://github.com/AlexProgrammerDE/ServerWrecker/issues")
    downloadUrl.set("https://www.oracle.com/java/technologies/downloads/#java17")
}
