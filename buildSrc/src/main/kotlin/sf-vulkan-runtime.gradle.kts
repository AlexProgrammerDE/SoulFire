import com.soulfiremc.gradle.BuildVulkanRuntime
import com.soulfiremc.gradle.PrepareVulkanRuntime
import com.soulfiremc.gradle.VulkanPlatforms

val hostPlatform = VulkanPlatforms.host(providers.systemProperty("os.name").get(), providers.systemProperty("os.arch").get())
val selectedPlatforms = providers.gradleProperty("vulkanPlatforms").map(VulkanPlatforms::select).getOrElse(listOf(hostPlatform))
val prebuiltDirectory = providers.gradleProperty("vulkanRuntimeDirectory")

val buildVulkanRuntime = tasks.register<BuildVulkanRuntime>("buildVulkanRuntime") {
  group = "build"
  description = "Builds the Vulkan loader and lavapipe for the current host."
  sourceRoot.set(layout.projectDirectory)
  script.set(layout.projectDirectory.file("scripts/build-vulkan-runtime.py"))
  nativeSources.set(layout.projectDirectory.dir("build-data/vulkan"))
  platform.set(hostPlatform)
  jobs.set(providers.gradleProperty("vulkanBuildJobs").map(String::toInt).orElse(Runtime.getRuntime().availableProcessors().coerceIn(1, 12)))
  useDocker.set(providers.gradleProperty("vulkanUseDocker").map(String::toBooleanStrict).orElse(hostPlatform.startsWith("linux-")))
  pythonExecutable.set(providers.gradleProperty("vulkanPython").orElse(if (hostPlatform.startsWith("windows-")) "python" else "python3"))
  runtimeJar.set(layout.buildDirectory.file("vulkan-native/soulfire-vulkan-$hostPlatform.jar"))
}

tasks.register<PrepareVulkanRuntime>("prepareVulkanRuntime") {
  group = "build"
  description = "Prepares and validates the selected Vulkan runtime packages for the launchers."
  platforms.set(selectedPlatforms)
  destination.set(layout.buildDirectory.dir("vulkan-runtime"))
  if (prebuiltDirectory.isPresent) {
    val directory = layout.projectDirectory.dir(prebuiltDirectory.get())
    require(directory.asFile.canonicalFile != destination.get().asFile.canonicalFile) {
      "vulkanRuntimeDirectory must differ from build/vulkan-runtime, which Gradle manages as an output."
    }
    runtimeJars.from(selectedPlatforms.map { directory.file("soulfire-vulkan-$it.jar") })
  } else {
    require(selectedPlatforms == listOf(hostPlatform)) {
      "This host builds $hostPlatform. For other platforms, set vulkanRuntimeDirectory to packages built on matching hosts."
    }
    runtimeJars.from(buildVulkanRuntime.flatMap { it.runtimeJar })
  }
}
