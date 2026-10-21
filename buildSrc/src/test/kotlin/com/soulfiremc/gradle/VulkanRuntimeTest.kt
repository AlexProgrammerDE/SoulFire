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
    directory.resolve("settings.gradle").writeText("rootProject.name = 'native-fixture'\n")
    directory.resolve("gradle.properties").writeText("org.gradle.jvmargs=-Xmx512m\n")
    directory.resolve("build.gradle").writeText(
      """
      plugins {
        id 'base'
        id 'java-library'
        id 'sf-vulkan-runtime'
      }
      def nativeRuntime = tasks.named('prepareVulkanRuntime')
      dependencies {
        runtimeOnly(files(nativeRuntime.flatMap { it.preparedJars }))
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
    directory.resolve("build-data/vulkan").createDirectories().resolve("Dockerfile").writeText("FROM debian:bookworm-slim\n")
    directory.resolve("scripts").createDirectories().resolve("build-vulkan-runtime.py").writeText(
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
    assertEquals(TaskOutcome.SUCCESS, first.task(":buildVulkanRuntime")?.outcome)
    ZipFile(directory.resolve("build/distribution/runtime.zip").toFile()).use {
      assertEquals(listOf("soulfire-vulkan-$host.jar"), it.entries().asSequence().map(ZipEntry::getName).toList())
    }
    val second = runner("packageRuntime").build()
    assertEquals(TaskOutcome.UP_TO_DATE, second.task(":buildVulkanRuntime")?.outcome)
    assertEquals(TaskOutcome.UP_TO_DATE, second.task(":prepareVulkanRuntime")?.outcome)

    directory.resolve("scripts/build-vulkan-runtime.py").toFile().appendText("\n# Changed native inputs\n")
    assertEquals(TaskOutcome.SUCCESS, runner("packageRuntime").build().task(":buildVulkanRuntime")?.outcome)
    val clean = runner("clean", "packageRuntime", "--parallel").build()
    assertEquals(TaskOutcome.SUCCESS, clean.task(":buildVulkanRuntime")?.outcome)
    assertTrue(directory.resolve("build/vulkan-runtime/soulfire-vulkan-$host.jar").toFile().isFile)
  }

  @Test
  fun `all platform packaging uses supplied packages and removes unselected outputs`() {
    VulkanPlatforms.all.forEach { prebuilt(it) }
    val result = runner("packageRuntime", "-PvulkanPlatforms=all", "-PvulkanRuntimeDirectory=prebuilt").build()
    assertNull(result.task(":buildVulkanRuntime"))
    ZipFile(directory.resolve("build/distribution/runtime.zip").toFile()).use {
      assertEquals(VulkanPlatforms.all.map { platform -> "soulfire-vulkan-$platform.jar" }.toSet(), it.entries().asSequence().map(ZipEntry::getName).toSet())
    }
    runner("packageRuntime", "-PvulkanPlatforms=windows-arm64", "-PvulkanRuntimeDirectory=prebuilt").build()
    assertEquals(listOf("soulfire-vulkan-windows-arm64.jar"), directory.resolve("build/vulkan-runtime").toFile().listFiles()!!.map { it.name })
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
    assertEquals(TaskOutcome.FAILED, result.task(":prepareVulkanRuntime")?.outcome)
    assertFalse(directory.resolve("build/distribution/runtime.zip").toFile().exists())
  }

  @Test
  fun `a package with a mismatched manifest stops packaging`() {
    prebuilt(host, valid = false)
    val result = runner("packageRuntime", "-PvulkanRuntimeDirectory=prebuilt").buildAndFail()
    assertEquals(TaskOutcome.FAILED, result.task(":prepareVulkanRuntime")?.outcome)
    assertTrue(directory.resolve("build/vulkan-runtime").toFile().listFiles().isNullOrEmpty())
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
