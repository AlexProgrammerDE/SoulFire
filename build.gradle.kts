import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "3.0.0"
    id("org.openjfx.javafxplugin") version "0.0.14"
}

version = "1.0.0"

repositories {
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.viaversion.com/")
    maven("https://maven.lenni0451.net/releases/") {
        name = "Lenni0451 Releases"
    }
    maven("https://maven.lenni0451.net/snapshots") {
        name = "Lenni0451 Snapshots"
    }
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft Repository"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype Repository"
    }
    maven("https://jitpack.io/") {
        name = "JitPack Repository"
    }
    mavenCentral()
}

application {
    mainClass.set("ServerWrecker")
}

javafx {
    version = "20"
    modules = listOf("javafx.controls", "javafx.swing")
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
    implementation("net.raphimc:MinecraftAuth:2.1.3")

    // For supporting multiple Minecraft versions
    val vvVer = "4.7.0-23w18a-SNAPSHOT"
    val vbVer = "4.7.0-23w18a-SNAPSHOT"
    val vrVer = "5f7fdc5"
    implementation("com.viaversion:viaversion:$vvVer") { isTransitive = false }
    implementation("com.viaversion:viabackwards:$vbVer") { isTransitive = false }
    implementation("com.github.ViaVersion.ViaRewind:viarewind-all:$vrVer") { isTransitive = false }
    implementation("net.raphimc:ViaAprilFools:2.0.6")
    implementation("net.raphimc:ViaLegacy:2.2.16")
    implementation("net.raphimc:ViaProtocolHack:2.2.4") {
        exclude("org.slf4j", "slf4j-api")
        exclude("org.yaml", "snakeyaml")
    }
    implementation("net.raphimc:ViaBedrock:0.0.1-SNAPSHOT") {
        exclude("io.netty", "netty-codec-http")
    }
    implementation("org.cloudburstmc.netty:netty-transport-raknet:1.0.0.CR1-SNAPSHOT") {
        isTransitive = false
    }

    implementation(libs.brigadier)
    implementation(libs.flatlaf)
    implementation(libs.pf4j) {
        isTransitive = false
    }
    implementation(libs.jansi)
    implementation(libs.guava)
    implementation(libs.gson)

    implementation("li.flor:native-j-file-chooser:1.6.4")

    implementation("com.thealtening.api:api:4.1.0")

    implementation("net.kyori:adventure-text-serializer-plain:4.13.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")

    implementation("net.kyori:event-api:5.0.0-SNAPSHOT")
    implementation("ch.jalu:injector:1.0")
    implementation("org.yaml:snakeyaml:2.0")
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
        attributes["Main-Class"] = "ServerWrecker"
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
    mainClassName = "ServerWrecker"
    icon = "${rootDir}/assets/robot.ico"
    headerType = "gui"
    productName = "ServerWrecker"
    internalName = "ServerWrecker"
    companyName = "AlexProgrammerDE"
    copyright = "© 2023 AlexProgrammerDE"
    copyConfigurable = emptyArray<Any>()
    jarTask = project.tasks.shadowJar.get()
}
