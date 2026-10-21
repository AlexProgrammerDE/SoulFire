package com.soulfiremc.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.zip.ZipFile

@DisableCachingByDefault(because = "Copies native runtime packages without compiling them")
abstract class PrepareVulkanRuntime : DefaultTask() {
  @get:Input
  abstract val platforms: ListProperty<String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val runtimeJars: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val destination: DirectoryProperty

  @get:Internal
  val preparedJars: Provider<List<RegularFile>>
    get() = platforms.zip(destination) { selected, directory ->
      selected.map { directory.file("soulfire-vulkan-$it.jar") }
    }

  @TaskAction
  fun prepare() {
    val sources = runtimeJars.files.associateBy { it.name }
    val selected = platforms.get().map { platform ->
      val name = "soulfire-vulkan-$platform.jar"
      val jar = sources[name]
      check(jar != null && jar.isFile) {
        "Missing $name. Build this platform on a matching host, or set vulkanRuntimeDirectory to the directory containing its package."
      }
      ZipFile(jar).use {
        check(it.getEntry("soulfire-vulkan/$platform/runtime.properties") != null) {
          "Invalid native runtime JAR: $jar"
        }
      }
      jar
    }
    val directory = destination.get().asFile
    directory.mkdirs()
    directory.listFiles()?.filter { it.name !in selected.map { jar -> jar.name } }?.forEach { it.deleteRecursively() }
    selected.forEach { it.copyTo(directory.resolve(it.name), overwrite = true) }
  }
}
