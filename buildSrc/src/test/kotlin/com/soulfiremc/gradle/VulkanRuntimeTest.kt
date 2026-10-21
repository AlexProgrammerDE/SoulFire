package com.soulfiremc.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class VulkanRuntimeTest {
  @TempDir
  lateinit var directory: Path

  private val host = VulkanPlatforms.host(System.getProperty("os.name"), System.getProperty("os.arch"))

  @BeforeEach
  fun fixture() {
    directory.resolve("settings.gradle").writeText("rootProject.name = 'native-fixture'\ninclude 'vulkan-runtime'\n")
    directory.resolve("gradle.properties").writeText("org.gradle.jvmargs=-Xmx512m\n")
    directory.resolve("build.gradle").writeText(
      """
      plugins {
        id 'base'
        id 'java-library'
      }
      def nativeDependencies = configurations.dependencyScope('vulkanRuntimeDependencies')
      def nativeRuntime = configurations.resolvable('vulkanRuntime') {
        extendsFrom(nativeDependencies.get())
      }
      dependencies {
        add(nativeDependencies.name, project(':vulkan-runtime'))
        runtimeOnly(files(nativeRuntime))
      }
      tasks.register('packageRuntime', Zip) {
        from(configurations.runtimeClasspath)
        destinationDirectory = layout.buildDirectory.dir('distribution')
        archiveFileName = 'runtime.zip'
      }
      tasks.register('packageResolvedRuntime', Zip) {
        dependsOn(configurations.runtimeClasspath)
        from(configurations.named('runtimeClasspath').map { classpath ->
          classpath.files.findAll { file -> file.name.endsWith('.jar') }
        })
        destinationDirectory = layout.buildDirectory.dir('distribution')
        archiveFileName = 'resolved.zip'
      }
      """.trimIndent()
    )
    val nativeProject = directory.resolve("vulkan-runtime").createDirectories()
    nativeProject.resolve("build.gradle").writeText("plugins { id 'sf-vulkan-runtime' }\n")
    nativeProject.resolve("Dockerfile").writeText("FROM debian:bookworm-slim\n")
    nativeProject.resolve("src/main/c").createDirectories().resolve("smoke.c").writeText("int main(void) { return 0; }\n")
    nativeProject.resolve("scripts").createDirectories().resolve("build-vulkan-runtime.py").writeText(
      """
      import argparse
      from pathlib import Path
      from zipfile import ZipFile
      parser = argparse.ArgumentParser()
      parser.add_argument('--jobs', type=int)
      parser.add_argument('--output-dir', type=Path)
      args = parser.parse_args()
      args.output_dir.mkdir(parents=True, exist_ok=True)
      with ZipFile(args.output_dir / 'soulfire-vulkan-$host.jar', 'w') as jar:
          jar.writestr('soulfire-vulkan/$host/runtime.properties', 'loader=loader\ndriver=driver\n')
      """.trimIndent()
    )
  }

  private fun runner(vararg arguments: String) = GradleRunner.create()
    .withProjectDir(directory.toFile())
    .withPluginClasspath()
    .withArguments(*arguments, "--configuration-cache", "--stacktrace", "-PvulkanUseDocker=false")

  private fun prebuilt(platform: String, valid: Boolean = true) {
    val jar = directory.resolve("prebuilt").createDirectories().resolve("soulfire-vulkan-$platform.jar")
    ZipOutputStream(jar.toFile().outputStream()).use {
      it.putNextEntry(ZipEntry(if (valid) "soulfire-vulkan/$platform/runtime.properties" else "other/runtime.properties"))
      it.write("loader=loader\ndriver=driver\n".toByteArray())
      it.closeEntry()
    }
  }

  @Test
  fun `packaging builds its runtime and tracks changes and clean`() {
    val first = runner("packageRuntime").build()
    assertEquals(TaskOutcome.SUCCESS, first.task(":vulkan-runtime:buildVulkanRuntime")?.outcome)
    assertNull(first.task(":buildVulkanRuntime"))
    ZipFile(directory.resolve("build/distribution/runtime.zip").toFile()).use {
      assertEquals(listOf("soulfire-vulkan-$host.jar"), it.entries().asSequence().map(ZipEntry::getName).toList())
    }
    val second = runner("packageRuntime").build()
    assertEquals(TaskOutcome.UP_TO_DATE, second.task(":vulkan-runtime:buildVulkanRuntime")?.outcome)
    assertEquals(TaskOutcome.UP_TO_DATE, second.task(":vulkan-runtime:prepareVulkanRuntime")?.outcome)

    directory.resolve("vulkan-runtime/scripts/build-vulkan-runtime.py").toFile().appendText("\n# Changed native inputs\n")
    assertEquals(TaskOutcome.SUCCESS, runner("packageRuntime").build().task(":vulkan-runtime:buildVulkanRuntime")?.outcome)
    val clean = runner("clean", "packageRuntime", "--parallel").build()
    assertEquals(TaskOutcome.SUCCESS, clean.task(":vulkan-runtime:buildVulkanRuntime")?.outcome)
    assertTrue(directory.resolve("vulkan-runtime/build/runtime/soulfire-vulkan-$host.jar").toFile().isFile)
  }

  @Test
  fun `all platform packaging uses supplied packages and removes unselected outputs`() {
    VulkanPlatforms.all.forEach { prebuilt(it) }
    val result = runner("packageRuntime", "-PvulkanPlatforms=all", "-PvulkanRuntimeDirectory=prebuilt").build()
    assertNull(result.task(":vulkan-runtime:buildVulkanRuntime"))
    ZipFile(directory.resolve("build/distribution/runtime.zip").toFile()).use {
      assertEquals(VulkanPlatforms.all.map { platform -> "soulfire-vulkan-$platform.jar" }.toSet(), it.entries().asSequence().map(ZipEntry::getName).toSet())
    }
    runner("packageRuntime", "-PvulkanPlatforms=windows-arm64", "-PvulkanRuntimeDirectory=prebuilt").build()
    assertEquals(listOf("soulfire-vulkan-windows-arm64.jar"), directory.resolve("vulkan-runtime/build/runtime").toFile().listFiles()!!.map { it.name })
  }

  @Test
  fun `mapped runtime classpaths include packages after clean`() {
    prebuilt(host)
    runner("packageResolvedRuntime", "-PvulkanRuntimeDirectory=prebuilt").build()
    ZipFile(directory.resolve("build/distribution/resolved.zip").toFile()).use {
      assertEquals(listOf("soulfire-vulkan-$host.jar"), it.entries().asSequence().map(ZipEntry::getName).toList())
    }
    runner("clean", "packageResolvedRuntime", "--parallel", "-PvulkanRuntimeDirectory=prebuilt").build()
    ZipFile(directory.resolve("build/distribution/resolved.zip").toFile()).use {
      assertEquals(listOf("soulfire-vulkan-$host.jar"), it.entries().asSequence().map(ZipEntry::getName).toList())
    }
  }

  @Test
  fun `a missing release platform stops packaging`() {
    prebuilt(host)
    val result = runner("packageRuntime", "-PvulkanPlatforms=all", "-PvulkanRuntimeDirectory=prebuilt").buildAndFail()
    assertEquals(TaskOutcome.FAILED, result.task(":vulkan-runtime:prepareVulkanRuntime")?.outcome)
    assertFalse(directory.resolve("build/distribution/runtime.zip").toFile().exists())
  }

  @Test
  fun `a package with a mismatched manifest stops packaging`() {
    prebuilt(host, valid = false)
    val result = runner("packageRuntime", "-PvulkanRuntimeDirectory=prebuilt").buildAndFail()
    assertEquals(TaskOutcome.FAILED, result.task(":vulkan-runtime:prepareVulkanRuntime")?.outcome)
    assertTrue(directory.resolve("vulkan-runtime/build/runtime").toFile().listFiles().isNullOrEmpty())
  }

  @Test
  fun `the native subproject assembles independently`() {
    val result = runner(":vulkan-runtime:assemble").build()
    assertEquals(TaskOutcome.SUCCESS, result.task(":vulkan-runtime:buildVulkanRuntime")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":vulkan-runtime:prepareVulkanRuntime")?.outcome)
    assertNull(result.task(":packageRuntime"))
    assertTrue(directory.resolve("vulkan-runtime/build/runtime/soulfire-vulkan-$host.jar").toFile().isFile)
  }

  @ParameterizedTest
  @CsvSource(
    "Linux, amd64, linux-x86_64", "Linux, aarch64, linux-arm64",
    "Windows 11, amd64, windows-x86_64", "Windows 11, aarch64, windows-arm64",
    "Mac OS X, x86_64, macos-x86_64", "Mac OS X, aarch64, macos-arm64",
  )
  fun `Java host names match native package platforms`(os: String, arch: String, expected: String) {
    assertEquals(expected, VulkanPlatforms.host(os, arch))
  }
}
