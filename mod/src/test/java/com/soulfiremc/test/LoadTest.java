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
package com.soulfiremc.test;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.shared.SFLogAppender;
import com.soulfiremc.test.utils.TestBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public final class LoadTest {
  @TempDir
  public Path tempDir;

  @Test
  void load() {
    assertDoesNotThrow(() -> {
      System.setProperty("sf.baseDir", tempDir.toAbsolutePath().toString());
      System.setProperty("sf.unit.test", "true");

      // Bootstrap mixins and Minecraft registries
      TestBootstrap.bootstrapForTest();

      SFLogAppender.INSTANCE.start();

      var server = new SoulFireServer("127.0.0.1", PortHelper.getRandomAvailablePort(), Instant.now());

      server.shutdownManager().shutdownSoftware(false);
      server.shutdownManager().awaitShutdown();
    });
  }
}
