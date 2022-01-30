import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.kotlin.dsl.accessors._0f69d7ba4d7e65fd0b32302f27f7e401.shadowJar
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named

fun Project.setupVersion(versionNumber: String) {
    val relocateShadowJar = tasks.create<ConfigureShadowRelocation>("relocateShadowJar") {
        target = tasks.shadowJar.get()
        prefix = "com.github.steveice10.$versionNumber.shadow"
    }

    tasks.named<ShadowJar>("shadowJar").get().apply {
        dependsOn(relocateShadowJar)
        relocate("com.github.steveice10", "com.github.steveice10.$versionNumber") {
            exclude("com.github.steveice10.mc.auth.data.GameProfile")
        }
        // Fix for relocating through the configure-relocation-tasks
        relocate("com.github.steveice10.mc.auth.data.GameProfile", "com.github.steveice10.mc.auth.data.GameProfile")
    }
}
