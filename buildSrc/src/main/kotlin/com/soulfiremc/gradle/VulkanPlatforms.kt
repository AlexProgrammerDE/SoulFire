package com.soulfiremc.gradle

object VulkanPlatforms {
  val all = listOf("linux-x86_64", "linux-arm64", "windows-x86_64", "windows-arm64", "macos-x86_64", "macos-arm64")

  fun host(os: String, arch: String): String {
    val system = when {
      os.startsWith("Linux", ignoreCase = true) -> "linux"
      os.startsWith("Windows", ignoreCase = true) -> "windows"
      os.startsWith("Mac", ignoreCase = true) || os.equals("Darwin", ignoreCase = true) -> "macos"
      else -> error("Unsupported Vulkan build OS: $os")
    }
    val architecture = when (arch.lowercase()) {
      "amd64", "x86_64" -> "x86_64"
      "aarch64", "arm64" -> "arm64"
      else -> error("Unsupported Vulkan build architecture: $arch")
    }
    return "$system-$architecture"
  }

  fun select(value: String): List<String> {
    if (value == "all") return all
    val platforms = value.split(',').map(String::trim).distinct()
    require(platforms.all { it in all }) {
      "Unsupported vulkanPlatforms: $value. Use 'all' or a comma-separated list of ${all.joinToString()}."
    }
    return platforms
  }
}
