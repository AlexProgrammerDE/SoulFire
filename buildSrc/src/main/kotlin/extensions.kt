import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named

fun Project.setupVersion(versionNumber: String) {
    tasks.named<ShadowJar>("shadowJar").get().apply {
        relocate("com.github.steveice10", "com.github.steveice10.$versionNumber") {
            exclude("com.github.steveice10.mc.auth.data.GameProfile")
        }
        // Fix for relocating through the configure-relocation-tasks
        relocate("com.github.steveice10.mc.auth.data.GameProfile", "com.github.steveice10.mc.auth.data.GameProfile")
        // Fix multiple logger warning
        dependencies {
            exclude(dependency("ch.qos.logback:logback-classic"))
        }
    }
}
