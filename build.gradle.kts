plugins {
    application
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "2.5.4"
}

repositories {
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.viaversion.com/")
    maven("https://maven.lenni0451.net/releases/")
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
    mainClass.set("net.pistonmaster.serverwrecker.Main")
}

dependencies {
    implementation(projects.buildData)

    implementation("ch.qos.logback:logback-classic:1.4.7")

    implementation("info.picocli:picocli:4.7.3")
    annotationProcessor("info.picocli:picocli-codegen:4.7.3")

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    implementation("com.mojang:brigadier:1.1.8")
    implementation("com.formdev:flatlaf:3.1.1")
    implementation("org.pf4j:pf4j:3.9.0") {
        isTransitive = false
    }
    implementation("org.fusesource.jansi:jansi:2.4.0")

    implementation("com.thealtening.api:api:4.1.0")

    implementation("net.kyori:adventure-text-serializer-plain:4.13.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")

    implementation("net.kyori:event-api:5.0.0-SNAPSHOT")
    implementation("ch.jalu:injector:1.0")

    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.velocitypowered:velocity-native:3.1.2-SNAPSHOT")
    implementation("org.powernukkit.fastutil:fastutil-lite:8.1.1")

    implementation("com.github.GeyserMC:MCProtocolLib:master-SNAPSHOT")

    val vvVer = "4.7.0-23w16a-SNAPSHOT"
    val vbVer = "4.7.0-23w16a-SNAPSHOT"
    val vrVer = "5f7fdc5"
    implementation("com.viaversion:viaversion:$vvVer") { isTransitive = false }
    implementation("com.viaversion:viabackwards:$vbVer") { isTransitive = false }
    implementation("com.github.ViaVersion.ViaRewind:viarewind-all:$vrVer") { isTransitive = false }
    implementation("net.raphimc:ViaAprilFools:2.0.6")
    implementation("net.raphimc:ViaLegacy:2.2.16")
}

tasks.compileJava.get().apply {
    options.compilerArgs.add("-Aproject=${project.name}")
}

tasks.named<Jar>("jar").get().manifest {
    attributes["Main-Class"] = "net.pistonmaster.serverwrecker.Main"
}

tasks {
    processResources {
        excludes += "**/minecraft/**"
        dependsOn("copyMinecraft")
    }
    create("copyMinecraft") {
        copy {
            from(layout.projectDirectory.file("/src/main/resources/minecraft"))
            into(layout.buildDirectory.file("resources/main/minecraft"))
        }
    }
}

launch4j {
    mainClassName = "net.pistonmaster.serverwrecker.Main"
    icon = "${rootDir}/assets/robot.ico"
    headerType = "gui"
    productName = "ServerWrecker"
    internalName = "ServerWrecker"
    companyName = "AlexProgrammerDE"
    copyright = "© 2023 AlexProgrammerDE"
    copyConfigurable = emptyArray<Any>()
    jarTask = project.tasks.shadowJar.get()
}
