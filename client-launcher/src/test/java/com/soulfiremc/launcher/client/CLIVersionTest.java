/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.launcher.client;

import com.soulfiremc.builddata.BuildData;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CLIVersionTest {
  @ParameterizedTest
  @MethodSource("versionRequests")
  void packagedVersionRequestsDoNotInitializeTheRuntime(List<String> arguments, @TempDir Path directory) throws Exception {
    var home = directory.resolve("cli-home");
    var output = directory.resolve("output.txt");
    var javaExecutable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    var command = new ArrayList<>(List.of(
      Path.of(System.getProperty("java.home"), "bin", javaExecutable).toString(),
      "-Dsf.cliHomeDir=" + home,
      "-Dsf.remoteAddress=127.0.0.1:1",
      "-Dsf.remoteToken=unused-version-test",
      "-jar", System.getProperty("sf.cli.testJar")));
    command.addAll(arguments);
    var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
    try {
      assertTrue(process.waitFor(20, TimeUnit.SECONDS));
      var text = Files.readString(output);
      assertEquals(0, process.exitValue(), text);
      assertEquals(BuildData.versionInfo() + System.lineSeparator(), text);
      assertFalse(Files.exists(home));
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
        process.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  private static Stream<List<String>> versionRequests() {
    return Stream.of(List.of("--version"), List.of("-V"), List.of("--version", "-V"));
  }
}
