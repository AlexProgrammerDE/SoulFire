plugins {
    application
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "2.5.4"
}

repositories {
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://jitpack.io/") {
        name = "JitPack Repository"
    }
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft Repository"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype Repository"
    }
    maven("https://repo.viaversion.com/")
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

application {
    mainClass.set("net.pistonmaster.serverwrecker.Main")
}

dependencies {
    implementation("info.picocli:picocli:4.7.3")
    annotationProcessor("info.picocli:picocli-codegen:4.7.3")

    implementation("com.mojang:brigadier:1.1.8")
    implementation("com.formdev:flatlaf:3.1.1")
    implementation("org.pf4j:pf4j:3.9.0")

    implementation("com.thealtening.api:api:4.1.0")

    implementation("net.kyori:adventure-text-serializer-plain:4.13.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")

    implementation("net.kyori:event-api:3.0.0")
    implementation("ch.jalu:injector:1.0")

    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.velocitypowered:velocity-native:3.1.2-SNAPSHOT")
    implementation("org.powernukkit.fastutil:fastutil-lite:8.1.1")

    implementation("com.github.GeyserMC:MCProtocolLib:master-SNAPSHOT")

    val vvVer = "4.7.0-23w14a-SNAPSHOT"
    implementation("com.viaversion:viaversion:$vvVer") { isTransitive = false }
}

tasks.compileJava.get().apply {
    options.compilerArgs.add("-Aproject=${project.name}")
}

tasks.named<Jar>("jar").get().manifest {
    attributes["Main-Class"] = "net.pistonmaster.serverwrecker.Main"
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
