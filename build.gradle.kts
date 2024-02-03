plugins {
    application
    idea
    id("sf.java-conventions")
    alias(libs.plugins.jmh)
}

allprojects {
    group = "net.pistonmaster"
    version = "1.6.0-SNAPSHOT"
    description = "Advanced Minecraft Server-Stresser Tool."

    // Uppercase all artifacts
    tasks.withType<AbstractArchiveTask> {
        if (archiveBaseName.isPresent && archiveBaseName.get() == "soulfire") {
            archiveBaseName.set("SoulFire")
        }
    }
}

var mainClassString = "net.pistonmaster.soulfire.launcher.SoulFireJava8Launcher"

application {
    applicationName = "SoulFire"
    mainClass = mainClassString
}

dependencies {
    implementation(projects.buildData)
    implementation(projects.proto)

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
    implementation(libs.bundles.flatlaf)
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

    api(libs.bundles.kyori)

    api(libs.commons.validator)
    api(libs.commons.io)
    api(libs.httpclient)

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

    // For detecting the dir to put data in
    implementation(libs.appdirs)

    // For microsoft account authentication
    api(libs.minecraftauth) {
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
    }

    // For class injection
    api(libs.injector)
}

fun Manifest.applySFAttributes() {
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

tasks {
    distTar {
        onlyIf { false }
    }
    distZip {
        onlyIf { false }
    }
    startScripts {
        onlyIf { false }
    }
    // So the run task doesn't get marked as up-to-date, ever.
    run.get().apply {
        outputs.upToDateWhen { false }
    }
    withType<Checkstyle> {
        exclude("**/net/pistonmaster/soulfire/data**")
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
    val uberJar = register<Jar>("uberJar") {
        dependsOn(jar)
        from(zipTree(jar.get().outputs.files.singleFile))

        manifest.applySFAttributes()

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .filter { !it.toString().contains("build/libs") }
        }) {
            into("META-INF/lib")
        }
    }
    build {
        dependsOn(uberJar)
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
