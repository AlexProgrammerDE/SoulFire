import org.gradle.api.internal.catalog.DelegatingProjectDependency
import xyz.wagyourtail.unimined.api.minecraft.task.AbstractRemapJarTask
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
  `sf-java-conventions`
  id("xyz.wagyourtail.unimined")
}

val modImplementation: Configuration by configurations.creating
val include: Configuration by configurations.creating

dependencies {
  libs.bundles.bom.get().forEach { api(platform(it)) }

  compileOnly(projects.shared)

  modImplementation("com.viaversion:viafabricplus:4.2.3")
  include("com.viaversion:viafabricplus:4.2.3") {
    isTransitive = false
  }
  modImplementation("net.kyori:adventure-platform-fabric:6.5.1")
  include("net.kyori:adventure-platform-fabric:6.5.1") {
    isTransitive = false
  }

  annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.4")

  annotationProcessor(libs.immutables.gson)
  compileOnly(libs.immutables.value)
  annotationProcessor(libs.immutables.value)

  annotationProcessor(libs.picoli.codegen)

  val excludeConf: ModuleDependency.() -> Unit = {
    exclude("io.netty")
    exclude("org.slf4j")
  }

  fun apiInclude(dependencyNotation: String) {
    api(dependencyNotation, excludeConf)
    include(dependencyNotation, excludeConf)
  }

  fun apiInclude(dependencyNotation: Provider<*>) {
    api(dependencyNotation, excludeConf)
    include(dependencyNotation, excludeConf)
  }

  fun apiInclude(dependencyNotation: ProviderConvertible<*>) {
    api(dependencyNotation, excludeConf)
    include(dependencyNotation, excludeConf)
  }

  fun apiInclude(dependencyNotation: DelegatingProjectDependency) {
    api(dependencyNotation, excludeConf)
    include(dependencyNotation, excludeConf)
  }

  // For CLI support
  apiInclude(libs.picoli)
  apiInclude(projects.proto)
  apiInclude("headlessmc:headlessmc-lwjgl:2.6.1:no-asm@jar")

  apiInclude(libs.bundles.armeria)
  apiInclude(libs.bundles.reactor.netty)

  testRuntimeOnly(libs.junit.launcher)
  testImplementation(libs.junit)
  testImplementation(projects.shared)
}

tasks {
  test {
    classpath += sourceSets.main.get().compileClasspath
  }
}

unimined.minecraft {
  version("1.21.8")

  mappings {
    intermediary()
    mojmap()
    parchment("1.21.8", "2025.07.20")
  }

  fabric {
    loader("0.16.14")
    accessWidener(project.projectDir.resolve("src/main/resources/soulfire.accesswidener"))
  }

  mods {
    modImplementation {
      mixinRemap {
        @Suppress("UnstableApiUsage")
        reset()
        enableBaseMixin()
        enableMixinExtra()
      }
    }
  }

  defaultRemapJar = true
  runs.off = true
}

configurations.create("remapped")

artifacts {
  val remapJarTask = tasks.getByName<RemapJarTask>("remapJar")
  add("remapped", remapJarTask.outputs.files.singleFile) {
    builtBy(remapJarTask)
  }
}

tasks {
  withType<AbstractRemapJarTask> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
