import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "3.0.4"
}

version = "1.2.0-SNAPSHOT"

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

    implementation(libs.bundles.log4j)
    implementation(libs.terminalconsoleappender)
    implementation(libs.slf4j)
    implementation(libs.disruptor)

    implementation(libs.brigadier)

    implementation(libs.picoli)
    annotationProcessor(libs.picoli.codegen)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Main protocol library
    implementation(libs.mcprotocollib)

    // For advanced encryption and compression
    implementation(libs.velocity.native)

    // For advanced account authentication
    implementation("net.raphimc:MinecraftAuth:2.1.4") {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }

    // For supporting multiple Minecraft versions
    implementation("com.viaversion:viaversion:4.7.0") { isTransitive = false }
    implementation("com.viaversion:viabackwards:4.7.0") { isTransitive = false }
    implementation("com.viaversion:viarewind-core:2.0.4-SNAPSHOT")

    implementation("net.raphimc:ViaLegacy:2.2.17")
    implementation("net.raphimc:ViaAprilFools:2.0.8")
    implementation("net.raphimc:ViaLoader:2.2.7") {
        exclude("org.slf4j", "slf4j-api")
        exclude("org.yaml", "snakeyaml")
    }

    // For Bedrock support
    implementation("net.raphimc:ViaBedrock:0.0.2-SNAPSHOT") {
        exclude("io.netty", "netty-codec-http")
    }
    implementation("org.cloudburstmc.netty:netty-transport-raknet:1.0.0.CR1-SNAPSHOT") {
        isTransitive = false
    }

    implementation(libs.flatlaf)
    implementation(libs.flatlaf.intellij.themes)
    implementation(libs.flatlaf.extras)
    implementation("org.knowm.xchart:xchart:3.8.5")

    implementation(libs.brigadier)
    implementation(libs.pf4j) {
        isTransitive = false
    }
    implementation(libs.jansi)
    implementation(libs.guava)
    implementation(libs.gson)
    implementation("commons-validator:commons-validator:1.7")
    implementation("commons-io:commons-io:2.13.0")

    implementation("com.thealtening.api:api:4.1.0")

    implementation("net.kyori:adventure-text-serializer-plain:4.14.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.14.0")

    implementation("net.kyori:event-api:5.0.0-SNAPSHOT")
    implementation("ch.jalu:injector:1.0")
    implementation("org.yaml:snakeyaml:2.1")

    val javaFXVersion = "20"
    val javaFXModules = listOf("base", "graphics", "controls", "swing")
    val javaFXPlatforms = listOf("linux", "linux-aarch64", "mac", "mac-aarch64", "win")

    javaFXModules.forEach { module ->
        javaFXPlatforms.forEach { platform ->
            implementation("org.openjfx:javafx-$module:$javaFXVersion:$platform")
        }
    }
}

tasks.compileJava.get().apply {
    options.compilerArgs.add("-Aproject=${project.name}")
}

val mcFolder = File("${rootDir}/assets/minecraft")
if (!mcFolder.exists()) {
    throw IllegalStateException("Minecraft folder not found!")
}

tasks.named<Jar>("jar").get().apply {
    registerMCJar()
    manifest {
        attributes["Main-Class"] = "net.pistonmaster.serverwrecker.ServerWreckerBootstrap"
        attributes["Add-Opens"] = moduleOpens.joinToString(" ")
    }
}

tasks.named<ShadowJar>("shadowJar").get().apply {
    registerMCJar()
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

fun CopySpec.registerMCJar() {
    from(mcFolder) {
        into("minecraft")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

val copyMinecraft = tasks.create("copyMinecraft") {
    copy {
        from(mcFolder)
        into(layout.buildDirectory.file("resources/main/minecraft"))
    }
}

tasks.named("processResources").get().dependsOn(copyMinecraft)

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
