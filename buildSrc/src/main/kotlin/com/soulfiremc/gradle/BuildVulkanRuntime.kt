package com.soulfiremc.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.inject.Inject

@DisableCachingByDefault(because = "Native output depends on the installed compiler and system libraries")
abstract class BuildVulkanRuntime : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val script: RegularFileProperty

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val nativeSources: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val dockerfile: RegularFileProperty

  @get:Internal
  abstract val sourceRoot: DirectoryProperty

  @get:Input
  abstract val platform: Property<String>

  @get:Input
  abstract val jobs: Property<Int>

  @get:Input
  abstract val useDocker: Property<Boolean>

  @get:Input
  abstract val pythonExecutable: Property<String>

  @get:OutputFile
  abstract val runtimeJar: RegularFileProperty

  @get:Inject
  abstract val execOperations: ExecOperations

  @TaskAction
  fun build() {
    require(jobs.get() > 0) { "vulkanBuildJobs must be positive." }
    val root = sourceRoot.get().asFile
    val output = runtimeJar.get().asFile.parentFile
    if (useDocker.get()) {
      val dockerfile = dockerfile.get().asFile
      val digest = MessageDigest.getInstance("SHA-256").digest(dockerfile.readBytes())
        .joinToString("") { "%02x".format(it) }.take(12)
      val image = "soulfire-vulkan-builder:$digest"
      execOperations.exec {
        commandLine("docker", "build", "-t", image, "-f", dockerfile, root)
      }
      fun id(flag: String): String {
        val buffer = ByteArrayOutputStream()
        execOperations.exec {
          commandLine("id", flag)
          standardOutput = buffer
        }
        return buffer.toString(Charsets.UTF_8).trim()
      }
      execOperations.exec {
        commandLine(
          "docker", "run", "--rm", "--user", "${id("-u")}:${id("-g")}", "--env", "HOME=/tmp",
          "--mount", "type=bind,source=${root.absolutePath},target=/work", image,
          "--jobs", jobs.get(), "--output-dir", "/work/${output.relativeTo(root).invariantSeparatorsPath}",
        )
      }
    } else {
      execOperations.exec {
        workingDir(root)
        commandLine(pythonExecutable.get(), script.get().asFile, "--jobs", jobs.get(), "--output-dir", output)
      }
    }
    check(runtimeJar.get().asFile.isFile) { "The native build did not produce ${runtimeJar.get().asFile}." }
  }
}
