/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.test;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.grpc.DefaultAuthSystem;
import com.soulfiremc.util.PortHelper;
import com.soulfiremc.util.SFLogAppender;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginManager;

public class LoadTest {
  @TempDir
  public Path tempDir;

  @Test
  public void testLoad() {
    SFLogAppender.INSTANCE.start();

    var server = new SoulFireServer("127.0.0.1", PortHelper.getRandomAvailablePort(), new DefaultPluginManager(), Instant.now(), new DefaultAuthSystem(), tempDir);

    server.shutdownManager().shutdownSoftware(false);
    server.shutdownManager().awaitShutdown();
  }
}
