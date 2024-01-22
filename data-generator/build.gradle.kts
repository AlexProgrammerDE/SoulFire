plugins {
    id("sf.java-conventions")
    alias(libs.plugins.loom)
}

repositories {
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
        parchment("org.parchmentmc.data:parchment-1.20.3:2023.12.31@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
        strictVersions(true)
        testWith(17)
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}
